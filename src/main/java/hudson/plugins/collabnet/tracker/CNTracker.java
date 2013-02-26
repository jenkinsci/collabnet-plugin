package hudson.plugins.collabnet.tracker;

import com.collabnet.ce.webservices.CTFArtifact;
import com.collabnet.ce.webservices.CTFFile;
import com.collabnet.ce.webservices.CTFProject;
import com.collabnet.ce.webservices.CTFRelease;
import com.collabnet.ce.webservices.CTFTracker;
import com.collabnet.ce.webservices.CollabNetApp;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.model.TaskListener;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import static hudson.model.Result.SUCCESS;
import static hudson.model.Result.UNSTABLE;

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
     * @param attachLog if true, Jenkins build logs will be uploaded and
     *                   attached when creating/updating tracker artifacts.
     * @param alwaysUpdate if true, always update the tracker artifacts (or
     *                      create one), even if build is successful and
     *                      the tracker artifact is closed.  If false, only
     *                      update when the tracker artifact is failing
     *                      or is open.
     * @param closeOnSuccess if true, the tracker artifact will be closed if the
     *                    Jenkins build is successful.  Otherwise, open issues
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
     *         the Jenkins build is successful and the artifact is not open.
     */
    public boolean getAlwaysUpdate() {
        return this.always_update;
    }
    
    /**
     * @return true, if artifacts should be closed when the Jenkins build
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
     * Create/Update/Close the tracker issue, according to the Jenkins 
     * build status.
     * 
     * @param build the current Jenkins build.
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
        this.cna = connect();
        if (this.cna == null) {
            this.log("Critical Error: login to " + this.getCollabNetUrl() +
                     " failed.  Setting build status to UNSTABLE (or worse).");
            build.setResult(UNSTABLE);
            return false;
        }
        try {
            CTFProject p = cna.getProjectByTitle(getProject());
            if (p == null) {
                this.log("Critical Error: projectId cannot be found for " +
                         this.getProject() + ".  This could mean that the project " +
                         "does not exist OR that the user logging in does not " +
                         "have access to that project.  " +
                         "Setting build status to UNSTABLE (or worse).");
                build.setResult(UNSTABLE);
                return false;
            }
            CTFTracker t = p.getTrackers().byTitle(this.tracker);
            if (t == null) {
                this.log("Critical Error: trackerId cannot be found for " +
                         this.tracker + ".  This could mean the tracker does " +
                         "not exist OR that the user logging in does not have " +
                         "access to that tracker.  "
                         + "Setting build status to UNSTABLE (or worse).");
                build.setResult(UNSTABLE);
                return false;
            }
            CTFArtifact issue = this.findTrackerArtifact(t, build);
            Result buildStatus = build.getResult();
            if (issue == null) {
                // no issue and failure found
                if (buildStatus.isWorseThan(SUCCESS)) {
                    this.log("Build is not successful; opening a new issue.");
                    String description = "The build has failed.  Latest " +
                        "build status: " + build.getResult() + ".  For more info, " +
                        "see " + this.getBuildUrl(build);
                    this.createNewTrackerArtifact(t,"Open", description, build);
                    // no issue and success may open a new issue if we're updating
                    // no matter what
                } else {
                    this.log("Build is successful!");
                    if (this.getAlwaysUpdate()) {
                        String description = "The build has succeeded.  For " +
                            "more info, see " + this.getBuildUrl(build);
                        this.createNewTrackerArtifact(t,"Closed", description, build);
                    }
                }
            } else {// update existing issue
                issue.refill();
                if (issue.getStatusClass().equals("Open") &&
                        buildStatus.isWorseThan(SUCCESS)) {
                    // update existing fail -> fail
                    this.log("Build is continuing to fail; updating issue.");
                    this.updateFailingBuild(issue, build);
                }
                // close or update existing  fail -> succeed
                else if (issue.getStatusClass().equals("Open") &&
                        buildStatus.isBetterOrEqualTo(SUCCESS)) {
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
                        buildStatus.isWorseThan(SUCCESS)) {
                    // create new or reopen?
                    if (this.getAlwaysUpdate()) {
                        this.log("Build is not successful; re-opening issue.");
                        this.updateFailingBuild(issue, build);
                    } else {
                        this.log("Build is not successful; opening a new issue.");
                        String description = "The build has failed.  Latest " +
                                "build status: " + build.getResult() + ".  For more " +
                                "info, see " + this.getBuildUrl(build);
                        this.createNewTrackerArtifact(t, "Open", description, build);
                    }
                } else if (issue.getStatusClass().equals("Close") &&
                        buildStatus.isBetterOrEqualTo(SUCCESS)) {
                    this.log("Build continues to be successful!");
                    if (this.getAlwaysUpdate()) {
                        this.updateSucceedingBuild(issue, build);
                    }
                } else {
                    this.log("Unexpected state:  result is: " + buildStatus +
                            ".  Issue status " + "class is: " + issue.getStatusClass()
                            + ".");
                }
            }
            return true;
        } finally {
            logoff();
        }
    }

    /**
     * Log out of the collabnet server.
     */
    public void logoff() {
        CNHudsonUtil.logoff(this.cna);
        this.cna = null;
    }

    /**
     * Return a tracker artifact with the matching title.
     *
     * @param build the current Jenkins build.
     * @return the artifact soap data object, if one exists which matches
     *         the title.  Otherwise, null.
     */
    public CTFArtifact findTrackerArtifact(CTFTracker tracker,
            AbstractBuild<?, ?> build) throws IOException, InterruptedException {
        if (this.cna == null) {
            this.log("Cannot call findTrackerArtifact, not logged in!");
            return null;
        }
        String title = this.getInterpreted(build, this.getTitle());
        List<CTFArtifact> r = tracker.getArtifactsByTitle(title);
        Collections.sort(r, new Comparator<CTFArtifact>() {
            public int compare(CTFArtifact o1, CTFArtifact o2) {
                return o2.getLastModifiedDate().compareTo(o1.getLastModifiedDate());
            }
        });
        if (r.size()>0) return r.get(0);
        return null;
    }
    
    /**
     * Create a new tracker artifact with the given values.
     * 
     * @param status status to set on the new artifact (Open, Closed, etc.).
     * @param description description of the new artifact.
     * @return the newly created ArtifactSoapDO.
     */
    public CTFArtifact createNewTrackerArtifact(CTFTracker t,
                                                   String status,
                                                   String description,
                                                   AbstractBuild <?, ?> build)
                                                   throws IOException, InterruptedException {
        if (this.cna == null) {
            this.log("Cannot call createNewTrackerArtifact, not logged in!");
            return null;
        }
        CTFFile buildLog = null;
        if (this.getAttachLog()) {
            buildLog = this.uploadBuildLog(build);
            if (buildLog != null) {
                this.log("Successfully uploaded build log.");
            } else {
                this.log("Failed to upload build log.");
            }
        }
        // check assign user validity
        String assignTo = this.getValidAssignUser(t.getProject());
        String title = this.getInterpreted(build, this.getTitle());
        CTFRelease release = CNHudsonUtil.getProjectReleaseId(t.getProject(),this.getRelease());
        try {
            CTFArtifact asd = t.createArtifact(title,
                                              description, null, null, status,
                                              null, this.priority, 0, 
                                              assignTo, release!=null?release.getId():null, null,
                                              build.getLogFile().getName(), 
                                              "text/plain", buildLog);
            this.log("Created tracker artifact '" + title + "' in tracker '" 
                     + this.getTracker() + "' in project '" + this.getProject()
                     + "' on behalf of '" + this.getUsername() + "' at " 
                     + asd.getURL() + ".");
            return asd;
        } catch (RemoteException re) {
            this.log("createNewTrackerArtifact", re);
            return null;
        }
    }
    
    /**
     * @return the assigned user, if that user is a member of the project.
     *         Otherwise, null.
     */
    private String getValidAssignUser(CTFProject p) throws RemoteException {
        if (!p.hasMember(this.assign_user)) {
            this.log("User (" + this.assign_user + ") is not a member of " +
                     "the project (" + this.getProject() + ").  " + "Instead " +
                     "any new issues filed will be assigned to 'None'.");
            return null;
        }
        return this.assign_user;
    }
    
    /**
     * Update the issue with failing build status.
     * 
     * @param issue the existing issue.
     * @param build the current Jenkins build.
     */
    public void updateFailingBuild(CTFArtifact issue,
            AbstractBuild<?, ?> build) throws IOException, InterruptedException {
        if (this.cna == null) {
            this.log("Cannot call updateFailingBuild, not logged in!");
            return;
        }
        CTFFile buildLog = null;
        if (this.getAttachLog()) {
            buildLog = this.uploadBuildLog(build);
            if (buildLog != null) {
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
        try {
            issue.update(comment, build.getLogFile().getName(), "text/plain", buildLog);
            this.log(update + " tracker artifact '" + title + "' in tracker '" 
                     + this.getTracker() + "' in project '" + this.getProject()
                     + "' on behalf of '" + this.getUsername() + "' at " 
                     + issue.getURL() +
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
     * @param build the current Jenkins build.
     */
    public void updateSucceedingBuild(CTFArtifact issue,
            AbstractBuild<?, ?> build) throws IOException, InterruptedException {
        if (this.cna == null) {
            this.log("Cannot call updateSucceedingBuild, not logged in!");
            return;
        }
        CTFFile buildLog = null;
        if (this.getAttachLog()) {
            buildLog = this.uploadBuildLog(build);
            if (buildLog != null) {
                this.log("Successfully uploaded build log.");
            } else {
                this.log("Failed to upload build log.");
            }
        }
        String comment = "The build is succeeding.  For more info, " +
            "see " + this.getBuildUrl(build);
        String title = this.getInterpreted(build, this.getTitle());
        try {
            issue.update(comment, build.getLogFile().getName(), "text/plain", buildLog);
            this.log("Updated tracker artifact '" + title + "' in tracker '" 
                     + this.getTracker() + "' in project '" + this.getProject()
                     + "' on behalf of '" + this.getUsername() + "' at " 
                     + issue.getURL() +
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
     * @param build the current Jenkins build.
     */
    public void closeSucceedingBuild(CTFArtifact issue,
            AbstractBuild<?, ?> build) throws IOException, InterruptedException {
        if (this.cna == null) {
            this.log("Cannot call updateSucceedingBuild, not logged in!");
            return;
        }
        CTFFile buildLog = null;
        if (this.getAttachLog()) {
            buildLog = this.uploadBuildLog(build);
            if (buildLog != null) {
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
        try {
            issue.update(comment, build.getLogFile().getName(), "text/plain", buildLog);
            this.log("Closed tracker artifact '" + title + "' in tracker '" 
                     + this.getTracker() + "' in project '" + this.getProject()
                     + "' on behalf of '" + this.getUsername() + "' at " 
                     + issue.getURL() +
                     " with successful status.");
        } catch (RemoteException re) {
            this.log("closeSucceedingBuild", re);
        }
    }
    
    /**
     * Returns the absolute URL to the build, if rootUrl has been configured.
     * If not, returns the build number.
     *
     * @param build the current Jenkins build.
     * @return the absolute URL for this build, or the a string containing the
     *         build number.
     */
    private String getBuildUrl(AbstractBuild<?, ?> build) {
        Hudson hudson = Hudson.getInstance();
        String rootUrl = hudson.getRootUrl();
        if (rootUrl == null) {
            return "Jenkins Build #" + build.number;
        } else {
            return hudson.getRootUrl() + build.getUrl();
        }
    }    
    
    /**
     * Upload the build log to the collabnet server.
     *
     * @param build the current Jenkins build.
     * @return the id associated with the file upload.
     */
    private CTFFile uploadBuildLog(AbstractBuild <?, ?> build) {
        if (this.cna == null) {
            this.log("Cannot call updateSucceedingBuild, not logged in!");
            return null;
        }
        try {
            return cna.upload(build.getLogFile());
        } catch (RemoteException re) {
            this.log("uploadBuildLog", re);
        }
        return null;
    }
    
    /**
     * Translates a string that may contain  build vars like ${BUILD_VAR} to
     * a string with those vars interpreted.
     * 
     * @param build the Jenkins build.
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
         */
        public FormValidation doCheckTracker(CollabNetApp cna, 
                @QueryParameter String project, @QueryParameter String tracker) throws RemoteException {
            return CNFormFieldValidator.trackerCheck(cna, project, tracker);
        }
        
        /**
         * Form validation for "assign issue to".
         */
        public FormValidation doCheckAssignUser(CollabNetApp cna, 
                @QueryParameter String project, @QueryParameter String assignUser) throws RemoteException {
            return CNFormFieldValidator.assignCheck(cna, project, assignUser);
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
                                @QueryParameter("package") String rpackage, @QueryParameter String release) throws RemoteException {
            return CNFormFieldValidator.releaseCheck(cna,project,rpackage,release,false);
        }

        /**********************************************
         * Methods for updating editable combo boxes. *
         **********************************************/

        /**
         * Gets a list of trackers to choose from and write them as a 
         * JSON string into the response data.
         */
        public ComboBoxModel doFillTrackerItems(CollabNetApp cna, @QueryParameter String project) throws RemoteException {
            return ComboBoxUpdater.getTrackerList((cna == null) ? null : cna.getProjectByTitle(project));
        }

        /**
         * Gets a list of projectUsers to choose from and write them as a 
         * JSON string into the response data.
         */
        public ComboBoxModel doFillAssignUserItems(CollabNetApp cna, @QueryParameter String project)
            throws IOException {
            return ComboBoxUpdater.getUsers(cna,project);
        }
        
        /**
         * Gets a list of releases to choose from and write them as a
         * JSON string into the response data.
         */
        public ComboBoxModel doFillReleaseItems(CollabNetApp cna,
                @QueryParameter String project, @QueryParameter("package") String _package) throws RemoteException {
            return ComboBoxUpdater.getReleases(cna,project,_package);
        }
    }
}
