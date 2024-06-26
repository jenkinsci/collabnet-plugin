package hudson.plugins.collabnet.util;

import com.collabnet.ce.webservices.CTFPackage;
import com.collabnet.ce.webservices.CTFProject;
import com.collabnet.ce.webservices.CTFRelease;
import com.collabnet.ce.webservices.CTFScmRepository;
import com.collabnet.ce.webservices.CTFTracker;
import com.collabnet.ce.webservices.CollabNetApp;
import hudson.plugins.collabnet.auth.CNAuthentication;
import hudson.plugins.collabnet.CtfSoapHttpSender;
import hudson.util.FormValidation;
import hudson.util.Secret;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public abstract class CNFormFieldValidator {
//    private static Logger log = Logger.getLogger("CNFormFieldValidator");

        // no special permisssion is required for our checks
        // without proper rights, no data will be returned from 
        // CollabNet server anyhow.

    /**
     * Utility function to check that a str contains only valid
     * environmental variables to interpret.
     *
     * @param str the string to test.
     * @throws FormValidation  error message, if any variables are missing, null if all
     *         are found.
     */
    public static void checkInterpretedString(String str) throws FormValidation {
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
        if (message!=null)
            throw FormValidation.error(message);
    }

    /**
     * Returns form validation that represents the validity of the URL.
     */
    public static FormValidation checkUrl(String url) {
        CloseableHttpClient httpClient = null;
        try {
            httpClient = getHttpClient();
            HttpGet httpGet = new HttpGet(url);
            CloseableHttpResponse r = httpClient.execute(httpGet);
            try {
                if (r.getStatusLine().getStatusCode() == 200) {
                    return FormValidation.ok();
                }
                else {
                    return FormValidation.error(url+" reported HTTP status code "+ r.getStatusLine().getStatusCode() +
                            " with resaon " + r.getStatusLine().getReasonPhrase());
                }
            }
            finally {
                if (r != null) {
                    r.close();
                }
            }
        } catch (Exception e) {
            return FormValidation.error(e,"Failed to connect to "+url+" : "+e.getMessage());
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public static CloseableHttpClient getHttpClient()
            throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        if (CollabNetApp.areSslErrorsIgnored()) {
            // finally create the HttpClient using HttpClient factory methods and assign the ssl socket factory
            return HttpClients
                    .custom()
                    .setSSLSocketFactory(CtfSoapHttpSender.tryCreateAcceptAllSslSocketFactory())
                    .build();
        }
        else {
            return HttpClients.createDefault();
        }
    }

    /**
     * Class for checking that a required value is set.  Expects a 
     * StaplerRequest with a value set to the value and a name set to
     * the name of what is being set (used for error msg).
     */
    public static FormValidation requiredCheck(String value, String name) {
        if (CommonUtil.unset(name)) {
            // ideally this should be set
            name = "above value";
        }
        if (CommonUtil.isEmpty(value)) {
            return FormValidation.error("The " + name + " is required.");
        }
        return FormValidation.ok();
    }

    /**
     * Class for checking an unrequired value that may include
     * interpreted strings (i.e. Jenkins environmental values). Expects a 
     * StaplerRequest with value.  If it's a required value, expects a 
     * value name.
     */
    public static FormValidation interpretedCheck(String str, String name, boolean isRequired) throws FormValidation {
        if (CommonUtil.unset(str)) {
            if (!isRequired) {
                return FormValidation.ok();
            } else {
                if (CommonUtil.unset(name)) {
                    // ideally this should be set
                    name = "above value";
                }
                return FormValidation.error("The " + name + " is required.");
            }
        }
        checkInterpretedString(str);

        return FormValidation.ok();
    }

    /**
     * Class for checking an interpreted string which is unrequired.
     */
    public static FormValidation unrequiredInterpretedCheck(String str, String name) throws FormValidation {
        return interpretedCheck(str, name, false);
    }

    /**
     * Class for checking an interpreted string which is required.
     */
    public static FormValidation requiredInterpretedCheck(String str, String name) throws FormValidation {
        return interpretedCheck(str, name, true);
    }

    /**
     * Class for checking if a Host URL is correct.  Expects a StaplerRequest 
     * with value set to the url.
     */
    public static FormValidation hostUrlCheck(String hostUrl) {
        if (CommonUtil.unset(hostUrl)) {
            return FormValidation.error("The Host URL is required.");
        }
        return checkUrl(hostUrl);
    }

    /**
     * Class for checking if a URL is correct and corresponds to a 
     * CollabNet server.  Expects a StaplerRequest with value set
     * to the url.
     */
    public static FormValidation soapUrlCheck(String collabNetUrl) {
        if (CommonUtil.unset(collabNetUrl)) {
            return FormValidation.error("The CollabNet TeamForge URL is required.");
        }
        return checkSoapUrl(collabNetUrl);
    }

    /**
     * Check that a URL has the expected SOAP service.
     *
     * @param collabNetUrl for the CollabNet server
     * @return returns the validation result of the URL.
     */
    public static FormValidation checkSoapUrl(String collabNetUrl) {
        String soapURL = collabNetUrl + "/ce-soap60/services/CollabNet?wsdl";
        return checkUrl(soapURL);
    }

    /**
     * Class for checking that a login to CollabNet is valid.  Expects
     * a StaplerRequest with url, username, and password set.
     */
    public static FormValidation loginCheck(CollabNetApp app, String password) {
        if (CommonUtil.unset(password)) {
            return FormValidation.error("The password is required.");
        }
        if (app == null) {
            return FormValidation.warning("Login fails with this CollabNet " +
                    "URL/username/password combination.");
        } else {
            CNHudsonUtil.logoff(app);
        }
        return FormValidation.ok();
    }
    
    /**
     * Checks if a project name is valid, by using the given connection.
     */
    public static FormValidation projectCheck(CollabNetApp app, String project) throws IOException {
        if (CommonUtil.unset(project)) {
            return FormValidation.error("The project is required.");
        }
        if (app != null) {
            try {
                if (app.getProjectByTitle(project) == null) {
                    return FormValidation.warning(String.format(
                            "Project '%s' cannot be found, or user %s does not have permission to access it.",
                            project, app.getUsername()));
                }
            } finally {
                CNHudsonUtil.logoff(app);
            }
        }
        return FormValidation.ok();
    }

    /**
     * Class to check that the path to a document exists.  Warns about 
     * any missing folders.  Expects a StaplerRequest with url, username,
     * password, project, and path.
     */
    public static FormValidation documentPathCheck(CollabNetApp app, String project, String path) throws IOException {
        try {
            path = path.replaceAll("/+", "/");
            path = CommonUtil.stripSlashes(path);
            if (CommonUtil.unset(path)) {
                return FormValidation.error("The path is required.");
            }
            checkInterpretedString(path);

            if (app == null) {
                return FormValidation.ok();
            }
            CTFProject p = app.getProjectByTitle(project);
            if (p != null) {
                String missing =  p.verifyPath(path);
                if (missing != null) {
                    return FormValidation.warning(String.format(
                            "Folder '%s' could not be found in path '%s'.  It (and any subfolders) will be created dynamically.", missing, path));
                }
            }
        } finally {
            CNHudsonUtil.logoff(app);
        }
        return FormValidation.ok();
    }

    /**
     * Class to check that a package exists.  Expects a StaplerRequest with 
     * a url, username, password, project, and package.
     */
    public static FormValidation packageCheck(CollabNetApp cna, String project, String rpackage) throws IOException {
        try {
            if (CommonUtil.unset(rpackage)) {
                return FormValidation.error("The package is required.");
            }
            if (cna == null) {
                return FormValidation.ok();
            }
            CTFProject p = cna.getProjectByTitle(project);
            if (p != null) {
                CTFPackage pkg = p.getPackages().byTitle(rpackage);
                if (pkg == null) {
                    return FormValidation.warning("Package could not be found.");
                }
            }
            return FormValidation.ok();
        } finally {
            CNHudsonUtil.logoff(cna);
        }
    }

    /**
     * Class to check that a release exists.  Expects a StaplerRequest with 
     * a url, username, password, project, package (optional), and release.
     */
    public static FormValidation releaseCheck(CollabNetApp cna, String project, String rpackage, String release, boolean required) throws IOException {
        try {
            if (CommonUtil.unset(release)) {
                if (required) {
                    return FormValidation.error("The release is required.");
                } else {
                    return FormValidation.ok();
                }
            }

            if (cna == null) {
                return FormValidation.ok();
            }
            CTFProject p = cna.getProjectByTitle(project);
            if (p==null)    return FormValidation.ok(); // not entered yet?

            String releaseMsg = "Release could not be found. A new release will be created.";
            CTFPackage pkg = p.getPackages().byTitle(rpackage);
            if (pkg != null) {
                CTFRelease r = pkg.getReleaseByTitle(release);
                if (r == null)
                    return FormValidation.warning(releaseMsg);
            } else {
                // locate the release from all the packages
                for (CTFPackage x : p.getPackages()) {
                    if (x.getReleaseByTitle(release)!=null)
                        return FormValidation.ok();
                }
                return FormValidation.warning(releaseMsg);
            }
            return FormValidation.ok();
        } finally {
            CNHudsonUtil.logoff(cna);
        }
    }

    /**
     * Class to check that a repo exists.  Expects a StaplerRequest with 
     * a url, username, password, and project.
     */
    public static FormValidation repoCheck(StaplerRequest request) throws IOException {
        String project = request.getParameter("project");
        String repoName = request.getParameter("repo");
        CollabNetApp cna = CNHudsonUtil.getCollabNetApp(request);
        try {
            if (cna==null)  return FormValidation.ok();

            CTFProject p = cna.getProjectByTitle(project);
            if (CommonUtil.unset(repoName)) {
                return FormValidation.error("The repository name is required.");
            }
            if (p != null) {
                CTFScmRepository r = p.getScmRepositories().byTitle(repoName);
                if (r == null) {
                    return FormValidation.warning("Repository could not be " +
                                                  "found.");
                }
            }
            return FormValidation.ok();
        } finally {
            CNHudsonUtil.logoff(cna);
        }
    }

    /**
     * Class to check that a tracker exists.  Expects a StaplerRequest with 
     * a url, username, password, project, and tracker.
     */
    public static FormValidation trackerCheck(CollabNetApp cna, String project, String tracker) throws IOException {
        if (CommonUtil.unset(tracker)) {
            return FormValidation.error("The tracker is required.");
        }
        try {
            if (cna == null) {
                return FormValidation.ok();
            }

            CTFProject p = cna.getProjectByTitle(project);
            if (p!=null) {
                CTFTracker t = p.getTrackers().byTitle(tracker);
                if (t == null) {
                    return FormValidation.warning("Tracker could not be found.");
                }
            }
            return FormValidation.ok();
        } finally {
            CNHudsonUtil.logoff(cna);
        }
    }

    /**
     * Class for checking if a user can be assigned a tracker artifact.  
     * Expects a StaplerRequest with login info (url, username, password), 
     * project, and assign (which is the username).
     */
    public static FormValidation assignCheck(CollabNetApp cna, String project, String assign) throws IOException {
        if (CommonUtil.isEmpty(assign)) {
            return FormValidation.ok();
        }
        try {
            if (cna == null) {
                return FormValidation.ok();
            }
            CTFProject p = cna.getProjectByTitle(project);
            if (p == null) {
                return FormValidation.ok();
            }
            if (!p.hasMember(assign)) {
                return FormValidation.warning("This user is not a member of the " +
                        "project.");
            }
            return FormValidation.ok();
        } finally {
            CNHudsonUtil.logoff(cna);
        }
    }

    
    /**
     * Check that a comma-separated list of users exists.
     * The check only works for a logged-in site-admin.  Otherwise,
     * give a warning that we cannot check the users' validity.
     */
    public static FormValidation userListCheck(String userStr) throws IOException {
        if (userStr == null || userStr.equals("")) {
            return FormValidation.ok();
        }
        CNAuthentication auth = CNAuthentication.get();
        if (auth == null || !auth.isSuperUser()) {
            return FormValidation.warning("Cannot check if users exist unless logged " +
                    "in as a TeamForge site admin.  Be careful!");
        }
        Collection<String> invalidUsers = getInvalidUsers(auth.getCredentials(), userStr);
        if (!invalidUsers.isEmpty()) {
            return FormValidation.error("The following users do not exist: " +
                  invalidUsers);
        }
        return FormValidation.ok();
    }

    /**
     * @param cna
     * @param userStr
     * @return the collection of users from the array which do not exist.
     */
    private static Collection<String> getInvalidUsers(CollabNetApp cna, String userStr) throws IOException {
        Collection<String> invalidUsers = new ArrayList<String>();
        if (cna != null) {
            for (String user: CommonUtil.splitCommaStr(userStr)) {
                if (!cna.isUsernameValid(user)) {
                    invalidUsers.add(user);
                }
            }
        }
        return invalidUsers;
    }

    /**
     * Check that a comma-separated list of groups exists.
     * The check only works for a logged-in site-admin.  Also warns
     * the current user if s/he will be locked out once that user
     * saves the configuration.
     */
    public static FormValidation groupListCheck(String groupStr, String userStr) throws IOException {
        Collection<String> invalidGroups = getInvalidGroups(groupStr);
        if (!invalidGroups.isEmpty()) {
            return FormValidation.error("The following groups do not exist: " +
                  invalidGroups);
            // anyone who can see if groups are invalid will
            // never be locked out, so we can return here
        }

        if (userStr != null) {
            if (locksOutCurrentUser(userStr, groupStr)) {
                return FormValidation.error("The authorization settings would lock " +
                      "the current user out of this page.  " +
                      "You may want to add your username to " +
                      "the user list.");
            }
        }

        return FormValidation.ok();
    }

    /**
     * @param groupStr
     * @return the collection of groups from the array which do not exist.
     */
    private static Collection<String> getInvalidGroups(String groupStr) throws IOException {
        CNAuthentication auth = CNAuthentication.get();
        if (auth == null) {
            // cannot connect to check.
            return Collections.emptyList();
        }
        if (!auth.isSuperUser()) {
            // only super users can see all groups and do this check.
            return Collections.emptyList();
        }
        Set<String> invalidGroups = new HashSet<String>(CommonUtil.splitCommaStr(groupStr));
        invalidGroups.removeAll(auth.getCredentials().getGroups().getTitles());
        return invalidGroups;
    }

    /**
     * Return true if the given admin user/groups would mean that
     * the current user would be locked out of the system.
     *
     * @param userStr
     * @param groupStr
     * @return true if the user would not have admin access with these
     *         authorizations.
     */
    private static boolean locksOutCurrentUser(String userStr, String groupStr) {
        CNAuthentication auth = CNAuthentication.get();
        if (auth == null) {
            // cannot check
            return false;
        }
        if (auth.isSuperUser()) {
            return false;
        }
        String currentUser = CommonUtil.getUsername(auth.getPrincipal());
        for (String user: CommonUtil.splitCommaStr(userStr)) {
            if (user.equals(currentUser)) {
                return false;
            }
        }
        return !auth.isMemberOfAny(CommonUtil.splitCommaStr(groupStr));
    }


    /**
     * Class to check for validity of a regex expression.  Expects
     * a StaplerRequest with value set.  
     */
    public static FormValidation regexCheck(String regex) {
        if(!CommonUtil.unset(regex)) {
            try {
                Pattern.compile(regex);
            } catch (PatternSyntaxException ex){
                return FormValidation.error("The regular expression is not syntactically "
                      + "correct.");
            }
        }
        return FormValidation.ok();
    }

    /**
     * Perform checking of number
     * @param number the number
     * @param allowPositive true to allow positive
     * @param allowZero true to allow zero
     * @param allowNegative true to allow negative
     * @return validation
     */
    public static FormValidation numberCheck(String number, boolean allowPositive, boolean allowZero, 
                                             boolean allowNegative) {

        int integer;
        try {
            integer = Integer.parseInt(number);
        } catch (Exception e) {
            integer = 0;
        }

        if (integer < 0 && !allowNegative) {
            return FormValidation.error("Integer cannot be negative: " + integer);
        }

        if (integer == 0 && !allowZero) {
            return FormValidation.error("Integer cannot be zero: " + integer);
        }

        if (integer > 0 && !allowPositive) {
            return FormValidation.error("Integer cannot be positive: " + integer);
        }

        return FormValidation.ok();
    }
}
