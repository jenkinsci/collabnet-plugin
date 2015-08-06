package hudson.plugins.collabnet.pblupload;

import com.collabnet.cubit.api.CubitConnector;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.collabnet.documentuploader.FilePattern;
import hudson.plugins.collabnet.util.CNFormFieldValidator;
import hudson.plugins.collabnet.util.CommonUtil;
import hudson.plugins.promoted_builds.Promotion;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.jenkinsci.remoting.RoleChecker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The PblUploader is used to upload files to the Project Build Library (Pbl) 
 * of a Lab Management manager node.
 */
public class PblUploader extends Notifier implements java.io.Serializable {
    private static final String LEFT_NAV_DISPLAY_MESSAGE = "Download from CollabNet Project Build Library";
    private static final String IMAGE_URL = "/plugin/collabnet/images/48x48/";   
    private static final long serialVersionUID = 1L;
    private static String API_URL = "cubit_api";
    private static String API_VERSION = "1";
    private static String PBL_UPLOAD_URL = "pbl_upload";
    private String hostUrl;
    private String user;
    private String key;
    private String project;
    private String pubOrPriv = "priv"; // "pub" or "priv" for compatibility reasons
    private FilePattern[] file_patterns;
    private String removePrefixRegex;
    private String path;
    private boolean preserveLocal = false;
    private boolean force = false;
    private String comment;
    private String description;

    // listener is used for logging and will only be
    // set at the beginning of perform.
    private transient BuildListener listener = null;
    
    /**
     * Constructs a new PblUploader instance.
     * 
     * @param hostUrl for the Cubit host.
     * @param user to login as.
     * @param key to login with.
     * @param project to upload files to
     * @param pubOrPriv whether these files should be in the pub (public)
     *                  or priv (private) directory.
     * @param filePatterns matching local files that should be uploaded.
     * @param path one the Cubit host where the files will be uploaded.
     * @param preserveLocal if true, local directory structure will be copied
     *                      to the server.
     * @param force if true, the files will be uploaded, even if they will
     *              overwrite existing files.
     * @param comment about the upload.
     * @param description of the files.
     */
    @DataBoundConstructor
    public PblUploader(String hostUrl, String user, String key, String project,
                       boolean pubOrPriv, FilePattern[] filePatterns, String path,
                       boolean preserveLocal, boolean force, String comment, 
                       String description, String removePrefixRegex) {
        this.hostUrl = hostUrl;
        this.user = user;
        // our sig generator depends on the letters in our hex
        // being lower case
        this.key = key.trim().toLowerCase();
        this.project = project;
        this.pubOrPriv = pubOrPriv?"pub":"priv";
        this.file_patterns = filePatterns;
        this.path = path;
        this.preserveLocal = preserveLocal;
        this.force = force;
        this.comment = comment;
        this.description = description;
        this.removePrefixRegex = removePrefixRegex;
    }

    /**
     * @return the Cubit host's URL.
     */
    public String getHostUrl() {
        if (this.hostUrl != null) {
            return this.hostUrl;
        } else {
            return "";
        }
    }

    /**
     * @return the user to login as.
     */
    public String getUser() {
        if (this.user != null) {
            return this.user;
        } else {
            return "";
        }
    }

    private String returnValueOrEmptyString(String input){
    	if (input != null) {
            return input;
        } else {
            return "";
        }	
    }
    
    
    /**
     * @return the key to login with.
     */
    public String getKey() {
      return returnValueOrEmptyString(this.key);
    }

    /**
     * @return the project to upload files to.
     */
    public String getProject() {
        if (this.project != null) {
            return this.project;
        } else {
            return "";
        }
    }

    /**
     * @return "pub" if the files should be uploaded as public,
     *         "priv" if the files should be uploaded as private.
     */
    public boolean getPubOrPriv() {
        return "pub".equals(pubOrPriv);
    }

    /**
     * @return the ant-style file patterns to match against the local
     *         workspace.
     */
    public FilePattern[] getFilePatterns() {
        if (this.file_patterns != null) {
            return this.file_patterns;
        } else {
            return new FilePattern[0];
        }
    }

    /**
     * @return the path to upload files to on the Cubit host.
     */
    public String getPath() {
        if (this.path != null) {
            return this.path;
        } else {
            return "";
        }
    }

    /**
     * @return whether or not local directory structure should be preserved.
     */
    public boolean getPreserveLocal() {
        return this.preserveLocal;
    }

    /**
     * @return whether or not the upload should continue if matching files
     *         are present.
     */
    public boolean getForce() {
        return this.force;
    }

    /**
     * @return the comment.
     */
    public String getComment() {
        if (this.comment != null) {
            return this.comment;
        } else {
            return "";
        }
    }

    /**
     * @return the description of the files.
     */
    public String getDescription() {
        if (this.description != null) {
            return this.description;
        } else {
            return "";
        }
    }

    /**
     * @return the description of the files.
     */
    public String getRemovePrefixRegex() {
        if (this.removePrefixRegex != null) {
            return this.removePrefixRegex;
        } else {
            return "";
        }
    }
    
    /**
     * setting the listener allows logging to work
     *
     * @param listener to use for logging events.
     */
    private void setupLogging(BuildListener listener) {
        this.listener = listener;
    }

    /**
     * Logging will only work once the listener is set.
     * Otherwise, it will fail (silently).
     *
     * @param message to log.
     */
    private void log(String message) {
        if (this.listener != null) {
            this.listener.getLogger().println(message);
        }
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    /**
     * Upload the files to the PBL.  This is the Jenkins builds entry point into this plugin
     * 
     * @param build the current Jenkins build.
     * @param launcher unused.
     * @param listener for events.
     * @return true if uploading files succeeded.
     * @throws InterruptedException
     * @throws IOException
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws java.lang.InterruptedException,
            IOException {
        this.setupLogging(listener);
        this.log("");
        if (build.getResult() == Result.FAILURE) {
            this.log("Not attempting to upload files to Project Build " +
                     "Library because the build has failed.");
            return true;
        }
        this.log("Uploading files to Project Build Library");
        this.log(this.getRemoteURL(build));

        boolean success = this.uploadFiles(build);
        
        // add result to build page
        build.
            addAction(new 
                      PblUploadResultAction(LEFT_NAV_DISPLAY_MESSAGE, 
                                            IMAGE_URL + "cubit-icon.gif", 
                                            "console", 
                                            addTrailSlash(this.
                                                          getRemoteURL(build)),
                                            success));
        
        if (!success) {
            // set the build result to the worse of the current result
            // and UNSTABLE
            build.setResult(build.getResult().combine(Result.UNSTABLE));
        }

        return success;
    }

    /**
     * Processes the configured list of file patterns to upload to the pbl.
     * All patterns are processed by the method getFilePatterns, and any empty
     * strings (which can result after) processing are ignored.
     * 
     * @param build the current Jenkins build
     * @return List of file patterns, after being interpreted with empty strings
     * removed. 
     */
    private List<String> getProcessedFilePatterns(AbstractBuild<?,?> build)
            throws IOException, InterruptedException {
        List<String> output = new ArrayList<String>();
        for (FilePattern uninterp_fp : this.getFilePatterns()) {
            String file_pattern;
            try {
                file_pattern = uninterp_fp.interpret(build, listener);
                if (!file_pattern.equals("")){
                    output.add(file_pattern);
                }
            } catch (IllegalArgumentException e) {
                this.log("File pattern " + uninterp_fp + " contained a bad "
                         + "env var.  Skipping.");
            }
        }
        return output;
    }
    
    /**
     * This methods is calculates the success or fail of the pbl upload plugin
     * and logs the state to Jenkins build log.  Success occurs if any files
     * are successfully uploaded.
     * 
     * @param num_files Total number of files processed during upload
     * @param failures Total number of files that failed to upload
     * @return true if some files uploaded successfully
     */
    private boolean determineAndLogFinalState(int num_files, int failures){
        num_files = num_files > 0 ? num_files: 0;
        failures = failures > 0 ? failures: 0;
        if (num_files == 0) {
            this.log("Could not find any matching files to upload.  "
                     + "Please check your file patterns.");
            return false;
        } else if (num_files == failures) {
            this.log("No files successfully uploaded.  "
                     + "You may want to check your " +
                     "configuration or the status of " +
                     "the Lab Management Manager.");
            return false;
        } else if (failures == 0) {
            this.log(num_files + " files successfully uploaded!");
            return true;
        } else {
            this.log("Attempted to upload " + num_files + " files.");
            this.log(failures + " file uploads failed.");
            this.log((num_files - failures) + " file uploads succeeded.");
            return true;
        }
    }
    
    /**
     * Upload the files.
     *
     * @param build the current Jenkins build.
     * @return true, if successful, false otherwise.
     * @throws IOException
     * @throws InterruptedException
     */
    private boolean uploadFiles(AbstractBuild<?, ?> build) {
        try {
            FilePath workspace = build.getWorkspace();
            int num_files = 0;
            int failures = 0;
            this.log("");
            for (String file_pattern : getProcessedFilePatterns(build)) {
                this.log("Upload files matching " + file_pattern + ":");
                for (FilePath pathOfFileToUpload : workspace.list(file_pattern)) {
                    num_files++;
                    Map<String, String> args = this.setupArgs(build, 
                                                              pathOfFileToUpload, 
                                                              workspace);
                    if (!this.pblUpload(args, pathOfFileToUpload, workspace)) {
                        failures++;
                    }
                }
                this.log("");
            }
            return determineAndLogFinalState(num_files, failures);
        } catch (Exception e) {
            this.log("CRITICAL ERROR: Upload of files failed due to: " 
                     + e.getMessage());
            return false;
        }      
    }

    /**
     * @return the local path to the uploaded file.
     */
    private String getLocalFilePath(FilePath workspace, 
                                    FilePath uploadFilePath) {
        String path = this.getRelativePath(workspace, uploadFilePath) +
            uploadFilePath.getName();
        if (path.startsWith("/")) {
            path = path.replaceFirst("/", "");
        }
        return path;
    }

    /**
     * @return the URL for where our upload will end up.
     */
    private String getRemoteURL(AbstractBuild<?, ?> build)
            throws IOException, InterruptedException {
        return addTrailSlash(getHostUrl())+"pbl/" + addTrailSlash(this.getProject()) 
            + pubOrPriv + "/"
            + this.getInterpreted(build, this.getPath());           
    }

    /**
     * @param str with or without slash
     * @return string with trailing slash.
     */
    private static String addTrailSlash(String str) {
        if (str.endsWith("/")) {
            return str;
        } else {
            return str + "/";
        }
    }

    /**
     * Figure out what the full path of the uploaded file should be.
     *
     * @param build the current Jenkins build.
     * @param workspace
     * @param uploadFilePath 
     * @return a string with the interpreted path plus possibly the local
     *         directory structure.
     */
    private String createUploadPath(AbstractBuild<?, ?> build, 
                                    FilePath workspace, FilePath uploadFilePath)
                                    throws IOException, InterruptedException {
        String fileDestinationPath = this.getInterpreted(build, this.getPath());
        if (this.getPreserveLocal()){
            String localPath = this.getRelativePath(workspace, uploadFilePath);
            if (this.removePrefixRegex != null && 
                !"".equals(this.removePrefixRegex)) {
                if (localPath.split(removePrefixRegex).length > 0 &&
                    // makes sure the regex is a prefix
                    localPath.split(removePrefixRegex)[0].equals("")){ 
                    localPath = localPath.replaceFirst(removePrefixRegex, "");
                }
                if (localPath.matches(removePrefixRegex)) {
                    // if the entire localPath matches, it's removed entirely
                    localPath = "";
                }
            }
            fileDestinationPath = addTrailSlash(fileDestinationPath) + 
                localPath;
        }
        return fileDestinationPath;
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
        Map<String, String> envVars = null;
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
            //XXX should this use envVars instead of build.getEnv.... ?
            return CommonUtil.getInterpreted(build.getEnvironment(TaskListener.NULL), str);
        } catch (IllegalArgumentException iae) {
            this.log(iae.getMessage());
            throw iae;
        }
    }

    /**
     * Returns the relative directory path between a child and it's ancestor
     * FilePath. For example, if ancestor = /a/b/c/d/ and child =
     * /a/b/c/d/e/f/g.txt this function will return /e/f. If ancestor is not
     * really an ancestor, "" will be returned.
     * 
     * @param ancestor
     * @param child
     * @return the child's directory, relative to the ancestor's.
     */
    private String getRelativePath(FilePath ancestor, FilePath child) {
        try {
            String ancestor_str = ancestor.toURI().toString();
            String child_str = child.getParent().toURI().toString();
            if (child_str.startsWith(ancestor_str)) {
                return child_str.replaceFirst(ancestor_str, "");
            }
        } catch (InterruptedException e){
            //TODO: perhaps log this.
        } catch (IOException e){
            //TODO: perhaps log this.
        } 
        return "";
    }

    /**
     * Setup of the args needed for uploading files.
     * 
     * @param build the current Jenkins build.
     * @param uploadFilePath
     * @param workspace
     * @return a key, value map of the args.
     * @throws IOException
     * @throws InterruptedException
     */
    private Map<String, String> setupArgs(AbstractBuild<?, ?> build,
                                          FilePath uploadFilePath,
                                          FilePath workspace) 
        throws IOException, InterruptedException {
        Map<String, String> args = new HashMap<String, String>();
        String md5sum = uploadFilePath.digest();
        String path;
        String description;
        String comment;
        try {
            path = this.createUploadPath(build, workspace, uploadFilePath);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Setting up args failed due to"
                                               + " bad path: " 
                                               + e.getMessage());
        }
        try {
            description = this.getInterpreted(build, this.getDescription());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Setting up args failed due to"
                                               + " bad description: " 
                                               + e.getMessage());
        }
        try {
            comment = this.getInterpreted(build, this.getComment());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Setting up args failed due to"
                                               + " bad comment: " 
                                               + e.getMessage());
        }
        args.put("md5sum", md5sum);
        args.put("path", path);
        args.put("proj", this.getProject());
        args.put("type", pubOrPriv);
        args.put("userid", this.getUser());
        if (this.getForce()){
            /* The value is irrelevant, the presence of the parameter is 
             * all the server needs to force the upload 
             */
            args.put("force","TRUE");
        } 
        args.put("comment", comment);
        args.put("desc", description);
        return args;
    }

    
    private void logPblCallResults(CubitConnector.ResponseCodeAndBody response,
                                   Map <String, String> args,
                                   FilePath uploadFilePath,
                                   FilePath workspace){
        String localFilePath = this.getLocalFilePath(workspace,
                                                     uploadFilePath);
        String remoteURL = addTrailSlash(addTrailSlash(getHostUrl()) + "pbl/" 
                                         + addTrailSlash(args.get("proj")) 
                                         + args.get("type") + "/"
                                         + args.get("path") 
                                        ) + uploadFilePath.getName();
        String resultStr = "Upload for file " + localFilePath
                           + " -> " + remoteURL;
        if (response.getStatus() == 200) {
           this.log(resultStr + ": OK");
        } 
        else {
            String[] lines = response.getBody().split("\\n");
            this.log("Upload for file " + uploadFilePath.getName() + 
            " failed: ");
            for (String line : lines) {
                this.log(line);
            }
            this.log(resultStr + ": FAILED");
        }
    }
    
    
    /**
     * Do the upload.
     * 
     * @param args to send to the Lab Management server.
     * @param uploadFilePath to the uploading file.
     * @return true, if successful.
     * @throws IOException
     * @throws InterruptedException
     */
    private boolean pblUpload(final Map<String, String> args, 
                              FilePath uploadFilePath,
                              FilePath workspace) 
        throws IOException, InterruptedException {
        CubitConnector.ResponseCodeAndBody result = uploadFilePath.
            act(new FileCallable<CubitConnector.ResponseCodeAndBody>() {
			private static final long serialVersionUID = 1L;
                        @Override
			public CubitConnector.ResponseCodeAndBody invoke(File file, VirtualChannel channel) 
            throws IOException
            {
		        final CubitConnector cubitConnector = new CubitConnector(getHostUrl(),
		                                                                 getUser(),
		                                                                 getKey());
 			    return cubitConnector.callCubit(PBL_UPLOAD_URL, 
                                                args, 
                                                file,
                                                true);
            }
            @Override
            public void checkRoles(RoleChecker arg0) throws SecurityException {
                // TODO Auto-generated method stub
            }
        });
        logPblCallResults(result, args, uploadFilePath, workspace);
        return(result.getStatus() == 200);
    }

    /**
     * PBLUploader does not need to wait til the build is finalized.
     */
    @Override
    public boolean needsToRunAfterFinalized() {
        return false;
    }

    /**
     * Descriptor for {@link PblUploader}. Used as a singleton. The class is
     * marked as public so that it can be accessed from views.
     * 
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        /**
         * @return human-readable name used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "Lab Management Project Build Library (PBL) Uploader";
        }

        /**
         * The PblUploader can be used as a post-promotion task.
         *
         * @param jobType
         * @return true
         */
        @Override
        @SuppressWarnings("unchecked")
        public boolean isApplicable(java.lang.Class<? extends AbstractProject> jobType) {
            return true;
        } 

        /**
         * Form validation for the host_url
         *
         * @param value url
         */
        public FormValidation doCheckHostUrl(@QueryParameter String value) {
            return CNFormFieldValidator.hostUrlCheck(value);
        }

        public FormValidation doCheckUser(@QueryParameter String value) {
            return CNFormFieldValidator.requiredCheck(value, "user name");
        }

        public FormValidation doCheckProject(@QueryParameter String value) {
            return CNFormFieldValidator.requiredCheck(value, "project");
        }

        /**
         * Form validation for the API key.
         */
        public FormValidation doCheckKey(@QueryParameter String hostUrl, @QueryParameter String user, @QueryParameter String key) {
            return CNFormFieldValidator.cubitKeyCheck(hostUrl,user,key);
        }

        public FormValidation doCheckPath(@QueryParameter String value) throws FormValidation {
            return CNFormFieldValidator.requiredInterpretedCheck(value, "path");
        }

        
        /**
         * Form validation for the path.
         *
         * @param value
         */
        public FormValidation doCheckRemovePrefixRegex(@QueryParameter String value) {
            return CNFormFieldValidator.regexCheck(value);
        }

        public FormValidation doCheckDescription(@QueryParameter String value) throws FormValidation {
            return CNFormFieldValidator.unrequiredInterpretedCheck(value, "description");
        }

        public FormValidation doCheckComment(@QueryParameter String value) throws FormValidation {
            return CNFormFieldValidator.unrequiredInterpretedCheck(value, "description");
        }
    }
}
