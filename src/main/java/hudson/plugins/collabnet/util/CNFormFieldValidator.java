package hudson.plugins.collabnet.util;

import com.collabnet.ce.webservices.CollabNetApp;
import com.collabnet.ce.webservices.DocumentApp;

import com.collabnet.cubit.api.CubitConnector;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.ServletException;

import hudson.util.FormFieldValidator;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.axis.utils.StringUtils;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public abstract class CNFormFieldValidator extends FormFieldValidator {
    private static Logger log = Logger.getLogger("CNFormFieldValidator");

    protected CNFormFieldValidator(StaplerRequest request, 
                                   StaplerResponse response) {
        // no special permisssion is required for our checks
        // without proper rights, no data will be returned from 
        // CollabNet server anyhow.
        super(request, response, false);
    }

    /**
     * Utility function to check that a str contains only valid
     * enviromental variables to interpret.
     *
     * @param str the string to test.
     * @return error message, if any variables are missing, null if all
     *         are found.
     */
    public static String checkInterpretedString(String str) {
        Pattern envPat = Pattern.compile("\\$\\{(\\w*)\\}");
        Matcher matcher = envPat.matcher(str);
        Set<String> envVars = new HashSet<String>(9);
        envVars.add("BUILD_NUMBER");
        envVars.add("BUILD_ID");
        envVars.add("JOB_NAME");
        envVars.add("BUILD_TAG");
        envVars.add("EXECUTOR_NUMBER");
        envVars.add("JAVA_HOME");
        envVars.add("WORKSPACE");
        envVars.add("HUDSON_URL");
        envVars.add("SVN_REVISION");
        envVars.add("CVS_BRANCH");
        String message = null;
        while (matcher.find()) {
            String key = matcher.group(1);
            if (!envVars.contains(key)) {
                if (message == null) {
                    message = "Environmental Variables not found: " + key;
                } else {
                    message += ", " + key;
                }
            }
        }
        return message;
    }

    /**
     * Returns true if a url is valid, false otherwise.
     */
    public static boolean checkUrl(String url) {
        HttpClient client = new HttpClient();
        try {
            GetMethod get = new GetMethod(url);
            int status = client.executeMethod(get);
            if (status == 200) {
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            return false;
        } catch (IllegalArgumentException iae) {
            return false;
        } 
    }

    /**
     * Class for checking that a required value is set.  Expects a 
     * StaplerRequest with a value set to the value and a name set to
     * the name of what is being set (used for error msg).
     */
    public static class RequiredCheck extends CNFormFieldValidator {
        
        public RequiredCheck(StaplerRequest request, 
                             StaplerResponse response) {
            super(request, response);
        }

        protected void check() throws IOException, ServletException {
            String value = StringUtils.strip(request.getParameter("value"));
            String name = request.getParameter("name");
            if (CommonUtil.unset(name)) {
                // ideally this should be set
                name = "above value";
            }
            if (CommonUtil.unset(value)) {
                error("The " + name + " is required.");
                return;
            }    
            ok();
        }
    }

    /**
     * Class for checking an unrequired value that may include
     * interpreted strings (i.e. Hudson environmental values). Expects a 
     * StaplerRequest with value.  If it's a required value, expects a 
     * value name.
     */
    public static class InterpretedCheck 
        extends CNFormFieldValidator {
        
        private boolean isRequired;

        public InterpretedCheck(StaplerRequest request,
                                StaplerResponse response,
                                boolean isRequired) {
            super(request, response);
            this.isRequired = isRequired;
        }
        
        protected void check() throws IOException, ServletException {
            String str = request.getParameter("value");
            if (CommonUtil.unset(str)) {
                if (!this.isRequired) {
                    ok();
                    return;
                } else {
                    String name = request.getParameter("name");
                    if (CommonUtil.unset(name)) {
                        // ideally this should be set
                        name = "above value";
                    }
                    error("The " + name + " is required.");
                    return;
                }
            }
            String errmsg;
            if ((errmsg = checkInterpretedString(str)) != null) {
                error(errmsg);
                return;
            }
            
            ok();
        }
    }

    /**
     * Class for checking an interpreted string which is unrequired.
     */
    public static class UnrequiredInterpretedCheck extends InterpretedCheck {
        
        public UnrequiredInterpretedCheck(StaplerRequest request,
                                StaplerResponse response) {
            super(request, response, false);
        }
    }

    /**
     * Class for checking an interpreted string which is required.
     */
    public static class RequiredInterpretedCheck extends InterpretedCheck {
        
        public RequiredInterpretedCheck(StaplerRequest request,
                                StaplerResponse response) {
            super(request, response, true);
        }
    }

    /**
     * Class for checking if a Host URL is correct.  Expects a StaplerRequest 
     * with value set to the url.
     */
    public static class HostUrlCheck extends CNFormFieldValidator {
        
        public HostUrlCheck(StaplerRequest request, 
                            StaplerResponse response) {
            super(request, response);
        }

        protected void check() throws IOException, ServletException {
            String hostUrl = request.getParameter("value");
            if (CommonUtil.unset(hostUrl)) {
                error("The Host URL is required.");
                return;
            }
            Protocol acceptAllSsl = 
                new Protocol("https", 
                             (ProtocolSocketFactory)
                             new EasySSLProtocolSocketFactory(),
                             443);
            Protocol.registerProtocol("https", acceptAllSsl);
            if (!checkUrl(hostUrl)) {
                error("Invalid Host URL.");
                return;
            }
            ok(); 
        }
    }

    /**
     * Class for checking if a URL is correct and corresponds to a 
     * CollabNet server.  Expects a StaplerRequest with value set
     * to the url.
     */
    public static class SoapUrlCheck extends CNFormFieldValidator {
        
        public SoapUrlCheck(StaplerRequest request, 
                            StaplerResponse response) {
            super(request, response);
        }

        protected void check() throws IOException, ServletException {
            String collabNetUrl = request.getParameter("value");
            if (CommonUtil.unset(collabNetUrl)) {
                error("The CollabNet TeamForge URL is required.");
                return;
            }
            if (!checkSoapUrl(collabNetUrl)) {
                error("Invalid CollabNet TeamForge URL.");
                return;
                    }
            ok(); 
        }

        /**
         * Check that a URL has the expected SOAP service.
         *
         * @param collabNetUrl for the CollabNet server
         * @return returns true if we can get a wsdl from the url, which
         *         indicates that it's a working CollabNet server.
         */
        private boolean checkSoapUrl(String collabNetUrl) {
            String soapURL = collabNetUrl + CollabNetApp.SOAP_SERVICE + 
                "CollabNet?wsdl";
            return checkUrl(soapURL);
        }
    }

    /**
     * Class for checking that a login to CollabNet is valid.  Expects
     * a StaplerRequest with url, username, and password set.
     */
    public static class LoginCheck extends CNFormFieldValidator {

        public LoginCheck(StaplerRequest request, StaplerResponse response) {
            super(request, response);
        }

        protected void check() throws IOException, ServletException {
            String password = request.getParameter("password");
            
            if (CommonUtil.unset(password)) {
                error("The password is required.");
                return;
            }
            CollabNetApp cna = CNHudsonUtil.getCollabNetApp(request);
            if (cna == null) {
                warning("Login fails with this CollabNet " +
                        "URL/username/password combination.");
                CNHudsonUtil.logoff(cna);
                return;
            } else {
                CNHudsonUtil.logoff(cna);
            }
            ok();
        }
    }
    
    /**
     * Class for checking that a project name is valid.  Expects a 
     * StaplerRequest with url, username, password, and project set.
     */
    public static class ProjectCheck extends CNFormFieldValidator {
        
        public ProjectCheck(StaplerRequest request, 
                            StaplerResponse response) {
            super(request, response);
        }

        protected void check() throws IOException, ServletException {
            String username = CNHudsonUtil.getUsername(request);
            String project = request.getParameter("project");
      
            if (CommonUtil.unset(project)) {
                error("The project is required.");
                return;
            }
            CollabNetApp cna = CNHudsonUtil.getCollabNetApp(request);
            if (cna != null) {
                if (CNHudsonUtil.getProjectId(cna, project) == null) {
                    warning("This project cannot be found, or user " +
                            username + " does not have permission " +
                            "to access it.");
                    CNHudsonUtil.logoff(cna);
                    return;
                }
                CNHudsonUtil.logoff(cna);
            }
            ok();
        }
    }

    /**
     * Class to check that the path to a document exists.  Warns about 
     * any missing folders.  Expects a StaplerRequest with url, username,
     * password, project, and path.
     */
    public static class DocumentPathCheck extends CNFormFieldValidator {

        public DocumentPathCheck(StaplerRequest request, 
                                 StaplerResponse response) {
            super(request, response);
        }
        
        protected void check() throws IOException, ServletException {  
            String project = request.getParameter("project");
            String path = request.getParameter("path");
            path = path.replaceAll("/+", "/");
            path = CommonUtil.stripSlashes(path);
            if (CommonUtil.unset(path)) {
                error("The path is required.");
                return;
            }
            String errmsg = null;
            if ((errmsg = checkInterpretedString(path)) != null) {
                error(errmsg);
                return;
            }
            CollabNetApp cna = CNHudsonUtil.getCollabNetApp(request);
            String projectId = CNHudsonUtil.getProjectId(cna, project);
            if (projectId != null) {
                DocumentApp da = new DocumentApp(cna);
                String missing = da.verifyPath(projectId, path);
                if (missing != null) {
                    warning("Folder '" + missing + "' could not be " +
                            "found in path '" + path + "'.  It (and " +
                            "any subfolders) will " +
                            "be created dynamically.");
                    CNHudsonUtil.logoff(cna);
                    return;
                }
            }
            CNHudsonUtil.logoff(cna);
            ok();
        }
    }

    /**
     * Class to check that a package exists.  Expects a StaplerRequest with 
     * a url, username, password, project, and package.
     */
    public static class PackageCheck extends CNFormFieldValidator {

        public PackageCheck(StaplerRequest request, 
                            StaplerResponse response) {
            super(request, response);
        }

        protected void check() throws IOException, ServletException {
            String rpackage = request.getParameter("package");
            String project = request.getParameter("project");
            if (CommonUtil.unset(rpackage)) {
                error("The package is required.");
                return;
            }
            CollabNetApp cna = CNHudsonUtil.getCollabNetApp(request);
            String projectId = CNHudsonUtil.getProjectId(cna, project);
            if (projectId != null) {
                String packageId = CNHudsonUtil.getPackageId(cna, rpackage, 
                                                             projectId);
                if (packageId == null) {
                    warning("Package could not be found.");
                    CNHudsonUtil.logoff(cna);
                    return;
                }
            }
            CNHudsonUtil.logoff(cna);
            ok();
        }
    }

    /**
     * Class to check that a release exists.  Expects a StaplerRequest with 
     * a url, username, password, project, package (optional), and release.
     */
    public static class ReleaseCheck extends CNFormFieldValidator {
        
        public ReleaseCheck(StaplerRequest request, 
                            StaplerResponse response) {
            super(request, response);
        }

        protected void check() throws IOException, ServletException {
            String release = request.getParameter("release");
            String rpackage = request.getParameter("package");
            String project = request.getParameter("project");
            String required = request.getParameter("required");
            if (CommonUtil.unset(release)) {
                if (required.toLowerCase().equals("true")) {
                    error("The release is required.");
                } else {
                    ok();
                }
                return;
            }
            CollabNetApp cna = CNHudsonUtil.getCollabNetApp(request);
            String projectId = CNHudsonUtil.getProjectId(cna, project);
            String packageId = CNHudsonUtil.getPackageId(cna, rpackage, 
                                                         projectId);
            if (packageId != null) {
                String releaseId = CNHudsonUtil.getReleaseId(cna, packageId, 
                                                             release);
                if (releaseId == null) {
                    warning("Release could not be found.");
                    CNHudsonUtil.logoff(cna);
                    return;
                }
            } else if (projectId != null) {
                String releaseId = CNHudsonUtil.getProjectReleaseId(cna, 
                                                                    projectId, 
                                                                    release);
                if (releaseId == null) {
                    warning("Release could not be found.");
                    CNHudsonUtil.logoff(cna);
                    return;
                }
            }
            CNHudsonUtil.logoff(cna);
            ok();            
        }
    }

    /**
     * Class to check that a tracker exists.  Expects a StaplerRequest with 
     * a url, username, password, project, and tracker.
     */
    public static class TrackerCheck extends CNFormFieldValidator {

        public TrackerCheck(StaplerRequest request, 
                            StaplerResponse response) {
            super(request, response);
        }

        protected void check() throws IOException, ServletException {
            String tracker = request.getParameter("tracker");
            String project = request.getParameter("project");
            if (CommonUtil.unset(tracker)) {
                error("The tracker is required.");
                return;
            }
            CollabNetApp cna = CNHudsonUtil.getCollabNetApp(request);
            String projectId = CNHudsonUtil.getProjectId(cna, project);
            if (projectId != null) {
                String trackerId = CNHudsonUtil.getTrackerId(cna, projectId,
                                                             tracker);
                if (trackerId == null) {
                    warning("Tracker could not be found.");
                    CNHudsonUtil.logoff(cna);
                    return;
                }
            }
            CNHudsonUtil.logoff(cna);
            ok();
        }
    }

    /**
     * Class for checking if a user can be assigned a tracker artifact.  
     * Expects a StaplerRequest with login info (url, username, password), 
     * project, and assign (which is the username).
     */
    public static class AssignCheck extends CNFormFieldValidator {

        public AssignCheck(StaplerRequest request, 
                            StaplerResponse response) {
            super(request, response);
        }

        protected void check() throws IOException, ServletException {
            String assign = StringUtils.strip(request.getParameter("assign"));
            if (CommonUtil.unset(assign)) {
                ok();
                return;
            }
            String project = request.getParameter("project");
            CollabNetApp cna = CNHudsonUtil.getCollabNetApp(request);
            if (cna == null) {
                ok();
                return;
            }
            if (!CNHudsonUtil.isUserValid(cna, assign)) {
                warning("This user cannot be found.");
                CNHudsonUtil.logoff(cna);
                return;
            }
            String projectId = CNHudsonUtil.getProjectId(cna, project);
            if (projectId == null) {
                ok();
                CNHudsonUtil.logoff(cna);
                return;
            }
            if (!CNHudsonUtil.isUserMember(cna, assign, projectId)) {
                warning("This user is not a member of the " +
                        "project.");
                CNHudsonUtil.logoff(cna);
                return;
            }
            CNHudsonUtil.logoff(cna);
            ok();
        }
    }


    /**
     * Class to check for validity of a regex expression.  Expects
     * a StaplerRequest with value set.  
     */
    public static class RegexCheck extends CNFormFieldValidator {
        
        public RegexCheck(StaplerRequest request, 
                                StaplerResponse response) {
            super(request, response);
        }

        protected void check() throws IOException, ServletException {
            String regex = request.getParameter("value");
            if(!CommonUtil.unset(regex)) {
                try {
                    Pattern.compile(regex);
                } catch (PatternSyntaxException ex){
                    error("The regular expression is not syntactically "
                          + "correct.");
                    return;
                }
            }
            ok();
        }
    }

    /**
     * Class to check if a CUBiT key has the proper format and allows
     * login.  Expects a StaplerRequest with value (key), hostURL, and user
     * set.
     */
    public static class CubitKeyCheck extends CNFormFieldValidator {
        
        public CubitKeyCheck(StaplerRequest request, 
                             StaplerResponse response) {
            super(request, response);
        }

        protected void check() throws IOException, ServletException {
            String key = request.getParameter("value");
            String hostURL = request.getParameter("hostURL");
            String user = request.getParameter("user");
            if (CommonUtil.unset(key)) {
                error("The user API key is required.");
                return;
            }
            if (!key.matches("\\p{XDigit}{8}-\\p{XDigit}{4}"
                             + "-\\p{XDigit}{4}-\\p{XDigit}{4}"
                             + "-\\p{XDigit}{12}")) {
                if (key.startsWith(" ")) {
                    error("The key's format is invalid.  "
                          + "There is a leading space.");
                } else if (key.endsWith(" ")) {
                    error("The key's format is invalid.  "
                          + "There is a trailing space.");
                } else {
                    error("The key's format is invalid.");
                }
                return;
            }
            if (!CommonUtil.unset(hostURL) && !CommonUtil.unset(user)) {
                boolean success = false;
                try {
                    success = signedStatus(hostURL, user, key);
                } catch (IllegalArgumentException iae) {
                    // failure
                    success = false;
                }
                if (!success) {
                    warning("This host URL, username, and user API "
                            + "key combination cannot successfully "
                            + "sign in.");
                    return;
                }
            }
            ok();
        }

        /**
         * Utility function to check that host, user, and key work.
         *
         * @param host URL.
         * @param user to login as.
         * @param key to login with.
         * @return true if the status is good.
         */
        private boolean signedStatus(String host, String user, String key) {
            key = key.toLowerCase();
            CubitConnector conn = new CubitConnector(host, user, key);
            String status;
            try {
                status = conn.callCubitApi("status_signed", 
                                              new HashMap<String, String>(), 
                                              true);
            } catch (IOException e) {
                return false;
            }
            Pattern pat = Pattern.compile(".*OK.*", Pattern.DOTALL);
            if (pat.matcher(status).matches()) {
                return true;
            } else {
                return false;
            }
        }
    }
}
