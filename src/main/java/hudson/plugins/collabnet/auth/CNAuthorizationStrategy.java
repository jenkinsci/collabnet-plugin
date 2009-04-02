package hudson.plugins.collabnet.auth;

import hudson.model.AbstractProject;
import hudson.model.AbstractItem;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.User;
import hudson.model.View;
import hudson.security.ACL;
import hudson.security.AuthorizationStrategy;
import hudson.security.Permission;
import hudson.util.FormFieldValidator;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.acegisecurity.Authentication;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Class for the CollabNet Authorization.
 */
public class CNAuthorizationStrategy extends AuthorizationStrategy {
    private Collection<String> readUsers;
    private Collection<String> readGroups;
    private Collection<String> adminUsers;
    private Collection<String> adminGroups;
    private ACL rootACL;

    private static Logger log = Logger.getLogger("CNAuthorizationStrategy");

    /**
     * Constructs a new CNAUthorizationStrategy object.  This 
     * AuthorizationStrategy depends upon the CNAuthentication SecurityRealm.
     *
     * @param readUsers a list of usernames (from CollabNet) that has 
     *                  system-wide read.
     * @param readGroups a list of groupnames (from CollabNet) whose members
     *                  have system-wide read.
     * @param adminUsers a list of usernames (from CollabNet) that have
     *                  all permissions in Hudson.
     * @param adminGroups a list of groupnames (from CollabNet) whose members
     *                  have all permissions in Hudson.
     */
    public CNAuthorizationStrategy(String[] readUsers, String [] readGroups, 
                                   String[] adminUsers, String [] adminGroups) 
    {
        this.readUsers = Arrays.asList(readUsers);
        this.readGroups = Arrays.asList(readGroups);
        this.adminUsers = Arrays.asList(adminUsers);
        this.adminGroups = Arrays.asList(adminGroups);
        this.rootACL = new CNRootACL(this.adminUsers, this.adminGroups, 
                                     this.readUsers, this.readGroups);
    }

    /**
     * @return a comma-delimited string of the read-only users.
     */
    public String getReadUsersStr() {
        if (this.readUsers.isEmpty()) {
            return "";
        }
        return CNAuthorizationStrategy.join(this.readUsers, ", ");
    }

    /**
     * @return a comma-delimited string of the read-only groups.
     */
    public String getReadGroupsStr() {
        if (this.readGroups.isEmpty()) {
            return "";
        }
        return CNAuthorizationStrategy.join(this.readGroups, ", ");
    }

    /**
     * @return a comma-delimited string of the admin users.
     */
    public String getAdminUsersStr() {
        if (this.adminUsers.isEmpty()) {
            return "";
        }
        return CNAuthorizationStrategy.join(this.adminUsers, ", ");
    }

    /**
     * @return a comma-delimited string of the admin groups.
     */
    public String getAdminGroupsStr() {
        if (this.adminGroups.isEmpty()) {
            return "";
        }
        return CNAuthorizationStrategy.join(this.adminGroups, ", ");
    }

    /**
     * Utility method to join a Collection of Strings together with a 
     * delimiter.
     * 
     * @param strs a Collection of strings to join.
     * @param delimiter a separator that should be between each string.
     * @return a single string of the collection strings separated by the
     *         delimiter value.
     */
    public static String join(Collection<String> strs, String delimiter) {
        StringBuffer buf = new StringBuffer();
        for (Iterator<String> it = strs.iterator(); it.hasNext();) {
            String next = (String)it.next();
            buf.append(next);
            if (it.hasNext()) {
                buf.append(delimiter);
            } 
        }
        return buf.toString();        
    }

    /**
     * @return the names of all groups/roles used in this authorization
     *         strategy.
     */
    public Collection<String> getGroups() {
        return CNProjectACL.CollabNetRoles.getNames();
    }

    /**
     * @return the default ACL.
     */
    public ACL getRootACL() {
        log.info("Getting Root ACL");
        if (this.rootACL == null) {
            this.rootACL = new CNRootACL(this.adminUsers, this.adminGroups, 
                                         this.readUsers, this.readGroups);
        }
        return this.rootACL;
    }

    /**
     * @return the ACL specific to the CSFE project, if available.
     *         Otherwise, return the root ACL.
     */
    public ACL getACL(Job <?, ?> job) {
        log.info("Getting Project ACL for: name: " + job.getName() + 
                 ", type: " +  job.getClass().getName());
        CNAuthProjectProperty capp = (CNAuthProjectProperty)job.
            getProperty(CNAuthProjectProperty.class);
        if (capp != null) {
            String projectName = capp.getProject();
            if (projectName != null && !projectName.equals("")) {
                return new CNRootACL(this.adminUsers, this.adminGroups, 
                                     this.readUsers, this.readGroups, 
                                     new CNProjectACL(projectName));
            } else {
                return this.getRootACL();
            }
        } else {
            return this.getRootACL();
        }
    }

    public ACL getACL(AbstractItem item) {
        log.info("Getting AbstractItem ACL");
        return this.getRootACL();
    }

    public ACL getACL(AbstractProject<?, ?> project) {
        log.info("Getting AbstractProject ACL");
        return this.getACL((Job)project);
    }

    public ACL getACL(View view) {
        log.info("Getting View ACL");
        return this.getRootACL();
    }

    public ACL getACL(Computer computer) {
        log.info("Getting Computer ACL");
        return this.getRootACL();
    }

    public ACL getACL(User user) {
        log.info("Getting User ACL");
        return this.getRootACL();
    }

    /**
     * @return the descriptor for CNAuthorizationStrategy
     */
    public Descriptor<AuthorizationStrategy> getDescriptor() {
        return DESCRIPTOR;
    }

    /**
     * Descriptor should be singleton.
     */
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    
    /**
     * The CNAuthorizationStrategy Descriptor class.
     */
    public static final class DescriptorImpl 
        extends Descriptor<AuthorizationStrategy> {

        // any version later than this has the features
        // we require for authorization to work correctly
        public static String GOOD_VERSION = "5.2.0.0";

        DescriptorImpl() {
            super(CNAuthorizationStrategy.class);
        }

        /**
         * @return string to display for configuration screen.
         */
        public String getDisplayName() {
            return "CollabNet Authorization";
        }

        /**
         * @return the path to the help files.
         */
        public static String getHelpUrl() {
            return "/plugin/collabnet/auth/";
        }

        /**
         * @return the path to the help file.
         */
        @Override
        public String getHelpFile() {
            return getHelpUrl() + "help-authStrategy.html";
        }

        /**
         * @param req config page parameters.
         * @return new CNAuthorizationStrategy object, instantiated from the 
         *         configuration form vars.
         * @throws FormException
         */
        @Override
        public CNAuthorizationStrategy newInstance(StaplerRequest req, 
                                                  JSONObject formData) 
            throws FormException {
            String[] readUsers = this.splitCommaStr((String)formData
                                                    .get("readUsers"));
            String[] readGroups = this.splitCommaStr((String)formData
                                                     .get("readGroups"));
            String[] adminUsers = this.splitCommaStr((String)formData
                                                     .get("adminUsers"));
            String[] adminGroups = this.splitCommaStr((String)formData
                                                      .get("adminGroups"));
            return new CNAuthorizationStrategy(readUsers, readGroups, 
                                               adminUsers, adminGroups);
        }
        
        /**
         * Given a comma-delimited string, split it into an array of
         * strings, removing unneccessary whitespace.  Also will remove
         * empty values (i.e. only whitespace).
         * 
         * @param commaStr 
         * @return an array of the strings, with leading and trailing 
         *         whitespace removed.
         */
        private String[] splitCommaStr(String commaStr) {
            Collection<String> results = 
                new ArrayList<String>(Arrays.asList(commaStr.trim()
                                                    .split("\\s*,\\s*")));
            for (Iterator<String> it = results.iterator(); it.hasNext();) {
                String next = (String)it.next();
                next = next.trim();
                if (next.equals("")) {
                    it.remove();
                }
            }
            return results.toArray(new String[0]);
        }

        /**
         * @return the currently saved configured CollabNet url
         */
        public static String getCollabNetUrl() {
            CNConnection conn = CNConnection.getInstance();
            if (conn == null) {
                return null;
            }
            return conn.getCollabNetApp().getServerUrl();
        }

        /**
         * @param url for the CollabNet server.
         * @return the CollabNet version number.
         */
        public static CNVersion getVersion(String url) {
            if (url == null) {
                return null;
            }
            return CNConnection.getVersion(url);
        }
        
        /**
         * @return true if the CollabNet version is late enough (5.2+)
         *         that using this AuthorizationStrategy is effective.
         */
        public static boolean isGoodCNVersion(String url) {
            CNVersion version = getVersion(url);
            if (version == null) {
                // we can't check, so we'll assume it's ok.
                return true;
            }
            CNVersion desiredVersion = new CNVersion(GOOD_VERSION);
            if (version.compareTo(desiredVersion) >= 0) {
                log.info("current version (" + version + ") is greater " +
                         "than or equal to desired version " + 
                         desiredVersion); 
                return true;
            } else {
                return false;
            }
        }

        /**
         * Check whether the "incorrect version" msg should be displayed, 
         * and returns what the currently configured version is, in a json.
         */
        public void doVersionCheck(StaplerRequest req, StaplerResponse rsp) 
            throws IOException {
            String url = req.getParameter("url");
            rsp.setContentType("text/plain;charset=UTF-8");
            JSONObject versionJSON = new JSONObject();
            String error_display_style = "none";
            if (!isGoodCNVersion(url)) {
                error_display_style = "inline";
            }
            versionJSON.element("error_display_style", error_display_style);
            CNVersion version = getVersion(url);
            if (version != null) {
                versionJSON.element("version", getVersion(url).toString());
            } else {
                versionJSON.element("version", "unknown");
            }
            rsp.getWriter().print(versionJSON.toString());
        }
        

        /**
         * Check that the users are valid.
         */
        public void doUserCheck(StaplerRequest req, 
                                StaplerResponse rsp) 
            throws IOException, ServletException {
            new FormFieldValidator(req,rsp,true) {
                protected void check() throws IOException, ServletException {  
                    String userStr = request.getParameter("value");
                    Collection<String> invalidUsers = 
                        getInvalidUsers(userStr);
                    if (!invalidUsers.isEmpty()) {
                        error("The following users do not exist: " + 
                              invalidUsers);
                        return;
                    }
                    ok();           
                }
            }.process();
        }

        /**
         * @param userStr
         * @return the collection of users from the array which do not exist.
         */
        private Collection<String> getInvalidUsers(String userStr) {
            String[] users = splitCommaStr(userStr);
            CNConnection conn = CNConnection.getInstance();
            if (conn == null) {
                return Collections.emptyList();
            }
            Collection<String> invalidUsers = new ArrayList<String>();
            for (String user: users) {
                if (!conn.isUsernameValid(user)) {
                    invalidUsers.add(user);
                }
            }
            return invalidUsers;
        }

        /**
         * Check that the groups are valid.
         */
        public void doGroupCheck(StaplerRequest req, 
                                StaplerResponse rsp) 
            throws IOException, ServletException {
            new FormFieldValidator(req,rsp,true) {
                protected void check() throws IOException, ServletException {
                    String groupStr = request.getParameter("groups");
                    Collection<String> invalidGroups = 
                        getInvalidGroups(groupStr);
                    if (!invalidGroups.isEmpty()) {
                        error("The following groups do not exist: " + 
                              invalidGroups);
                        // anyone who can see if groups are invalid will
                        // never be locked out, so we can return here
                        return;
                    }

                    String userStr = request.getParameter("users");
                    if (userStr != null) {
                        if (locksOutCurrentUser(userStr, groupStr)) {
                            error("The authorization settings would lock " +
                                  "the current user out of this page.  " +
                                  "You may want to add your username to " +
                                  "the user list.");
                            return;
                        }
                    }

                    ok(); 
                }
            }.process();
        } 

        /**
         * @param groupStr
         * @return the collection of groups from the array which do not exist.
         */
        private Collection<String> getInvalidGroups(String groupStr) {
            CNConnection conn = CNConnection.getInstance();
            if (conn == null) {
                // cannot connect to check.
                return Collections.emptyList();
            }
            if (!conn.isSuperUser()) {
                // only super users can see all groups and do this check.
                return Collections.emptyList();
            }
            String[] groups = splitCommaStr(groupStr);
            Collection<String> invalidGroups = new ArrayList<String>();
            for (String group: groups) {
                if (!conn.isGroupnameValid(group)) {
                    invalidGroups.add(group);
                }
            }
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
        private boolean locksOutCurrentUser(String userStr, String groupStr) {
            CNConnection conn = CNConnection.getInstance();
            if (conn == null) {
                // cannot check
                return false;
            }
            if (conn.isSuperUser()) {
                return false;
            }
            String currentUser = conn.getUsername();
            String[] users = this.splitCommaStr(userStr);
            for (String user: users) {
                if (user.equals(currentUser)) {
                    return false;
                }
            }
            String[] groups = this.splitCommaStr(groupStr);
            if (conn.isMemberOfAny(Arrays.asList(groups))) {
                return false;
            }
            return true;
        }
    }   
}
