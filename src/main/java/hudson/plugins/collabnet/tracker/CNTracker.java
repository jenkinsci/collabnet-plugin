package hudson.plugins.collabnet.tracker;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.tasks.Publisher;

import hudson.plugins.collabnet.share.TeamForgeShare;
import hudson.plugins.collabnet.util.CNFormFieldValidator;
import hudson.plugins.collabnet.util.CNHudsonUtil;
import hudson.plugins.collabnet.util.ComboBoxUpdater;
import hudson.plugins.collabnet.util.CommonUtil;

import java.io.IOException;
import java.lang.InterruptedException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.apache.axis.utils.StringUtils;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.collabnet.ce.soap50.webservices.tracker.ArtifactSoapDO;
import com.collabnet.ce.webservices.CollabNetApp;
import com.collabnet.ce.webservices.FrsApp;
import com.collabnet.ce.webservices.FileStorageApp;
import com.collabnet.ce.webservices.TrackerApp;

public class CNTracker extends Publisher {
    private static String SOAP_SERVICE = "/ce-soap50/services/";
    private static int DEFAULT_PRIORITY = 3;

    // listener is used for logging and will only be
    // set at the beginning of perform.
    private transient BuildListener listener = null;
    
    // data from jelly
    private boolean override_auth = true;
    private String username = null;
    private String password = null;
    private String collabNetUrl = null;
    private String project = null;
    private String tracker = null;
    private String title = null;
    private String assign_user = null;
    private int priority = DEFAULT_PRIORITY;
    private boolean attach_log = true;
    private boolean always_update = false;
    private boolean close_issue = true;
    private String release;

    // collabNet object
    private transient CollabNetApp cna = null;

    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private transient static TeamForgeShare.TeamForgeShareDescriptor 
        shareDescriptor = null;

    /**
     * Constructs a new CNTracker instance.
     *
     * @param username to login as.
     * @param password to login with.
     * @param collabNetUrl URL of the CollabNet server.
     * @param project project name.
     * @param tracker tracker name.
     * @param title title to use when create new tracker artifacts OR to find
     *              existing tracker artifacts.
     * @param assign_user user to assign new tracker artifacts to.
     * @param priority of new tracker artifacts.
     * @param attach_log if true, Hudson build logs will be uploaded and
     *                   attached when creating/updating tracker artifacts.
     * @param always_update if true, always update the tracker artifacts (or
     *                      create one), even if build is successful and
     *                      the tracker artifact is closed.  If false, only
     *                      update when the tracker artifact is failing
     *                      or is open.
     * @param close_issue if true, the tracker artifact will be closed if the
     *                    Hudson build is successful.  Otherwise, open issues
     *                    will be updated with a successful message, but
     *                    remain open.
     * @param release to report the tracker artifact in.
     */
    public CNTracker(String username, String password, String collabNetUrl, 
                     String project, String tracker, String title, 
                     String assign_user, String priority, boolean attach_log, 
                     boolean always_update, boolean close_issue, 
                     String release, boolean override_auth) {
        this.username = username;
        this.password = password;
        this.collabNetUrl = collabNetUrl;
        this.project = project;
        this.tracker = tracker;
        this.title = title;
        this.assign_user = assign_user;
        this.priority = Integer.parseInt(priority);
        this.attach_log = attach_log;
        this.always_update = always_update;
        this.close_issue = close_issue;
        this.release = release;
        this.override_auth = override_auth;
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
     * @return whether or not auth is overriden
     */
    public boolean overrideAuth() {
        return this.override_auth;
    }

    /**
     * @return username to login as.
     */
    public String getUsername() {
        if (this.overrideAuth()) {
            return this.username;            
        } else {
            return getTeamForgeShareDescriptor().getUsername();
        }
    }
    
    /**
     * @return password to login with.
     */
    public String getPassword() {
        if (this.overrideAuth()) {
            return this.password;
        } else {
            return getTeamForgeShareDescriptor().getPassword();
        }
    }

    /**
     * @return URL of the CollabNet server.
     */
    public String getCollabNetUrl() {
        if (this.overrideAuth()) {
            return this.collabNetUrl;
        } else {
            return getTeamForgeShareDescriptor().getCollabNetUrl();
        }
    }
    
    /**
     * @return project name.
     */
    public String getProject() {
        return this.project;
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
        if (this.assign_user == null || this.assign_user.equals("")) {
            return null;
        } else {
            return this.assign_user;
        }
    }
    
    /**
     * @return the priority to set new Tracker Artifacts to.
     */
    public int getPriority() {
        return this.priority;
    }
    
    /**
     * @return true, if logs should be attached to Tracker Artifacts.
     */
    public boolean attachLog() {
        return this.attach_log;
    }
    
    /**
     * @return true, if artifact creation/update should happen, even if
     *         the Hudson build is successful and the artifact is not open.
     */
    public boolean alwaysUpdate() {
        return this.always_update;
    }
    
    /**
     * @return true, if artifacts should be closed when the Hudson build
     *         succeeds.
     */
    public boolean closeOnSuccess() {
        return this.close_issue;
    }
    
    /**
     * @return the name of the release which new Tracker Artifacts will be
     *         reported in.
     */
    public String getRelease() {
        return this.release;
    }

    /**
     * @return the TeamForge share descriptor.
     */
    public static TeamForgeShare.TeamForgeShareDescriptor 
        getTeamForgeShareDescriptor() {
        if (shareDescriptor == null) {
            shareDescriptor = TeamForgeShare.getTeamForgeShareDescriptor();
        }
        return shareDescriptor;
    }

    /**
     * @return the list of all possible projects, given the login data.
     */
    public String[] getProjects() {
        CollabNetApp cna = CNHudsonUtil.getCollabNetApp(this.getCollabNetUrl(),
                                                        this.getUsername(), 
                                                        this.getPassword());
        Collection<String> projects = ComboBoxUpdater.ProjectsUpdater
            .getProjectList(cna);
        CNHudsonUtil.logoff(cna);
        return projects.toArray(new String[0]);
    }

    /**
     * @return the list of all possible trackers, given the login data.
     */
    public String[] getTrackers() {
        CollabNetApp cna = CNHudsonUtil.getCollabNetApp(this.getCollabNetUrl(),
                                                        this.getUsername(), 
                                                        this.getPassword());
        String projectId = this.getProjectId(this.getProject());
        Collection<String> trackers = ComboBoxUpdater.TrackersUpdater
            .getTrackerList(cna, projectId);
        CNHudsonUtil.logoff(cna);
        return trackers.toArray(new String[0]);
    }

    /**
     * @return the list of all project members, given the login data.
     */
    public String[] getUsers() {
        CollabNetApp cna = CNHudsonUtil.getCollabNetApp(this.getCollabNetUrl(),
                                                        this.getUsername(), 
                                                        this.getPassword());
        String projectId = this.getProjectId(this.getProject());
        Collection<String> users = ComboBoxUpdater.UsersUpdater
            .getUserList(cna, projectId);
        CNHudsonUtil.logoff(cna);
        return users.toArray(new String[0]);
    }

    /**
     * @return the list of all possible releases, given the login data.
     */
    public String[] getReleases() {
        CollabNetApp cna = CNHudsonUtil.getCollabNetApp(this.getCollabNetUrl(),
                                                        this.getUsername(), 
                                                        this.getPassword());
        String projectId = this.getProjectId(this.getProject());
        Collection<String> releases = ComboBoxUpdater.ReleasesUpdater
            .getProjectReleaseList(cna, projectId);
        CNHudsonUtil.logoff(cna);
        return releases.toArray(new String[0]);
    }
    
    /**
     * @return the descriptor.
     */
    public Descriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
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
            this.log("Critical Error: login to " + this.collabNetUrl + 
                     " failed.  Setting build status to UNSTABLE (or worse).");
            Result previousBuildStatus = build.getResult();
            build.setResult(previousBuildStatus.combine(Result.UNSTABLE));
            return false;
        }
        String projectId = this.getProjectId(this.project);
        if (projectId == null) {
            this.log("Critical Error: projectId cannot be found for " + 
                     this.project + ".  This could mean that the project " +
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
            if (this.alwaysUpdate()) {
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
            if (this.closeOnSuccess()) {
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
            if (this.alwaysUpdate()) {
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
            if (this.alwaysUpdate()) {
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
                                              AbstractBuild<?, ?> build) {
        if (this.cna == null) {
            this.log("Cannot call findTrackerArtifact, not logged in!");
            return null;
        }
        String title = this.getInterpreted(build, this.getTitle());
        TrackerApp ta = new TrackerApp(this.cna);
        ArtifactSoapDO asd = null;
        try {
            asd = ta.findLastTrackerArtifact(trackerId, title);
        } catch (RemoteException re) {
            this.log("findTrackerArtifact", re);
            return null;
        }
        return asd;
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
                                                   AbstractBuild <?, ?> build) {
        if (this.cna == null) {
            this.log("Cannot call createNewTrackerArtifact, not logged in!");
            return null;
        }
        ArtifactSoapDO asd = null;
        String buildLogId = null;
        if (this.attachLog()) {
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
            asd = ta.createNewTrackerArtifact(trackerId, title,
                                              description, null, null, status,
                                              null, this.priority, 0, 
                                              assignTo, releaseId, null, 
                                              build.getLogFile().getName(), 
                                              "text/plain", buildLogId);
            this.log("Created tracker artifact '" + title + "' in tracker '" 
                     + this.getTracker() + "' in project '" + this.getProject()
                     + "' on behalf of '" + this.getUsername() + "' at " 
                     + this.getArtifactUrl(asd) + ".");          
        } catch (RemoteException re) {
            this.log("createNewTrackerArtifact", re);
            return null;
        }
        return asd;
    }
    
    /**
     * @param projectId
     * @return the assigned user, if that user is a member of the project.
     *         Otherwise, null.
     */
    private String getValidAssignUser(String projectId) {
        String valid_user = this.assign_user;
        if (!CNHudsonUtil.isUserValid(this.cna, this.assign_user)) {
            this.log("User (" + this.assign_user + ") does not exist.  " +
                     "Instead any new issues filed will be assigned to " +
                     "'None'.");
            valid_user = null;
        } else if (!CNHudsonUtil.isUserMember(this.cna, this.assign_user, 
                                              projectId)) {
            this.log("User (" + this.assign_user + ") is not a member of " +
                     "the project (" + this.project + ").  " + "Instead " +
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
                                   AbstractBuild<?, ?> build) {
        if (this.cna == null) {
            this.log("Cannot call updateFailingBuild, not logged in!");
            return;
        }
        String buildLogId = null;
        if (this.attachLog()) {
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
                                      AbstractBuild<?, ?> build) {
        if (this.cna == null) {
            this.log("Cannot call updateSucceedingBuild, not logged in!");
            return;
        }
        String buildLogId = null;
        if (this.attachLog()) {
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
                                     AbstractBuild<?, ?> build) {
        if (this.cna == null) {
            this.log("Cannot call updateSucceedingBuild, not logged in!");
            return;
        }
        String buildLogId = null;
        if (this.attachLog()) {
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
    private String getInterpreted(AbstractBuild<?, ?> build, String str) {
        try {
            return CommonUtil.getInterpreted(build.getEnvVars(), str);
        } catch (IllegalArgumentException iae) {
            this.log(iae.getMessage());
            throw iae;
        }
    }
    
    public static final class DescriptorImpl extends Descriptor<Publisher> {
        private static Logger log = Logger.getLogger("CNTrackerDescriptor");

        public DescriptorImpl() {
            super(CNTracker.class);
        }
        
        /**
         * @return human readable name used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "CollabNet Tracker";
        }

        /**
         * @return the url for the help files.
         */
        public static String getHelpUrl() {
            return "/plugin/collabnet/tracker/";
        }

        /**
         * @return the help file for tracker.
         */
        @Override
        public String getHelpFile() {
            return getHelpUrl() + "help-main.html";
        }

        /**
         * @return true if there is auth data that can be inherited.
         */
        public boolean canInheritAuth() {
            return getTeamForgeShareDescriptor().useGlobal();
        }
        
        /**
         * Creates a new instance of {@link CNTracker} from 
         * a submitted form.
         *
         * @param req config page parameters.
         * @param formData data specific to this section, in json form.
         * @return new CNTracker instance.
         * @throws FormException
         */
        @Override
        public CNTracker newInstance(StaplerRequest req, JSONObject formData) 
            throws FormException {
            boolean override_auth = false;
            String username = null;
            String password = null;
            String collabneturl = null;
            if (formData.has("override_auth")) {
                override_auth = true;
                Object authObject = formData.get("override_auth");
                if (authObject instanceof JSONObject) {
                    username = (String)((JSONObject) authObject)
                        .get("username");
                    password = (String)((JSONObject) authObject)
                        .get("password");
                    collabneturl = (String)((JSONObject) authObject)
                        .get("collabneturl");
                } else if (authObject.equals(Boolean.TRUE)) {
                    username = (String) formData.get("username");
                    password = (String) formData.get("password");
                    collabneturl = (String) formData.get("collabneturl");
                } else {
                    override_auth = false;
                }
            } else if (!this.canInheritAuth()){
                override_auth = true;
                username = (String)formData.get("username");
                password = (String)formData.get("password");
                collabneturl = (String)formData.get("collabneturl");
            }
            return new CNTracker(username, password, collabneturl,
                                 (String)formData.get("project"),
                                 (String)formData.get("tracker"),
                                 StringUtils.
                                 strip((String)formData.get("title")),
                                 StringUtils.
                                 strip((String)formData.get("assign_user")),
                                 (String)formData.get("priority"),
                                 CommonUtil.getBoolean("attach_log", formData),
                                 CommonUtil.getBoolean("always_update", 
                                                       formData),
                                 CommonUtil.getBoolean("close_issue", 
                                                       formData),
                                 StringUtils.
                                 strip((String)formData.get("release")), 
                                 override_auth);
        }
        
        /**
         * Form validation for the CollabNet URL.
         *
         * @param req StaplerRequest which contains parameters from 
         *            the config.jelly.
         * @param rsp contains http response data.
         * @throws IOException
         * @throws ServletException
         */
        public void doCollabNetUrlCheck(StaplerRequest req, 
                                        StaplerResponse rsp) 
            throws IOException, ServletException {
            new CNFormFieldValidator.SoapUrlCheck(req, rsp).process();
        }
        
        /**
         * Check that a password is present and allows login.
         *
         * @param req StaplerRequest which contains parameters from 
         *            the config.jelly.
         * @param rsp contains http response data.
         * @throws IOException
         * @throws ServletException
         */
        public void doPasswordCheck(StaplerRequest req, 
                                    StaplerResponse rsp) 
            throws IOException, ServletException {
            new CNFormFieldValidator.LoginCheck(req, rsp).process();
        }
        
        /**
         * Form validation for the project field.
         *
         * @param req StaplerRequest which contains parameters from 
         *            the config.jelly.
         * @param rsp contains http response data.
         * @throws IOException
         * @throws ServletException
         */
        public void doProjectCheck(StaplerRequest req, 
                                   StaplerResponse rsp) 
            throws IOException, ServletException {
            new CNFormFieldValidator.ProjectCheck(req, rsp).process();
        }
        
        /**
         * Form validation for the tracker field.
         *
         * @param req StaplerRequest which contains parameters from 
         *            the config.jelly.
         * @param rsp contains http response data.
         * @throws IOException
         * @throws ServletException
         */
        public void doTrackerCheck(StaplerRequest req, 
                                   StaplerResponse rsp) 
            throws IOException, ServletException {
            new CNFormFieldValidator.TrackerCheck(req, rsp).process();
        }
        
        /**
         * Form validation for "assign issue to".
         *
         * @param req StaplerRequest which contains parameters from 
         *            the config.jelly.
         * @param rsp contains http response data.
         * @throws IOException
         * @throws ServletException
         */
        public void doAssignCheck(StaplerRequest req, 
                                  StaplerResponse rsp) 
            throws IOException, ServletException {
            new CNFormFieldValidator.AssignCheck(req, rsp).process();
        }
        
        /**
         * Form validation for username.
         *
         * @param req StaplerRequest which contains parameters from 
         *            the config.jelly.
         * @param rsp contains http response data.
         * @throws IOException
         * @throws ServletException
         */
        public void doRequiredCheck(StaplerRequest req, 
                                    StaplerResponse rsp) 
            throws IOException, ServletException {
            new CNFormFieldValidator.RequiredCheck(req, rsp).process();
        }
        
        /**
         * Form validation for the comment and description.
         *
         * @param req StaplerRequest which contains parameters from 
         *            the config.jelly.
         * @param rsp contains http response data.
         * @throws IOException
         * @throws ServletException
         */
        public void doRequiredInterpretedCheck(StaplerRequest req, 
                                               StaplerResponse rsp) 
            throws IOException, ServletException {
            new CNFormFieldValidator.RequiredInterpretedCheck(req, rsp)
                .process();
        }
        
        /**
         * Form validation for the release field.
         *
         * @param req StaplerRequest which contains parameters from 
         *            the config.jelly.
         * @param rsp contains http response data.
         * @throws IOException
         * @throws ServletException
         */
        public void doReleaseCheck(StaplerRequest req, 
                                   StaplerResponse rsp) 
            throws IOException, ServletException {
            new CNFormFieldValidator.ReleaseCheck(req, rsp).process();        
        }

        /**********************************************
         * Methods for updating editable combo boxes. *
         **********************************************/

        /**
         * Gets a list of projects to choose from and write them as a 
         * JSON string into the response data.
         *
         * @param req contains parameters from 
         *            the config.jelly.
         * @param rsp http response data.
         * @throws IOException
         */
        public void doGetProjects(StaplerRequest req, StaplerResponse rsp) 
            throws IOException {
            new ComboBoxUpdater.ProjectsUpdater(req, rsp).update();
        }

        /**
         * Gets a list of trackers to choose from and write them as a 
         * JSON string into the response data.
         *
         * @param req contains parameters from 
         *            the config.jelly.
         * @param rsp http response data.
         * @throws IOException
         */
        public void doGetTrackers(StaplerRequest req, StaplerResponse rsp) 
            throws IOException {
            new ComboBoxUpdater.TrackersUpdater(req, rsp).update();
        }

        /**
         * Gets a list of projectUsers to choose from and write them as a 
         * JSON string into the response data.
         *
         * @param req contains parameters from 
         *            the config.jelly.
         * @param rsp http response data.
         * @throws IOException
         */
        public void doGetProjectUsers(StaplerRequest req, StaplerResponse rsp) 
            throws IOException {
            new ComboBoxUpdater.UsersUpdater(req, rsp).update();
        }
        
        /**
         * Gets a list of releases to choose from and write them as a 
         * JSON string into the response data.
         *
         * @param req contains parameters from 
         *            the config.jelly.
         * @param rsp http response data.
         * @throws IOException
         */
        public void doGetReleases(StaplerRequest req, StaplerResponse rsp) 
            throws IOException {
            new ComboBoxUpdater.ReleasesUpdater(req, rsp).update();
        }

    }
}
