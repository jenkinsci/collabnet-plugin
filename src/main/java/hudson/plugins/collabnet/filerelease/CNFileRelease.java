package hudson.plugins.collabnet.filerelease;


import com.collabnet.ce.soap50.webservices.frs.FrsFileSoapDO;
import com.collabnet.ce.webservices.CollabNetApp;
import com.collabnet.ce.webservices.FileStorageApp;
import com.collabnet.ce.webservices.FrsApp;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.collabnet.AbstractTeamForgeNotifier;
import hudson.plugins.collabnet.ConnectionFactory;
import hudson.plugins.collabnet.documentuploader.FilePattern;
import hudson.plugins.collabnet.util.CNFormFieldValidator;
import hudson.plugins.collabnet.util.CNHudsonUtil;
import hudson.plugins.collabnet.util.ComboBoxUpdater;
import hudson.plugins.collabnet.util.CommonUtil;
import hudson.plugins.promoted_builds.Promotion;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepMonitor;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Map;


/**
 * Hudson plugin to update files from the Hudson workspace 
 * to the CollabNet File Release System.
 */
public class CNFileRelease extends AbstractTeamForgeNotifier {
    // listener is used for logging and will only be
    // set at the beginning of perform.
    private transient BuildListener listener = null;

    private static final String IMAGE_URL = "/plugin/collabnet/images/48x48/";
    
    // collabNet object
    private transient CollabNetApp cna = null;

    // Variables from the form
    private String rpackage;
    private String release;
    private boolean overwrite;
    private FilePattern[] file_patterns;

    /**
     * Creates a new CNFileRelease object.
     *
     * @param project where the files will be uploaded.  The project
     *                contains the package.
     * @param pkg where the files will be uploaded.  The package contains
     *                the release.
     * @param release where the files will be uploaded.
     * @param overwrite whether or not to overwrite existing files.
     * @param filePatterns Any files in the Hudson workspace that match these 
     *                     ant-style patterns will be uploaded to the 
     *                     CollabNet server.
     */
    @DataBoundConstructor
    public CNFileRelease(ConnectionFactory connectionFactory,
                         String project, String pkg, String release,
                         boolean overwrite, FilePattern[] filePatterns) {
        super(connectionFactory,project);
        this.rpackage = pkg;
        this.release = release;
        this.overwrite = overwrite;
        this.file_patterns = filePatterns;
    }

    /**
     * Setting the listener allows logging to work.
     *
     * @param listener passed into the perform method.
     */
    private void setupLogging(BuildListener listener) {
        this.listener = listener;
    }

    /**
     * Log a message to the console.  Logging will only work once the 
     * listener is set. Otherwise, it will fail (silently).
     *
     * @param message A string to print to the console.
     */
    private void logConsole(String message) {
        if (this.listener != null) {
            message = "CollabNet FileRelease: " + message;
            this.listener.getLogger().println(message);
        }
    }
    
    /**
     * Convenience method to log RemoteExceptions.  
     *
     * @param methodName in progress on when this exception occurred.
     * @param re The RemoteException that was thrown.
     */
    private void log(String methodName, RemoteException re) {
        this.logConsole(methodName + " failed due to " + re.getClass().getName() +
                 ": " + re.getMessage());
    }

    /**
     * @return the package of the release where the files are uploaded.
     */
    public String getPkg() {
        return this.rpackage;
    }

    /**
     * @return the release where the files are uploaded.
     */
    public String getRelease() {
        return this.release;
    }

    /**
     * @return whether or not existing files should be overwritten.
     */
    public boolean isOverwrite() {
        return this.overwrite;
    }

    /**
     * @return the ant-style file patterns.
     */
    public FilePattern[] getFilePatterns() {
        if (this.file_patterns != null) {
            return this.file_patterns;
        } else {
            return new FilePattern[0];
        }
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    /**
     * The function does the work of uploading files for the release.
     *
     * @param build current Hudson build.
     * @param launcher unused.
     * @param listener receives events that happen during a build.  We use it 
     *                 for logging.
     * @return true if successful, false if a critical error occurred.
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, 
                           BuildListener listener) throws IOException, InterruptedException {
        this.setupLogging(listener);
        this.cna = CNHudsonUtil.getCollabNetApp(this.getCollabNetUrl(), 
                                                this.getUsername(), 
                                                this.getPassword());
        if (this.cna == null) {
            this.logConsole("Critical Error: login to " + this.getCollabNetUrl() +
                     " failed.  Setting build status to UNSTABLE (or worse).");
            Result previousBuildStatus = build.getResult();
            build.setResult(previousBuildStatus.combine(Result.UNSTABLE));
            build.addAction(this.createAction(0, null));
            return false;
        }
        String releaseId = this.getReleaseId();
        if (releaseId == null) {
            Result previousBuildStatus = build.getResult();
            build.setResult(previousBuildStatus.combine(Result.UNSTABLE));
            this.logoff();
            build.addAction(this.createAction(0, releaseId));
            return false;
        }
        // now that we have the releaseId, we can do the upload.
        int numUploaded = this.uploadFiles(build, releaseId);
        build.addAction(this.createAction(numUploaded, releaseId));
        this.logoff();
        return true;
    }

    /**
     * Get the ResultAction for this build.
     *
     * @param numUploaded
     * @return CnfrResultAction.
     */
    public CnfrResultAction createAction(int numUploaded, String releaseId) {
        String displaymsg = "Download from CollabNet File Release System";
        return new CnfrResultAction(displaymsg, 
                                    IMAGE_URL + "cn-icon.gif", 
                                    "console", 
                                    this.getFileReleaseUrl(releaseId), 
                                    numUploaded);
    }

    /**
     * @param releaseId the release id
     * @return a link to the file release or the collabnet url if we don't
     *         have a releaseId.
     */
    private String getFileReleaseUrl(String releaseId) {
        String path = null;
        if (releaseId != null) {
            FrsApp fa = new FrsApp(this.cna);
            try {
                path = fa.getReleasePath(releaseId);                
            } catch (RemoteException re) {
                this.log("get release path", re);
            }
        }
        if (path != null) {
            return this.getCollabNetUrl() + "/sf/frs/do/viewRelease/" + path;
        } else {
            return this.getCollabNetUrl();
        }
    }

    /** 
     * @param path path to uploaded frs file.
     * @return url to uploaded frs file.
     */
    private String getFileUrl(String path) {
        return this.getCollabNetUrl() + "/sf/frs/do/downloadFile/" + path;
    }

    /**
     * Upload the files which match the file patterns to the given
     * releaseId.
     *
     * @param build current Hudson build.
     * @param releaseId where the files will be uploaded.
     * @return the number of files successfully uploaded.
     * @throws IOException
     * @throws InterruptedException
     */
    public int uploadFiles(AbstractBuild<?, ?> build, String releaseId) throws IOException, InterruptedException {
        int numUploaded = 0;
        final FrsApp fa = new FrsApp(this.cna);
        this.logConsole("Uploading file to project '" + this.getProject() +
                 "', package '" + this.getPkg() + "', release '" +
                 this.getRelease() + "' on host '" + this.getCollabNetUrl() + 
                 "' as user '" + this.getUsername() + "'.");
        // upload files
        for (FilePattern uninterp_fp : this.getFilePatterns()) {
            String file_pattern;
            try {
                // TODO: this handling and FilePattern.interpret are inconsistent.
                // probably it should be adjusted.
                file_pattern = getInterpreted(build, uninterp_fp.value);
            } catch (IllegalArgumentException e) {
                this.logConsole("File pattern " + uninterp_fp + " contained a bad "
                         + "env var.  Skipping.");
                continue;
            }
            if (file_pattern.equals("")) {
                // skip empty fields
                continue;
            }

            FilePath[] filePaths = this.getFilePaths(build, file_pattern);
            for (FilePath uploadFilePath : filePaths) {
                // check if a file already exists
                String fileId = null;
                try {
                    fileId = fa.findFrsFile(uploadFilePath.getName(), 
                                            releaseId);
                } catch (RemoteException re) {
                    this.log("find file", re);
                }
                if (fileId != null) {
                    if (this.isOverwrite()) {
                        // delete existing file
                        try {
                            fa.deleteFrsFile(fileId);
                            this.logConsole("Deleted previously uploaded file: " +
                                     uploadFilePath.getName());
                        } catch (RemoteException re) {
                            this.log("delete file", re);
                        }
                    } else {
                        this.logConsole("File " + uploadFilePath.getName() +
                                 " already exists in the file release " +
                                 "system and overwrite is set to false.  " +
                                 "Skipping.");
                        continue;
                    }
                }
                try {
                    // HACK: start
                    // All soap App must be preloaded by current classloader for "invoke" call below to work on slave
                    new FileStorageApp(this.cna);
                    // HACK: end
                    String path = uploadFilePath.act(
                        new RemoteFrsFileUploader(getCollabNetUrl(), getUsername(), cna.getSessionId(), releaseId)
                    );

                    this.logConsole("Uploaded file " + uploadFilePath.getName() + " -> " + this.getFileUrl(path));
                    numUploaded++;
                } catch (RemoteException re) {
                    this.log("upload file", re);
                } catch (IOException ioe) {
                    this.logConsole("Could not upload file due to IOException: "
                             + ioe.toString());
                    ioe.printStackTrace(this.listener.error("error"));
                } catch (InterruptedException ie) {
                    this.logConsole("Could not upload file due to " +
                             "InterruptedException: " + ie.getMessage());
                }
                
            }
        }
        return numUploaded;
    }

    /**
     * Private class that can perform upload function.
     */
    private static class RemoteFrsFileUploader implements FileCallable<String> {

        private String mServerUrl;
        private String mUsername;
        private String mSessionId;
        private String mReleaseId;

        /**
         * Constructor. Needs to have old sessionId, since the uploaded file is only available to the same session.
         * @param serverUrl collabnet serverUrl
         * @param username collabnet username
         * @param sessionId collabnet sessionId
         * @param releaseId the id of the release to create file in
         */
        public RemoteFrsFileUploader(String serverUrl, String username, String sessionId, String releaseId) {
            mServerUrl = serverUrl;
            mUsername = username;
            mSessionId = sessionId;
            mReleaseId = releaseId;
        }

        /**
         * @see FileCallable#invoke(File, VirtualChannel)
         */
        public String invoke(File f, VirtualChannel channel) throws IOException {
            CollabNetApp cnApp = CNHudsonUtil.recreateCollabNetApp(mServerUrl, mUsername, mSessionId);
            FileStorageApp fsApp = new FileStorageApp(cnApp);
            FrsApp frsApp = new FrsApp(cnApp);
            String fileId = fsApp.uploadFile(f);
            FrsFileSoapDO fileDO = frsApp.createFrsFile(mReleaseId, f.getName(), CNFileRelease.getMimeType(f), fileId);
            return fileDO.getPath();
        }
    }

    /**
     * Return the filepaths in the workspace which match the pattern.
     *
     * @param build The hudson build.
     * @param pattern An ant-style pattern.
     * @return an array of FilePaths which match this pattern in the 
     *         hudson workspace.
     */
    private FilePath[] getFilePaths(AbstractBuild<?, ?> build, 
                                    String pattern) {
        FilePath workspace;
        if (FreeStyleProject.class.isInstance(build.getProject())) { // generic instanceof causes compilation error
            // our standard project
            workspace = build.getWorkspace();
        } else {
            // promoted build - use the project's workspace, since the build doesn't always account for custom workspace
            // may be a bug with promoted build?
            workspace = build.getProject().getRootProject().getWorkspace();
        }

        String logEntry = "Searching ant pattern '" + pattern + "'";
        FilePath[] uploadFilePaths = new FilePath[0];
        try {
            uploadFilePaths = workspace.list(pattern);
            logEntry += " in " + workspace.absolutize().getRemote();
        } catch (IOException ioe) {
            this.logConsole("Could not list workspace due to IOException: "
                     + ioe.getMessage());
        } catch (InterruptedException ie) {
            this.logConsole("Could not list workspace due to " +
                     "InterruptedException: " + ie.getMessage());
        }
        logEntry += " : found " + uploadFilePaths.length + " entry(ies)";
        logConsole(logEntry);
        return uploadFilePaths;
    }

    /**
     * Get the mimetype for the file.
     *
     * @param f The file to return the mimetype for.
     * @return the string representing the mimetype of the file.
     */
    public static String getMimeType(File f) {
        return new MimetypesFileTypeMap().getContentType(f);
    }

    /**
     * Log out of the collabnet server.   Invalidates the cna object.
     */
    public void logoff() {
        if (this.cna != null) {
            CNHudsonUtil.logoff(cna);
            this.cna = null;
        } else {
            this.logConsole("logoff failed. Not logged in!");
        }
    }

    /**
     * Get the releaseId from the project/package/release names.
     * Returns null if somewhere along the way one of these IDs
     * cannot be found.
     *
     * @return the id for the release.
     */
    public String getReleaseId() {
        String projectId = this.getProjectId();
        if (projectId == null) {
            this.logConsole("Critical Error: projectId cannot be found for " +
                     this.getProject() + ".  This could mean that the project "
                     + "does not exist OR that the user logging in does not " 
                     + "have access to that project.  " +
                     "Setting build status to UNSTABLE (or worse).");
            return null;
        }
        String packageId = this.getPackageId(projectId);
        if (packageId == null) {
            this.logConsole("Critical Error: packageId cannot be found for " +
                     this.getPkg() + ".  " +
                     "Setting build status to UNSTABLE (or worse).");
            return null;
        }
        String releaseId = this.getReleaseId(packageId);
        if (releaseId == null) {
            this.logConsole("Critical Error: releaseId cannot be found for " +
                     this.getRelease() + ".  " +
                     "Setting build status to UNSTABLE (or worse).");
            return null;
        }
        return releaseId;
    }

    /**
     * Get the project id for the project name.
     * 
     * @return the matching project id, or null if none is found.
     */
    public String getProjectId() {
        if (this.cna == null) {
            this.logConsole("Cannot getProjectId, not logged in!");
            return null;
        }
        return CNHudsonUtil.getProjectId(this.cna, this.getProject());
    }

    /**
     * Get the package id.
     *
     * @param projectId the id of the project which contains this package.
     * @return the package id, or null if none is found.
     */
    public String getPackageId(String projectId) {
        if (this.cna == null) {
            this.logConsole("Cannot getPackageId, not logged in!");
            return null;
        }
        return CNHudsonUtil.getPackageId(this.cna, this.getPkg(),
                                         projectId);
    }

    /**
     * Get the release id.
     *
     * @param packageId the id of the package which contains this release.
     * @return the release id, or null if none is found.
     */
    public String getReleaseId(String packageId) {
        if (this.cna == null) {
            this.logConsole("Cannot getReleaseId, not logged in!");
            return null;
        }
        return CNHudsonUtil.getReleaseId(this.cna, packageId, 
                                         this.getRelease());
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
        Map<String, String> envVars;
        if (Hudson.getInstance().getPlugin("promoted-builds") != null
            && build.getClass().equals(Promotion.class)) {
            // if this is a promoted build, get the env vars from
            // the original build
            Promotion promotion = Promotion.class.cast(build);
            envVars = promotion.getTarget().getEnvironment(TaskListener.NULL);
        } else {
            envVars = build.getEnvironment(TaskListener.NULL);
        }
        try {
            return CommonUtil.getInterpreted(envVars, str);
        } catch (IllegalArgumentException iae) {
            this.logConsole(iae.getMessage());
            throw iae;
        }
    }

    /**
     * The CNFileRelease Descriptor class.
     */
    @Extension
    public static final class DescriptorImpl extends AbstractTeamForgeNotifier.DescriptorImpl {

        /**
         * @return string to display for configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "CollabNet File Release";
        }

        /**
         * Form validation for package.
         *
         * @return the form validation
         */
        public FormValidation doCheckPkg(CollabNetApp cna, @QueryParameter String project, @QueryParameter("package") String rpackage) {
            return CNFormFieldValidator.packageCheck(cna,project,rpackage);
        }

        /**
         * Form validation for release.
         *
         * @return the form validation
         */
        public FormValidation doCheckRelease(CollabNetApp cna, @QueryParameter String project,
                                @QueryParameter("package") String rpackage, @QueryParameter String release) {
            return CNFormFieldValidator.releaseCheck(cna,project,rpackage,release,true);
        }

        /**
         * Gets a list of packages to choose from and write them as a 
         * JSON string into the response data.
         */
        public ComboBoxModel doFillPkgItems(CollabNetApp cna, @QueryParameter String project) {
            return ComboBoxUpdater.getPackages(cna,project);
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
