package hudson.plugins.collabnet.tracker;

import com.collabnet.ce.soap50.webservices.tracker.ArtifactSoapDO;
import com.collabnet.ce.webservices.CollabNetApp;
import com.collabnet.ce.webservices.FileStorageApp;
import com.collabnet.ce.webservices.TrackerApp;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.*;
import hudson.plugins.collabnet.AbstractTeamForgeNotifier;
import hudson.plugins.collabnet.ConnectionFactory;
import hudson.plugins.collabnet.util.CNFormFieldValidator;
import hudson.plugins.collabnet.util.CNHudsonUtil;
import hudson.plugins.collabnet.util.ComboBoxUpdater;
import hudson.plugins.collabnet.util.CommonUtil;
import hudson.tasks.BuildStepMonitor;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.logging.Logger;

public class CNTracker extends AbstractTeamForgeNotifier {
    private static int DEFAULT_PRIORITY = Priority.DEFAULT.n;

    // listener is used for logging and will only be
    // set at the beginning of perform.
    private transient BuildListener listener = null;
    
    // data from jelly
    private String tracker = null;
    private String title = null;
    private String assign_user = null;
    private int priority = DEFAULT_PRIORITY; // for compatibility reason this has to be persisted as an integer
    private boolean attach_log = true;
    private boolean always_update = false;
    private boolean close_issue = true;
    private String release;

    // collabNet object
    private transient CollabNetApp cna = null;

    /**
     * Constructs a new CNTracker instance.
     *
     * @param tracker tracker name.
     * @param title title to use when create new tracker artifacts OR to find
     *              existing tracker artifacts.
     * @param assignUser user to assign new tracker artifacts to.
     * @param priority of new tracker artifacts.
     * @param attachLog if true, Hudson build logs will be uploaded and
     *                   attached when creating/updating tracker artifacts.
     * @param alwaysUpdate if true, always update the tracker artifacts (or
     *                      create one), even if build is successful and
     *                      the tracker artifact is closed.  If false, only
     *                      update when the tracker artifact is failing
     *                      or is open.
     * @param closeOnSuccess if true, the tracker artifact will be closed if the
     *                    Hudson build is successful.  Otherwise, open issues
     *                    will be updated with a successful message, but
     *                    remain open.
     * @param release to report the tracker artifact in.
     */
    @DataBoundConstructor
    public CNTracker(ConnectionFactory connectionFactory,
                     String project, String tracker, String title, 
                     String assignUser, Priority priority, boolean attachLog,
                     boolean alwaysUpdate, boolean closeOnSuccess, 
                     String release) {
        super(connectionFactory,project);
        this.tracker = tracker;
        this.title = title;
        this.assign_user = assignUser;
        this.priority = priority.n;
        this.attach_log = attachLog;
        this.always_update = alwaysUpdate;
        this.close_issue = closeOnSuccess;
        this.release = release;
    }

    /**
     * Setting the listener allows logging to work.
     *
     * @param listener handles build events.
     */
    private void setupLogging(BuildListener listener) {
        this.listener = listener;
    }

    /**
     * Logging will only work once the listener is set.
     * Otherwise, it will fail (silently).
     *
     * @param message to print to the console.
     */
    private void log(String message) {
        if (this.listener != null) {
            message = "CollabNet Tracker: " + message;
            this.listener.getLogger().println(message);
        }
    }

    /**
     * Convenience method to log RemoteExceptions.
     *
     * @param methodName in progress when the exception occurred.
     * @param re RemoteException that occurred.
     */
    private void log(String methodName, RemoteException re) {
        this.log(methodName + " failed due to " + re.getClass().getName() + 
                 ": " + re.getMessage());
    }

    /**
     * @return tracker name.
     */
    public String getTracker() {
        return this.tracker;
    }
    
    /**
     * @return title for the Tracker Artifact.
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * @return the user to assign new Tracker Artifacts to.
     */
    public String getAssignUser() {
        return Util.fixEmpty(this.assign_user);
    }
    
    /**
     * @return the priority to set new Tracker Artifacts to.
     */
    public Priority getPriority() {
        return Priority.valueOf(this.priority);
    }
    
    /**
     * @return true, if logs should be attached to Tracker Artifacts.
     */
    public boolean getAttachLog() {
        return this.attach_log;
    }
    
    /**
     * @return true, if artifact creation/update should happen, even if
     *         the Hudson build is successful and the artifact is not open.
     */
    public boolean getAlwaysUpdate() {
        return this.always_update;
    }
    
    /**
     * @return true, if artifacts should be closed when the Hudson build
     *         succeeds.
     */
    public boolean getCloseOnSuccess() {
        return this.close_issue;
    }
    
    /**
     * @return the name of the release which new Tracker Artifacts will be
     *         reported in.
     */
    public String getRelease() {
        return this.release;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }
    
    /**
     * Create/Update/Close the tracker issue, according to the Hudson 
     * build status.
     * 
     * @param build the current Hudson build.
     * @param launcher unused.
     * @param listener receives events that occur during a build; used for
     *                 logging.
     * @return false if a critical error occurred, true otherwise.
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                           BuildListener listener) 
        throws InterruptedException, IOException {
        this.setupLogging(listener);
        this.cna = CNHudsonUtil.getCollabNetApp(this.getCollabNetUrl(),
                                                this.getUsername(),
                                                this.getPassword());
        if (this.cna == null) {
            this.log("Critical Error: login to " + this.getCollabNetUrl() +
                     " failed.  Setting build status to UNSTABLE (or worse).");
            Result previousBuildStatus = build.getResult();
            build.setResult(previousBuildStatus.combine(Result.UNSTABLE));
            return false;
        }
        String projectId = this.getProjectId(getProject());
        if (projectId == null) {
            this.log("Critical Error: projectId cannot be found for " + 
                     this.getProject() + ".  This could mean that the project " +
                     "does not exist OR that the user logging in does not " +
                     "have access to that project.  " +
                     "Setting build status to UNSTABLE (or worse).");
            Result previousBuildStatus = build.getResult();
            build.setResult(previousBuildStatus.combine(Result.UNSTABLE));
            this.logoff();
            return false;
        }
        String trackerId = this.getTrackerId(projectId, this.tracker);
        if (trackerId == null) {
            this.log("Critical Error: trackerId cannot be found for " + 
                     this.tracker + ".  This could mean the tracker does " +
                     "not exist OR that the user logging in does not have " +
                     "access to that tracker.  " 
                     + "Setting build status to UNSTABLE (or worse).");
            Result previousBuildStatus = build.getResult();
            build.setResult(previousBuildStatus.combine(Result.UNSTABLE));
            this.logoff();
            return false;
        }
        ArtifactSoapDO issue = this.findTrackerArtifact(trackerId, build);
        Result buildStatus = build.getResult();
        // no issue and failure found
        if (issue == null &&
            buildStatus.isWorseThan(Result.SUCCESS)) {
            this.log("Build is not successful; opening a new issue.");
            String description = "The build has failed.  Latest " +
                "build status: " + build.getResult() + ".  For more info, " +
                "see " + this.getBuildUrl(build);
            this.createNewTrackerArtifact(projectId, trackerId, 
                                          "Open", description, build);
            // no issue and success may open a new issue if we're updating
            // no matter what
        } else if (issue == null &&
               buildStatus.isBetterOrEqualTo(Result.SUCCESS)) {
            this.log("Build is successful!");
            if (this.getAlwaysUpdate()) {
                String description = "The build has succeeded.  For " +
                    "more info, see " + this.getBuildUrl(build);
                this.createNewTrackerArtifact(projectId, trackerId, 
                                              "Closed", description, build);
            }
        // update existing fail -> fail
        } else if (issue.getStatusClass().equals("Open") &&
                   buildStatus.isWorseThan(Result.SUCCESS)) {
            this.log("Build is continuing to fail; updating issue.");
            this.updateFailingBuild(issue, build);
        }
        // close or update existing  fail -> succeed
        else if (issue.getStatusClass().equals("Open") &&
                 buildStatus.isBetterOrEqualTo(Result.SUCCESS)) {
            if (this.getCloseOnSuccess()) {
                this.log("Build succeeded; closing issue.");
                this.closeSucceedingBuild(issue, build);
            } else {
                // just update
                this.log("Build succeeded; updating issue.");
                this.updateSucceedingBuild(issue, build);        
            }
        }
        // create new succeed -> fail
        else if (issue.getStatusClass().equals("Close") && 
                 buildStatus.isWorseThan(Result.SUCCESS)) {
            // create new or reopen?
            if (this.getAlwaysUpdate()) {
                this.log("Build is not successful; re-opening issue.");
                this.updateFailingBuild(issue, build);
            } else {
                this.log("Build is not successful; opening a new issue.");
                String description = "The build has failed.  Latest " +
                    "build status: " + build.getResult() + ".  For more " +
                    "info, see " + this.getBuildUrl(build);
                this.createNewTrackerArtifact(projectId, trackerId, 
                                              "Open", description, build);
            }
        } else if (issue.getStatusClass().equals("Close") &&
                   buildStatus.isBetterOrEqualTo(Result.SUCCESS)) {
            this.log("Build continues to be successful!");
            if (this.getAlwaysUpdate()) {
                this.updateSucceedingBuild(issue, build);
            }
        } else {
            this.log("Unexpected state:  result is: " + buildStatus + 
                     ".  Issue status " + "class is: " + issue.getStatusClass()
                     + ".");
        }
        this.logoff();
        return true;
    }

    /**
     * Log out of the collabnet server.
     */
    public void logoff() {
        CNHudsonUtil.logoff(this.cna);
        this.cna = null;
    }

    /**
     * Given a project title, find the matching projectId.
     * If none is found, return null.
     *
     * @param projectName 
     * @return project id corresponding to the name, or null if none is found.
     */
    public String getProjectId(String projectName) {
        if (this.cna == null) {
            this.log("Cannot getProjectId, not logged in!");
            return null;
        }
        return CNHudsonUtil.getProjectId(this.cna, projectName);
    }

    /**
     * Given a tracker title and a projectId, find the matching tracker id.
     * 
     * @param projectId
     * @param trackerName
     * @return the tracker id for the tracker that matches this name, or null
     *         if no matching tracker is found.
     */
    public String getTrackerId(String projectId, String trackerName) {
        if (this.cna == null) {
            this.log("Cannot call getTrackerId, not logged in!");
            return null;
        }
        return CNHudsonUtil.getTrackerId(cna, projectId, trackerName);
    }

    /**
     * Return a tracker artifact with the matching title.
     *
     * @param trackerId
     * @param build the current Hudson build.
     * @return the artifact soap data object, if one exists which matches
     *         the title.  Otherwise, null.
     */
    public ArtifactSoapDO findTrackerArtifact(String trackerId,
            AbstractBuild<?, ?> build) throws IOException, InterruptedException {
        if (this.cna == null) {
            this.log("Cannot call findTrackerArtifact, not logged in!");
            return null;
        }
        String title = this.getInterpreted(build, this.getTitle());
        TrackerApp ta = new TrackerApp(this.cna);
        try {
            return ta.findLastTrackerArtifact(trackerId, title);
        } catch (RemoteException re) {
            this.log("findTrackerArtifact", re);
            return null;
        }
    }
    
    /**
     * Create a new tracker artifact with the given values.
     * 
     * @param projectId id for project
     * @param trackerId id for tracker
     * @param status status to set on the new artifact (Open, Closed, etc.).
     * @param description description of the new artifact.
     * @return the newly created ArtifactSoapDO.
     */
    public ArtifactSoapDO createNewTrackerArtifact(String projectId, 
                                                   String trackerId, 
                                                   String status,
                                                   String description,
                                                   AbstractBuild <?, ?> build)
                                                   throws IOException, InterruptedException {
        if (this.cna == null) {
            this.log("Cannot call createNewTrackerArtifact, not logged in!");
            return null;
        }
        String buildLogId = null;
        if (this.getAttachLog()) {
            buildLogId = this.uploadBuildLog(build);
            if (buildLogId != null) {
                this.log("Successfully uploaded build log.");
            } else {
                this.log("Failed to upload build log.");
            }
        }
        // check assign user validity
        String assignTo = this.getValidAssignUser(projectId);
        String title = this.getInterpreted(build, this.getTitle());
        String releaseId = CNHudsonUtil.getProjectReleaseId(this.cna, 
                                                            projectId, 
                                                            this.getRelease());
        TrackerApp ta = new TrackerApp(this.cna);
        try {
            ArtifactSoapDO asd = ta.createNewTrackerArtifact(trackerId, title,
                                              description, null, null, status,
                                              null, this.priority, 0, 
                                              assignTo, releaseId, null, 
                                              build.getLogFile().getName(), 
                                              "text/plain", buildLogId);
            this.log("Created tracker artifact '" + title + "' in tracker '" 
                     + this.getTracker() + "' in project '" + this.getProject()
                     + "' on behalf of '" + this.getUsername() + "' at " 
                     + this.getArtifactUrl(asd) + ".");
            return asd;
        } catch (RemoteException re) {
            this.log("createNewTrackerArtifact", re);
            return null;
        }
    }
    
    /**
     * @param projectId
     * @return the assigned user, if that user is a member of the project.
     *         Otherwise, null.
     */
    private String getValidAssignUser(String projectId) {
        String valid_user = this.assign_user;
        if (!CNHudsonUtil.isUserMember(this.cna, this.assign_user, 
                                              projectId)) {
            this.log("User (" + this.assign_user + ") is not a member of " +
                     "the project (" + this.getProject() + ").  " + "Instead " +
                     "any new issues filed will be assigned to 'None'.");
            valid_user = null;
        }
        return valid_user;
    }
    
    /**
     * Update the issue with failing build status.
     * 
     * @param issue the existing issue.
     * @param build the current Hudson build.
     */
    public void updateFailingBuild(ArtifactSoapDO issue,
            AbstractBuild<?, ?> build) throws IOException, InterruptedException {
        if (this.cna == null) {
            this.log("Cannot call updateFailingBuild, not logged in!");
            return;
        }
        String buildLogId = null;
        if (this.getAttachLog()) {
            buildLogId = this.uploadBuildLog(build);
            if (buildLogId != null) {
                this.log("Successfully uploaded build log.");
            } else {
                this.log("Failed to upload build log.");
            }
        }
        String update = "Updated";
        if (!issue.getStatus().equals("Open")) {
            issue.setStatus("Open");
            update = "Updated and reopened";
        }
        String comment = "The build is continuing to fail.  Latest " +
            "build status: " + build.getResult() + ".  For more info, see " + 
            this.getBuildUrl(build);
        String title = this.getInterpreted(build, this.getTitle());
        TrackerApp ta = new TrackerApp(this.cna);
        try {
            ta.setArtifactData(issue, comment, 
                               build.getLogFile().getName(), 
                               "text/plain", buildLogId);
            this.log(update + " tracker artifact '" + title + "' in tracker '" 
                     + this.getTracker() + "' in project '" + this.getProject()
                     + "' on behalf of '" + this.getUsername() + "' at " 
                     + this.getArtifactUrl(issue) + 
                     " with failed status.");
        } catch (RemoteException re) {
            this.log("updateFailingBuild", re);
        } catch (IOException ioe) {
            this.log("updateFailingBuild failed due to IOException:" + 
                     ioe.getMessage());
        }
    }
    
    /**
     * Update the issue with a build that's successful, but do not change 
     * its status.
     * 
     * @param issue the existing issue.
     * @param build the current Hudson build.
     */
    public void updateSucceedingBuild(ArtifactSoapDO issue,
            AbstractBuild<?, ?> build) throws IOException, InterruptedException {
        if (this.cna == null) {
            this.log("Cannot call updateSucceedingBuild, not logged in!");
            return;
        }
        String buildLogId = null;
        if (this.getAttachLog()) {
            buildLogId = this.uploadBuildLog(build);
            if (buildLogId != null) {
                this.log("Successfully uploaded build log.");
            } else {
                this.log("Failed to upload build log.");
            }
        }
        String comment = "The build is succeeding.  For more info, " +
            "see " + this.getBuildUrl(build);
        String title = this.getInterpreted(build, this.getTitle());
        TrackerApp ta = new TrackerApp(this.cna);
        try {
            ta.setArtifactData(issue, comment, build.getLogFile().getName(), 
                               "text/plain", buildLogId);
            this.log("Updated tracker artifact '" + title + "' in tracker '" 
                     + this.getTracker() + "' in project '" + this.getProject()
                     + "' on behalf of '" + this.getUsername() + "' at " 
                     + this.getArtifactUrl(issue) + 
                     " with successful status.");
        } catch (RemoteException re) {
            this.log("updateSucceedingBuild", re); 
        } catch (IOException ioe) {
            this.log("updateSuccedingBuild failed due to IOException:" + 
                     ioe.getMessage());
        }
    }
    
    /**
     * Update the issue with a build that's successful,  and close it.
     *
     * @param issue the existing issue.
     * @param build the current Hudson build.
     */
    public void closeSucceedingBuild(ArtifactSoapDO issue,
            AbstractBuild<?, ?> build) throws IOException, InterruptedException {
        if (this.cna == null) {
            this.log("Cannot call updateSucceedingBuild, not logged in!");
            return;
        }
        String buildLogId = null;
        if (this.getAttachLog()) {
            buildLogId = this.uploadBuildLog(build);
            if (buildLogId != null) {
                this.log("Successfully uploaded build log.");
            } else {
                this.log("Failed to upload build log.");
            }
        }
        String comment = "The build succeeded!  Closing issue.  " +
            "For more info, see " + this.getBuildUrl(build);
        issue.setStatusClass("Close");
        issue.setStatus("Closed");
        String title = this.getInterpreted(build, this.getTitle());
        TrackerApp ta = new TrackerApp(this.cna);
        try {
            ta.setArtifactData(issue, comment, 
                               build.getLogFile().getName(), 
                               "text/plain", buildLogId);
            this.log("Closed tracker artifact '" + title + "' in tracker '" 
                     + this.getTracker() + "' in project '" + this.getProject()
                     + "' on behalf of '" + this.getUsername() + "' at " 
                     + this.getArtifactUrl(issue) + 
                     " with successful status.");
        } catch (RemoteException re) {
            this.log("closeSucceedingBuild", re);
        }
    }
    
    /**
     * Returns the absolute URL to the build, if rootUrl has been configured.
     * If not, returns the build number.
     *
     * @param build the current Hudson build.
     * @return the absolute URL for this build, or the a string containing the
     *         build number.
     */
    private String getBuildUrl(AbstractBuild<?, ?> build) {
        Hudson hudson = Hudson.getInstance();
        String rootUrl = hudson.getRootUrl();
        if (rootUrl == null) {
            return "Hudson Build #" + build.number;
        } else {
            return hudson.getRootUrl() + build.getUrl();
        }
    }    
    
    /**
     * Get the artifact's url.
     *
     * @param art the artifact.
     * @return an absolute URL to the artifact.
     */
    private String getArtifactUrl(ArtifactSoapDO art) {
        return this.getCollabNetUrl() + "/sf/go/" + art.getId();
    }
    
    /**
     * Upload the build log to the collabnet server.
     *
     * @param build the current Hudson build.
     * @return the id associated with the file upload.
     */
    private String uploadBuildLog(AbstractBuild <?, ?> build) {
        if (this.cna == null) {
            this.log("Cannot call updateSucceedingBuild, not logged in!");
            return null;
        }
        String id = null;
        FileStorageApp sfsa = new FileStorageApp(this.cna);
        try {
            id = sfsa.uploadFile(build.getLogFile());
        } catch (RemoteException re) {
            this.log("uploadBuildLog", re);
        }
        return id;
    }
    
    /**
     * Translates a string that may contain  build vars like ${BUILD_VAR} to
     * a string with those vars interpreted.
     * 
     * @param build the Hudson build.
     * @param str the string to be interpreted.
     * @return the interpreted string.
     * @throws IllegalArgumentException if the env var is not found.
     */
    private String getInterpreted(AbstractBuild<?, ?> build, String str)
            throws IOException, InterruptedException {
        try {
            return CommonUtil.getInterpreted(build.getEnvironment(TaskListener.NULL), str);
        } catch (IllegalArgumentException iae) {
            this.log(iae.getMessage());
            throw iae;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractTeamForgeNotifier.DescriptorImpl {
        private static Logger log = Logger.getLogger("CNTrackerDescriptor");

        /**
         * @return human readable name used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "CollabNet Tracker";
        }

        /**
         * Form validation for the tracker field.
         *
         * @param req StaplerRequest which contains parameters from the config.jelly.
         */
        public FormValidation doCheckTracker(StaplerRequest req) {
            return CNFormFieldValidator.trackerCheck(req);
        }
        
        /**
         * Form validation for "assign issue to".
         *
         * @param req StaplerRequest which contains parameters from the config.jelly.
         */
        public FormValidation doCheckAssign(StaplerRequest req) {
            return CNFormFieldValidator.assignCheck(req);
        }
        
        /**
         * Form validation for the comment and description.
         *
         * @param value
         * @param name of field
         */
        public FormValidation doRequiredInterpretedCheck(
                @QueryParameter String value, @QueryParameter String name) throws FormValidation {
            return CNFormFieldValidator.requiredInterpretedCheck(value, name);
        }
        
        /**
         * Form validation for the release field.
         */
        public FormValidation doCheckRelease(CollabNetApp cna, @QueryParameter String project,
                                @QueryParameter("package") String rpackage, @QueryParameter String release) {
            return CNFormFieldValidator.releaseCheck(cna,project,rpackage,release,false);
        }

        /**********************************************
         * Methods for updating editable combo boxes. *
         **********************************************/

        /**
         * Gets a list of trackers to choose from and write them as a 
         * JSON string into the response data.
         */
        public ComboBoxModel doFillTrackerItems(CollabNetApp cna, @QueryParameter String project) {
            return ComboBoxUpdater.getTrackers(cna,project);
        }

        /**
         * Gets a list of projectUsers to choose from and write them as a 
         * JSON string into the response data.
         */
        public ComboBoxModel doFillAssignUserItems(CollabNetApp cna, @QueryParameter String project)
            throws IOException {
            return ComboBoxUpdater. getUsers(cna,project);
        }
        
        /**
         * Gets a list of releases to choose from and write them as a
         * JSON string into the response data.
         */
        public ComboBoxModel doFillReleaseItems(CollabNetApp cna,
                @QueryParameter String project, @QueryParameter("package") String _package) {
            return ComboBoxUpdater.getReleases(cna,project,_package);
        }
    }
}
