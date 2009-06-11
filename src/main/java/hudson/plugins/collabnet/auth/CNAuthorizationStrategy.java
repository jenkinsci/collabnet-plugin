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

import hudson.plugins.collabnet.util.CommonUtil;
import hudson.plugins.collabnet.util.CNFormFieldValidator;

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
        return this.getRootACL();
    }

    public ACL getACL(AbstractProject<?, ?> project) {
        return this.getACL((Job)project);
    }

    public ACL getACL(View view) {
        return this.getRootACL();
    }

    public ACL getACL(Computer computer) {
        return this.getRootACL();
    }

    public ACL getACL(User user) {
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
            String[] readUsers = CommonUtil.splitCommaStr((String)formData
                                                          .get("readUsers"));
            String[] readGroups = CommonUtil.splitCommaStr((String)formData
                                                           .get("readGroups"));
            String[] adminUsers = CommonUtil.splitCommaStr((String)formData
                                                           .get("adminUsers"));
            String[] adminGroups = CommonUtil
                .splitCommaStr((String)formData.get("adminGroups"));
            return new CNAuthorizationStrategy(readUsers, readGroups, 
                                               adminUsers, adminGroups);
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
            new CNFormFieldValidator.UserListCheck(req,rsp).process();
        }

        /**
         * Check that the groups are valid.
         */
        public void doGroupCheck(StaplerRequest req, 
                                StaplerResponse rsp) 
            throws IOException, ServletException {
            new CNFormFieldValidator.GroupListCheck(req, rsp).process();
        } 
    }   
}
