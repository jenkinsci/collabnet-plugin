package hudson.plugins.collabnet.filerelease;


import com.collabnet.ce.soap50.webservices.frs.FrsFileSoapDO;
import com.collabnet.ce.webservices.CollabNetApp;
import com.collabnet.ce.webservices.FrsApp;
import com.collabnet.ce.webservices.FileStorageApp;

import hudson.Launcher;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;

import hudson.plugins.collabnet.share.TeamForgeShare;
import hudson.plugins.collabnet.util.CNFormFieldValidator;
import hudson.plugins.collabnet.util.CNHudsonUtil;
import hudson.plugins.collabnet.util.ComboBoxUpdater;
import hudson.plugins.collabnet.util.CommonUtil;

import hudson.plugins.promoted_builds.Promotion;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;


/**
 * Hudson plugin to update files from the Hudson workspace 
 * to the CollabNet File Release System.
 */
public class CNFileRelease extends Publisher {
    // listener is used for logging and will only be
    // set at the beginning of perform.
    private transient BuildListener listener = null;

    private static final String IMAGE_URL = "/plugin/collabnet/images/48x48/";
    
    // collabNet object
    private transient CollabNetApp cna = null;

    // Variables from the form
    private boolean override_auth = true;
    private String url;
    private String username;
    private String password;
    private String project;
    private String rpackage;
    private String release;
    private boolean overwrite;
    private String[] file_patterns;

    private transient static TeamForgeShare.TeamForgeShareDescriptor 
        shareDescriptor = null;

    /**
     * Creates a new CNFileRelease object.
     *
     * @param url for the CollabNet server, i.e. http://forge.collab.net
     * @param username used to log into the CollabNet server.
     * @param password for the logging-in user.
     * @param project where the files will be uploaded.  The project 
     *                contains the package.
     * @param package where the files will be uploaded.  The package contains 
     *                the release.
     * @param release where the files will be uploaded.
     * @param overwrite whether or not to overwrite existing files.
     * @param filePatterns Any files in the Hudson workspace that match these 
     *                     ant-style patterns will be uploaded to the 
     *                     CollabNet server.
     */
    public CNFileRelease(String url, String username, String password, 
                         String project, String rpackage, String release, 
                         boolean overwrite, String[] filePatterns, 
                         boolean override_auth) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.project = project;
        this.rpackage = rpackage;
        this.release = release;
        this.overwrite = overwrite;
        this.file_patterns = filePatterns;
        this.override_auth = override_auth;
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
    private void log(String message) {
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
     * @return the url for the CollabNet server.
     */
    public String getCollabNetUrl() {
        if (this.overrideAuth()) {
            return this.url;
        } else {
            return getTeamForgeShareDescriptor().getCollabNetUrl();
        }
    }

    /**
     * @return the username used for logging in.
     */
    public String getUsername() {
        if (this.overrideAuth()) {
            return this.username;            
        } else {
            return getTeamForgeShareDescriptor().getUsername();
        }
    }

    /**
     * @return the password used for logging in.
     */
    public String getPassword() {
        if (this.overrideAuth()) {
            return this.password;
        } else {
            return getTeamForgeShareDescriptor().getPassword();
        }
    }

    /**
     * @return the project where the files are uploaded.
     */
    public String getProject() {
        return this.project;
    }

    /**
     * @return the package of the release where the files are uploaded.
     */
    public String getPackage() {
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
    public boolean overwrite() {
        return this.overwrite;
    }

    /**
     * @return the ant-style file patterns.
     */
    public String[] getFilePatterns() {
        if (this.file_patterns != null) {
            return this.file_patterns;
        } else {
            return new String[0];
        }
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
     * @return the list of all possible packages, given the login data and
     *         the project.
     */
    public String[] getPackages() {
        CollabNetApp cna = CNHudsonUtil.getCollabNetApp(this.getCollabNetUrl(),
                                                        this.getUsername(), 
                                                        this.getPassword());
        String projectId = CNHudsonUtil.getProjectId(cna, this.getProject());
        Collection<String> packages = ComboBoxUpdater.PackagesUpdater
            .getPackageList(cna, projectId);
        CNHudsonUtil.logoff(cna);
        return packages.toArray(new String[0]);
    }

    /**
     * @return the list of all possible releases, given the login data, the
     *         project and the package.
     */
    public String[] getReleases() {
        CollabNetApp cna = CNHudsonUtil.getCollabNetApp(this.getCollabNetUrl(),
                                                        this.getUsername(), 
                                                        this.getPassword());
        String packageId = this.getPackageId(this.getProjectId());
        Collection<String> releases = ComboBoxUpdater.ReleasesUpdater
            .getReleaseList(cna, packageId);
        CNHudsonUtil.logoff(cna);
        return releases.toArray(new String[0]);
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
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, 
                           BuildListener listener) {
        this.setupLogging(listener);
        this.cna = CNHudsonUtil.getCollabNetApp(this.getCollabNetUrl(), 
                                                this.getUsername(), 
                                                this.getPassword());
        if (this.cna == null) {
            this.log("Critical Error: login to " + this.getCollabNetUrl() + 
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
     * @param success true if successful, false otherwise.
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
     * @param releaseId
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
     * @param filePath path to uploaded frs file.
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
     */
    public int uploadFiles(AbstractBuild<?, ?> build, 
                            final String releaseId) {
        int numUploaded = 0;
        final FileStorageApp sfsa = new FileStorageApp(this.cna);
        final FrsApp fa = new FrsApp(this.cna);
        this.log("Uploading file to project '" + this.getProject() + 
                 "', package '" + this.getPackage() + "', release '" + 
                 this.getRelease() + "' on host '" + this.getCollabNetUrl() + 
                 "' as user '" + this.getUsername() + "'.");
        // upload files
        for (String uninterp_fp : this.getFilePatterns()) {
            String file_pattern = "";
            try {
                file_pattern = getInterpreted(build, uninterp_fp); 
            } catch (IllegalArgumentException e) {
                this.log("File pattern " + uninterp_fp + " contained a bad "
                         + "env var.  Skipping.");
                continue;
            }
            if (file_pattern.equals("")) {
                // skip empty fields
                continue;
            }
            this.log("Upload files matching " + file_pattern + ":");
            for (FilePath uploadFilePath : this.getFilePaths(build, 
                                                             file_pattern)) {
                // check if a file already exists
                String fileId = null;
                try {
                    fileId = fa.findFrsFile(uploadFilePath.getName(), 
                                            releaseId);
                } catch (RemoteException re) {
                    this.log("find file", re);
                }
                if (fileId != null) {
                    if (this.overwrite()) {
                        // delete existing file
                        try {
                            fa.deleteFrsFile(fileId);
                            this.log("Deleted previously uploaded file: " + 
                                     uploadFilePath.getName());
                        } catch (RemoteException re) {
                            this.log("delete file", re);
                        }                    
                    } else {
                        this.log("File " + uploadFilePath.getName() + 
                                 " already exists in the file release " +
                                 "system and overwrite is set to false.  " +
                                 "Skipping.");
                        continue;
                    }
                }
                try {
                    String path = uploadFilePath.
                        act(new FileCallable<String>() {
                            public String invoke(File f, 
                                                 VirtualChannel channel) 
                            throws IOException, RemoteException {
                                String fileId = sfsa.uploadFile(f);
                                FrsFileSoapDO fileSoap = fa.
                                createFrsFile(releaseId, 
                                              f.getName(), 
                                              CNFileRelease.getMimeType(f), 
                                              fileId);
                                return fileSoap.getPath();
                            }
                        });
                    this.log("Uploaded file " + uploadFilePath.getName() + 
                             " -> " + this.getFileUrl(path));
                    numUploaded++;
                } catch (RemoteException re) {
                    this.log("upload file", re);
                } catch (IOException ioe) {
                    this.log("Could not upload file due to IOException: " 
                             + ioe.getMessage());
                } catch (InterruptedException ie) {
                    this.log("Could not upload file due to " +
                             "InterruptedException: " + ie.getMessage());
                }
                
            }
        }
        return numUploaded;
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
        FilePath workspace = build.getProject().getWorkspace();
        FilePath[] uploadFilePaths = new FilePath[0];
        try {
            uploadFilePaths = workspace.list(pattern);
        } catch (IOException ioe) {
            this.log("Could not list workspace due to IOException: " 
                     + ioe.getMessage());
        } catch (InterruptedException ie) {
            this.log("Could not list workspace due to " +
                     "InterruptedException: " + ie.getMessage());
        }
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
            this.log("logoff failed. Not logged in!");
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
            this.log("Critical Error: projectId cannot be found for " + 
                     this.getProject() + ".  This could mean that the project "
                     + "does not exist OR that the user logging in does not " 
                     + "have access to that project.  " +
                     "Setting build status to UNSTABLE (or worse).");
            return null;
        }
        String packageId = this.getPackageId(projectId);
        if (packageId == null) {
            this.log("Critical Error: packageId cannot be found for " + 
                     this.getPackage() + ".  " +
                     "Setting build status to UNSTABLE (or worse).");
            return null;
        }
        String releaseId = this.getReleaseId(packageId);
        if (releaseId == null) {
            this.log("Critical Error: releaseId cannot be found for " + 
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
            this.log("Cannot getProjectId, not logged in!");
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
            this.log("Cannot getPackageId, not logged in!");
            return null;
        }
        return CNHudsonUtil.getPackageId(this.cna, this.getPackage(), 
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
            this.log("Cannot getReleaseId, not logged in!");
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
    private String getInterpreted(AbstractBuild<?, ?> build, String str) {
        Map<String, String> envVars = null;
        if (Hudson.getInstance().getPlugin("promoted-builds") != null
            && build.getClass().equals(Promotion.class)) {
            // if this is a promoted build, get the env vars from
            // the original build
            Promotion promotion = Promotion.class.cast(build);
            envVars = promotion.getTarget().getEnvVars();
        } else {
            envVars = build.getEnvVars();
        }
        try {
            return CommonUtil.getInterpreted(envVars, str);
        } catch (IllegalArgumentException iae) {
            this.log(iae.getMessage());
            throw iae;
        }
    }

    /**
     * @return the descriptor for CNFileRelease.
     */
    public Descriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    /**
     * Descriptor should be singleton.
     */
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    /**
     * The CNFileRelease Descriptor class.
     */
    public static final class DescriptorImpl 
        extends BuildStepDescriptor<Publisher> {

        private static int unique = 0;

        DescriptorImpl() {
            super(CNFileRelease.class);
        }

        /**
         * @return a unique integer, used to identify an instance
         *         of the File Release plugin on a page.
         */
        public synchronized int getUniqueId() {
            int return_value = unique;
            unique++;
            if (unique >= Integer.MAX_VALUE) {
                unique = 0;
            }
            return return_value;
        }

        /**
         * @return string to display for configuration screen.
         */
        public String getDisplayName() {
            return "CollabNet File Release";
        }

        /**
         * @return the url that contains the help files.
         */
        public static String getHelpUrl() {
            return "/plugin/collabnet/filerelease/";
        }

        /**
         * @return a relative url to the main help file.
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
         * Allows this plugin to be used as a promotion task.
         *
         * @param jobType a class to check for applicability.
         * @return true if CNFileRelease is applicable to this job.
         */
        public boolean isApplicable(java.lang.Class<? 
                                    extends hudson.model.AbstractProject> 
                                    jobType) {
            return true;
        }

        /**
         * Creates a new instance of {@link CNFileRelease} from a submitted 
         * form.
         *
         * @param req config page parameters.
         * @param formData data specific to this section, in json form.
         * @return new CNFileRelease object, instantiated from the 
         *         configuration form vars.
         * @throws FormException
         */
        public CNFileRelease newInstance(StaplerRequest req, 
                                         JSONObject formData) 
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
            } else if (!this.canInheritAuth()) {
                override_auth = true;
                username = (String)formData.get("username");
                password = (String)formData.get("password");
                collabneturl = (String)formData.get("collabneturl");
            }
            Object fileData = formData.get("files");
            JSONObject[] jpats;
            if (fileData instanceof JSONArray) {
                JSONArray patData = (JSONArray) fileData;
                jpats = (JSONObject []) JSONArray.toArray(patData, 
                                                          JSONObject.class);
            } else if (fileData instanceof JSONObject) {
                jpats = new JSONObject[1];
                jpats[0] = (JSONObject) fileData;
            } else {
                jpats = new JSONObject[0];
            }
            String[] patterns = new String[jpats.length];
            for (int i = 0; i < jpats.length; i++) {
                patterns[i] = (String) jpats[i].get("file");
            }            
            return new CNFileRelease(collabneturl, 
                                     username, 
                                     password, 
                                     (String)formData.get("project"),  
                                     (String)formData.get("package"), 
                                     (String)formData.get("release"), 
                                     CommonUtil.getBoolean("overwrite", 
                                                           formData),
                                     patterns, override_auth);
        }

        /**
         * Form validation for the CollabNet URL.
         *
         * @param req StaplerRequest which contains parameters from 
         *            the config.jelly.
         * @param rsp contains http response data (unused).
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
         * @param req contains parameters from the config.jelly.
         * @param rsp contains http response data (unused).
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
         * @param req contains parameters from 
         *            the config.jelly.
         * @param rsp contains http response data (unused).
         * @throws IOException
         * @throws ServletException
         */
        public void doProjectCheck(StaplerRequest req, 
                                   StaplerResponse rsp) 
            throws IOException, ServletException {
            new CNFormFieldValidator.ProjectCheck(req, rsp).process();
        }

        /**
         * Form validation for username.
         *
         * @param req contains parameters from config.jelly.
         * @param rsp contains http response data (unused).
         * @throws IOException
         * @throws ServletException
         */
        public void doRequiredCheck(StaplerRequest req, 
                                    StaplerResponse rsp) 
            throws IOException, ServletException {
            new CNFormFieldValidator.RequiredCheck(req, rsp).process();
        }

        /**
         * Form validation for package.
         *
         * @param req StaplerRequest which contains parameters from 
         *            the config.jelly.
         * @param rsp contains http response data (unused).
         * @throws IOException
         * @throws ServletException
         */
        public void doPackageCheck(StaplerRequest req, 
                                   StaplerResponse rsp) 
            throws IOException, ServletException {
            new CNFormFieldValidator.PackageCheck(req, rsp).process();      
        }

        /**
         * Form validation for release.
         *
         * @param req StaplerRequest which contains parameters from 
         *            the config.jelly.
         * @param rsp contains http response data (unused).
         * @throws IOException
         * @throws ServletException
         */
        public void doReleaseCheck(StaplerRequest req, 
                                   StaplerResponse rsp) 
            throws IOException, ServletException {
            new CNFormFieldValidator.ReleaseCheck(req, rsp).process();        
        }

        /**
         * Form validation for the file patterns.
         *
         * @param req StaplerRequest which contains parameters from 
         *            the config.jelly.
         * @param rsp contains http response data (unused).
         * @throws IOException
         * @throws ServletException
         */
        public void doUnRequiredInterpretedCheck(StaplerRequest req, 
                                                 StaplerResponse rsp) 
            throws IOException, ServletException {
            new CNFormFieldValidator.UnrequiredInterpretedCheck(req, rsp)
                .process();
        }
        
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
         * Gets a list of packages to choose from and write them as a 
         * JSON string into the response data.
         *
         * @param req contains parameters from 
         *            the config.jelly.
         * @param rsp http response data.
         * @throws IOException
         */
        public void doGetPackages(StaplerRequest req, StaplerResponse rsp) 
            throws IOException {
            new ComboBoxUpdater.PackagesUpdater(req, rsp).update();
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
