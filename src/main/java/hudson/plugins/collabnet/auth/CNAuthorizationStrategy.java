package hudson.plugins.collabnet.auth;

import com.collabnet.ce.webservices.CollabNetApp;
import hudson.Extension;
import hudson.model.AbstractItem;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.User;
import hudson.model.View;
import hudson.plugins.collabnet.util.CNFormFieldValidator;
import hudson.security.ACL;
import hudson.security.AuthorizationStrategy;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import static hudson.Util.fixNull;
import static hudson.Util.join;
import static hudson.plugins.collabnet.util.CommonUtil.splitCommaStr;
import static java.lang.Math.max;

/**
 * Class for the CollabNet Authorization.
 */
public class CNAuthorizationStrategy extends AuthorizationStrategy {
    private Collection<String> readUsers;
    private Collection<String> readGroups;
    private Collection<String> adminUsers;
    private Collection<String> adminGroups;
    private int mAuthCacheTimeoutMin;
    private volatile ACL rootACL;

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
     *                  all permissions in Jenkins.
     * @param adminGroups a list of groupnames (from CollabNet) whose members
     *                  have all permissions in Jenkins.
     * @param permCacheTimeoutMin the cache timeout in min, after which the cache entries are cleared. -1 to disable.
     */
    public CNAuthorizationStrategy(List<String> readUsers, List<String> readGroups,
                                   List<String> adminUsers, List<String> adminGroups, int permCacheTimeoutMin)
    {
        this.readUsers = new ArrayList<String>(readUsers);
        this.readGroups = new ArrayList<String>(readGroups);
        this.adminUsers = new ArrayList<String>(adminUsers);
        this.adminGroups = new ArrayList<String>(adminGroups);
        mAuthCacheTimeoutMin = max(0,permCacheTimeoutMin); // can't be negative
        this.rootACL = new CNRootACL(this.adminUsers, this.adminGroups, 
                                     this.readUsers, this.readGroups);
    }

    @DataBoundConstructor
    public CNAuthorizationStrategy(String readUsersStr, String readGroupsStr, String adminUsersStr, String adminGroupsStr, int authCacheTimeoutMin) {
        this(splitCommaStr(readUsersStr), splitCommaStr(readGroupsStr),
             splitCommaStr(adminUsersStr), splitCommaStr(adminGroupsStr), max(0,authCacheTimeoutMin));
    }

    /**
     * @return a comma-delimited string of the read-only users.
     */
    public String getReadUsersStr() {
        return join(this.readUsers, ", ");
    }

    /**
     * @return a comma-delimited string of the read-only groups.
     */
    public String getReadGroupsStr() {
        return join(this.readGroups, ", ");
    }

    /**
     * @return a comma-delimited string of the admin users.
     */
    public String getAdminUsersStr() {
        return join(this.adminUsers, ", ");
    }

    /**
     * @return a comma-delimited string of the admin groups.
     */
    public String getAdminGroupsStr() {
        return join(this.adminGroups, ", ");
    }

    /**
     * Get the number of min the cache is to be kept.
     * @return number of min
     */
    public int getAuthCacheTimeoutMin() {
        return mAuthCacheTimeoutMin;
    }

    /**
     * Get the number of ms the cache is to be kept.
     * @return number of ms
     */
    public long getAuthCacheTimeoutMs() {
        return getAuthCacheTimeoutMin() * (60L * 1000);
    }

    /**
     * @return the names of all groups/roles used in this authorization
     *         strategy.
     */
    @Override
    public Collection<String> getGroups() {
        return CNProjectACL.CollabNetRoles.getNames();
    }

    /**
     * @return the default ACL.
     */
    @Override
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
    @Override
    public ACL getACL(Job <?, ?> job) {
        CNAuthProjectProperty capp = job.getProperty(CNAuthProjectProperty.class);
        if (capp != null) {
            String projectId = capp.getProjectId();
            if (projectId != null && !projectId.equals("")) {
                return new CNRootACL(this.adminUsers, this.adminGroups, 
                                     this.readUsers, this.readGroups, 
                                     new CNProjectACL(projectId));
            }
        }

        // for jobs that are not associated with any project, we'll make it configuratble by any authenticated user
        return new CNRootACL(this.adminUsers, this.adminGroups,
            this.readUsers, this.readGroups, new CNAuthenticatedUserACL());
    }

    @Override
    public ACL getACL(AbstractItem item) {
        return this.getRootACL();
    }

    @Override
    public ACL getACL(View view) {
        return this.getRootACL();
    }

    @Override
    public ACL getACL(Computer computer) {
        return this.getRootACL();
    }

    @Override
    public ACL getACL(User user) {
        return this.getRootACL();
    }

    /**
     * The CNAuthorizationStrategy Descriptor class.
     */
    @Extension
    public static final class DescriptorImpl 
        extends Descriptor<AuthorizationStrategy> {

        // any version later than this has the features
        // we require for authorization to work correctly
        public static String GOOD_VERSION = "5.2.0.0";

        /**
         * @return string to display for configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "Digital.ai Authorization";
        }

        /**
         * @return the currently saved configured CollabNet url
         */
        public static String getCollabNetUrl() {
            CollabNetApp conn = CNConnection.getInstance();
            if (conn == null) {
                return null;
            }
            return conn.getServerUrl();
        }

        /**
         * Check that the users are valid.
         */
        public FormValidation doCheckAdminUsersStr(@QueryParameter String value) throws IOException {
            return CNFormFieldValidator.userListCheck(value);
        }

        public FormValidation doCheckReadUsersStr(@QueryParameter String value) throws IOException {
            return CNFormFieldValidator.userListCheck(value);
        }

        /**
         * Check that the groups are valid.
         */
        public FormValidation doCheckAdminGroupsStr(@QueryParameter String value,
                @QueryParameter String users) throws IOException {
            return CNFormFieldValidator.groupListCheck(fixNull(value), fixNull(users));
        } 

        public FormValidation doCheckReadGroupsStr(@QueryParameter String value) throws IOException {
            return CNFormFieldValidator.groupListCheck(value,null);
        }

        /**
         * Check that the timeout number is greater than or equal to 0
         * @param value the timeout to be checked
         */
        public FormValidation doCheckAuthCacheTimeoutMin(@QueryParameter String value) {
            return CNFormFieldValidator.numberCheck(value, true, true, false);
        }
    }
}
