package hudson.plugins.collabnet.pblupload;

import hudson.FilePath;
import hudson.Launcher;
import hudson.FilePath.FileCallable;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.plugins.promoted_builds.Promotion;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormFieldValidator;

import hudson.plugins.collabnet.util.CommonUtil;
import hudson.plugins.collabnet.util.CNFormFieldValidator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.collabnet.cubit.api.CubitConnector;

/**
 * The PblUploader is used to upload files to the Project Build Library (Pbl) 
 * of a Lab Management manager node.
 */
public class PblUploader extends Publisher implements java.io.Serializable {
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
    private String pubOrPriv = "priv";
    private String[] file_patterns;
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
     * Descriptor should be singleton.
     */
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    /**
     * Constructs a new PblUploader instance.
     * 
     * @param hostUrl for the Cubit host.
     * @param user to login as.
     * @param key to login with.
     * @param project to upload files to
     * @param pubOrPriv whether these files should be in the pub (public)
     *                  or priv (private) directory.
     * @param file_patterns matching local files that should be uploaded.
     * @param path one the Cubit host where the files will be uploaded.
     * @param preserveLocal if true, local directory structure will be copied
     *                      to the server.
     * @param force if true, the files will be uploaded, even if they will
     *              overwrite existing files.
     * @param comment about the upload.
     * @param description of the files.
     */
    public PblUploader(String hostUrl, String user, String key, String project,
                       String pubOrPriv, String[] file_patterns, String path, 
                       boolean preserveLocal, boolean force, String comment, 
                       String description, String removePrefixRegex) {
        this.hostUrl = hostUrl;
        this.user = user;
        // our sig generator depends on the letters in our hex
        // being lower case
        this.key = key.trim().toLowerCase();
        this.project = project;
        this.pubOrPriv = pubOrPriv;
        this.file_patterns = file_patterns;
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
    public String getPubOrPriv() {
        if (this.pubOrPriv != null) {
            return this.pubOrPriv;
        } else {
            return "";
        }
    }

    /**
     * @return the ant-style file patterns to match against the local
     *         workspace.
     */
    public String[] getFilePatterns() {
        if (this.file_patterns != null) {
            return this.file_patterns;
        } else {
            return new String[0];
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

    /**
     * Upload the files to the PBL.  This is the hudson builds entry point into this plugin
     * 
     * @param build the current Hudson build.
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
        boolean success = true;
        this.setupLogging(listener);
        this.log("");
        if (build.getResult() == Result.FAILURE) {
            this.log("Not attempting to upload files to Project Build " +
                     "Library because the build has failed.");
            return true;
        }
        this.log("Uploading files to Project Build Library");
        this.log(this.getRemoteURL(build));

        success = this.uploadFiles(build);
        
        // add result to build page
        build.
            addAction(new 
                      PblUploadResultAction(LEFT_NAV_DISPLAY_MESSAGE, 
                                            IMAGE_URL + "cubit-icon.gif", 
                                            "console", 
                                            addTrailSlash(this.
                                                          getRemoteURL(build)),
                                            success));
        
        if (success == false) {
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
     * @param build the current hudson build
     * @return List of file patterns, after being interpreted with empty strings
     * removed. 
     */
    private List<String> getProcessedFilePatterns(AbstractBuild<?, ?> build){
        List<String> output = new ArrayList<String>();
        for (String uninterp_fp : this.getFilePatterns()) {
            String file_pattern = "";
            try {
                file_pattern = getInterpreted(build, uninterp_fp);
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
     * and logs the state to Hudson build log.  Success occurs if any files
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
     * @param build the current Hudson build.
     * @return true, if successful, false otherwise.
     * @throws IOException
     * @throws InterruptedException
     */
    private boolean uploadFiles(AbstractBuild<?, ?> build) {
        try {
            FilePath workspace = build.getProject().getWorkspace();
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
    private String getRemoteURL(AbstractBuild<?, ?> build) {
        return addTrailSlash(getHostUrl())+"pbl/" + addTrailSlash(this.getProject()) 
            + this.getPubOrPriv() + "/"
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
     * @param build the current Hudson build.
     * @param workspace
     * @param uploadFilePath 
     * @return a string with the interpreted path plus possibly the local
     *         directory structure.
     */
    private String createUploadPath(AbstractBuild<?, ?> build, 
                                    FilePath workspace, 
                                    FilePath uploadFilePath) {
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
            return CommonUtil.getInterpreted(build.getEnvVars(), str);
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
     * @param build the current Hudson build.
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
        args.put("type", this.getPubOrPriv());
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
            for (int i = 0; i < lines.length; i++) {
                this.log(lines[i]);
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
			public CubitConnector.ResponseCodeAndBody invoke(File file, VirtualChannel channel) 
            throws FileNotFoundException, IOException
            {
		        final CubitConnector cubitConnector = new CubitConnector(getHostUrl(),
		                                                                 getUser(),
		                                                                 getKey());
 			    return cubitConnector.callCubit(PBL_UPLOAD_URL, 
                                                args, 
                                                file,
                                                true);
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
     * @return the descriptor.
     */
    public Descriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    /**
     * Descriptor for {@link PblUploader}. Used as a singleton. The class is
     * marked as public so that it can be accessed from views.
     * 
     */
    public static final class DescriptorImpl 
        extends BuildStepDescriptor<Publisher> {

        private static int unique = 0;

        DescriptorImpl() {
            super(PblUploader.class);
        }

        /**
         * @return a unique integer, used to identify an instance
         *         of the PBLUploader plugin on a page.
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
         * @return human-readable name used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "Lab Management Project Build Library (PBL) Uploader";
        }

        /**
         * @return the path to the help files.
         */
        public String getHelpUrl() {
            return "/plugin/collabnet/pblupload/";
        }

        /**
         * @return url for the generic help-file for this plug-in.
         */
        @Override
        public String getHelpFile() {
            return getHelpUrl() + "help-projectConfig.html";
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
         * Creates a new instance of {@link PblUploader} from 
         * a submitted form.
         *
         * @param req config page parameters.
         * @param formData data specific to this section, in json form.
         * @return new PblUploader instance.
         * @throws FormException
         */
        @Override
        public PblUploader newInstance(StaplerRequest req, 
                                       JSONObject formData) 
        throws FormException {
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
            
            return new PblUploader((String)formData.get("host_url"), 
                                   (String)formData.get("user"), 
                                   (String)formData.get("key"),
                                   (String)formData.get("project"),
                                   (String)formData.get("pub_or_priv"),
                                   patterns,
                                   (String)formData.get("path"),
                                   formData.get("preserve") != null,
                                   CommonUtil.getBoolean("force", formData),
                                   (String)formData.get("comment"),
                                   (String)formData.get("description"),
                                   formData.get("preserve") != null?
                                       (String)((JSONObject)formData.
                                           get("preserve")).
                                           get("remove_prefix_regex"):
                                       ""
                                   );
        }
        
        /**
         * Form validation for the host_url
         *
         * @param req StaplerRequest which contains parameters from 
         *            the config.jelly.
         * @param rsp contains http response data (unused).
         * @throws IOException
         * @throws ServletException
         */
        public void doHostUrlCheck(StaplerRequest req, 
                                   StaplerResponse rsp) 
            throws IOException, ServletException {
            new CNFormFieldValidator.HostUrlCheck(req,rsp).process();
        }

        /**
         * Form validation for the user and project
         *
         * @param req StaplerRequest which contains parameters from 
         *            the config.jelly.
         * @param rsp contains http response data (unused).
         * @throws IOException
         * @throws ServletException
         */
        public void doRequiredCheck(StaplerRequest req, 
                                       StaplerResponse rsp) 
            throws IOException, ServletException {
            new CNFormFieldValidator.RequiredCheck(req,rsp).process();
        }

        /**
         * Form validation for the API key.
         *
         * @param req StaplerRequest which contains parameters from 
         *            the config.jelly.
         * @param rsp contains http response data (unused).
         * @throws IOException
         * @throws ServletException
         */
        public void doKeyCheck(StaplerRequest req, 
                                       StaplerResponse rsp) 
            throws IOException, ServletException {
            new CNFormFieldValidator.CubitKeyCheck(req, rsp).process();
        }

        /**
         * Form validation for the path.
         *
         * @param req StaplerRequest which contains parameters from 
         *            the config.jelly.
         * @param rsp contains http response data (unused).
         * @throws IOException
         * @throws ServletException
         */
        public void doRequiredInterpretedCheck(StaplerRequest req, 
                                               StaplerResponse rsp) 
            throws IOException, ServletException {
            new CNFormFieldValidator.RequiredInterpretedCheck(req,rsp)
                .process();
        }

        
        /**
         * Form validation for the path.
         *
         * @param req StaplerRequest which contains parameters from 
         *            the config.jelly.
         * @param rsp contains http response data (unused).
         * @throws IOException
         * @throws ServletException
         */
        public void doRegexPrefixCheck(StaplerRequest req, 
                                       StaplerResponse rsp) 
        throws IOException, ServletException {
            new CNFormFieldValidator.RegexCheck(req, rsp).process();
        }
        
        /**
         * Form validation for the comment and description.
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
            new CNFormFieldValidator.UnrequiredInterpretedCheck(req,rsp)
                .process();
        }
    }
}
