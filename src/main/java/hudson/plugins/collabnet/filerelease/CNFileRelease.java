package hudson.plugins.collabnet.filerelease;


import com.collabnet.ce.webservices.CTFFile;
import com.collabnet.ce.webservices.CTFPackage;
import com.collabnet.ce.webservices.CTFProject;
import com.collabnet.ce.webservices.CTFRelease;
import com.collabnet.ce.webservices.CTFReleaseFile;
import com.collabnet.ce.webservices.CollabNetApp;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.collabnet.AbstractTeamForgeNotifier;
import hudson.plugins.collabnet.ConnectionFactory;
import hudson.plugins.collabnet.documentuploader.FilePattern;
import hudson.plugins.collabnet.util.CNFormFieldValidator;
import hudson.plugins.collabnet.util.CNHudsonUtil;
import hudson.plugins.collabnet.util.ComboBoxUpdater;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepMonitor;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.jenkinsci.remoting.RoleChecker;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;


/**
 * Jenkins plugin to update files from the Jenkins workspace 
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

    private String description = "";
    private static final String RELEASE_STATUS_ACTIVE = "active";
    private static final String MATURITY_NONE = "";
    
    /**
     * Creates a new CNFileRelease object.
     *
     * @param project where the files will be uploaded.  The project
     *                contains the package.
     * @param pkg where the files will be uploaded.  The package contains
     *                the release.
     * @param release where the files will be uploaded.
     * @param overwrite whether or not to overwrite existing files.
     * @param filePatterns Any files in the Jenkins workspace that match these 
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
     * @param build current Jenkins build.
     * @param launcher unused.
     * @param listener receives events that happen during a build.  We use it 
     *                 for logging.
     * @return true if successful, false if a critical error occurred.
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, 
                           BuildListener listener) throws IOException, InterruptedException {
        this.setupLogging(listener);
        this.cna = connect();
        if (this.cna == null) {
            this.logConsole("Critical Error: login to " + this.getCollabNetUrl() +
                     " failed.  Setting build status to UNSTABLE (or worse).");
            Result previousBuildStatus = build.getResult();
            build.setResult(previousBuildStatus.combine(Result.UNSTABLE));
            build.addAction(this.createAction(0, null));
            return false;
        }
        CTFRelease release = this.getReleaseObject();
        if (release == null) {
            Result previousBuildStatus = build.getResult();
            build.setResult(previousBuildStatus.combine(Result.UNSTABLE));
            this.logoff();
            build.addAction(this.createAction(0, release));
            return false;
        }
        // now that we have the releaseId, we can do the upload.
        int numUploaded = this.uploadFiles(build, release);
        build.addAction(this.createAction(numUploaded, release));
        this.logoff();
        return true;
    }

    /**
     * Get the ResultAction for this build.
     *
     * @param numUploaded
     * @return CnfrResultAction.
     */
    public CnfrResultAction createAction(int numUploaded, CTFRelease release) {
        String displaymsg = "Download from CollabNet File Release System";
        return new CnfrResultAction(displaymsg,
                                    IMAGE_URL + "cn-icon.gif", 
                                    "console",
                                    release.getUrl(),
                                    numUploaded);
    }

    /**
     * Upload the files which match the file patterns to the given
     * releaseId.
     *
     * @param build current Jenkins build.
     * @param release where the files will be uploaded.
     * @return the number of files successfully uploaded.
     * @throws IOException
     * @throws InterruptedException
     */
    public int uploadFiles(AbstractBuild<?, ?> build, CTFRelease release) throws IOException, InterruptedException {
        int numUploaded = 0;
        this.logConsole("Uploading file to project '" + this.getProject() +
                 "', package '" + this.getPkg() + "', release '" +
                 this.getRelease() + "' on host '" + this.getCollabNetUrl() + 
                 "' as user '" + this.getUsername() + "'.");
        // upload files
        for (FilePattern uninterp_fp : this.getFilePatterns()) {
            String file_pattern;
            try {
                file_pattern = uninterp_fp.interpret(build, listener);
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
                CTFReleaseFile file = null;
                try {
                    file = release.getFileByTitle(uploadFilePath.getName());
                } catch (RemoteException re) {
                    this.log("find file", re);
                }
                if (file != null) {
                    if (this.isOverwrite()) {
                        // delete existing file
                        try {
                            file.delete();
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
                    CTFFile f = new CTFFile(cna,uploadFilePath.act(
                        new RemoteFrsFileUploader(getCollabNetUrl(), getUsername(), cna.getSessionId())
                    ));
                    CTFReleaseFile rf = release.addFile(uploadFilePath.getName(), getMimeType(uploadFilePath), f);

                    this.logConsole("Uploaded file " + uploadFilePath.getName() + " -> " + rf.getURL());
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

        /**
         * Constructor. Needs to have old sessionId, since the uploaded file is only available to the same session.
         * @param serverUrl collabnet serverUrl
         * @param username collabnet username
         * @param sessionId collabnet sessionId
         */
        public RemoteFrsFileUploader(String serverUrl, String username, String sessionId) {
            mServerUrl = serverUrl;
            mUsername = username;
            mSessionId = sessionId;
        }

        /**
         * @see FileCallable#invoke(File, VirtualChannel)
         */
        public String invoke(File f, VirtualChannel channel) throws IOException {
            CollabNetApp cnApp = CNHudsonUtil.recreateCollabNetApp(mServerUrl, mUsername, mSessionId);
            return cnApp.upload(f).getId();
        }
        @Override
        public void checkRoles(RoleChecker arg0) throws SecurityException {
            // TODO Auto-generated method stub
        }
    }

    /**
     * Return the filepaths in the workspace which match the pattern.
     *
     * @param build The Jenkins build.
     * @param pattern An ant-style pattern.
     * @return an array of FilePaths which match this pattern in the 
     *         Jenkins workspace.
     */
    private FilePath[] getFilePaths(AbstractBuild<?, ?> build, 
                                    String pattern) {
        FilePath workspace = build.getWorkspace();

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
    public static String getMimeType(FilePath f) {
        return new MimetypesFileTypeMap().getContentType(f.getName());
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
    public CTFRelease getReleaseObject() throws RemoteException {
        CTFProject projectId = this.getProjectObject();
        if (projectId == null) {
            this.logConsole("Critical Error: projectId cannot be found for " +
                     this.getProject() + ".  This could mean that the project "
                     + "does not exist OR that the user logging in does not " 
                     + "have access to that project.  " +
                     "Setting build status to UNSTABLE (or worse).");
            return null;
        }
        CTFPackage pkg = projectId.getPackages().byTitle(getPkg());
        if (pkg == null) {
            this.logConsole("Critical Error: packageId cannot be found for " +
                     this.getPkg() + ".  " +
                     "Setting build status to UNSTABLE (or worse).");
            return null;
        }
        CTFRelease release = pkg.getReleaseByTitle(getRelease());
        if (release == null) {
            release = pkg.createRelease(getRelease(), description, RELEASE_STATUS_ACTIVE, MATURITY_NONE);
            this.logConsole("Note: releaseId cannot be found for " +
                     this.getRelease() + ".  " +
                     "Creating a new release with specified releaseId. Setting build status to STABLE.");
            return release;
        }
        return release;
    }

    /**
     * Get the project id for the project name.
     * 
     * @return the matching project id, or null if none is found.
     */
    public CTFProject getProjectObject() throws RemoteException {
        if (this.cna == null) {
            this.logConsole("Cannot getProjectId, not logged in!");
            return null;
        }
        return cna.getProjectByTitle(this.getProject());
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
        public FormValidation doCheckPkg(CollabNetApp cna, @QueryParameter String project, @QueryParameter String pkg) throws RemoteException {
            return CNFormFieldValidator.packageCheck(cna,project,pkg);
        }

        /**
         * Form validation for release.
         *
         * @return the form validation
         */
        public FormValidation doCheckRelease(CollabNetApp cna, @QueryParameter String project,
                                @QueryParameter String pkg, @QueryParameter String release) throws RemoteException {
            return CNFormFieldValidator.releaseCheck(cna,project,pkg,release,true);
        }

        /**
         * Gets a list of packages to choose from and write them as a 
         * JSON string into the response data.
         */
        public ComboBoxModel doFillPkgItems(CollabNetApp cna, @QueryParameter String project) throws RemoteException {
            return ComboBoxUpdater.getPackages(cna,project);
        }

        /**
         * Gets a list of releases to choose from and write them as a 
         * JSON string into the response data.
         */
        public ComboBoxModel doFillReleaseItems(CollabNetApp cna,
                @QueryParameter String project, @QueryParameter("pkg") String _package) throws RemoteException {
            return ComboBoxUpdater.getReleases(cna,project,_package);
        }
    }
}
