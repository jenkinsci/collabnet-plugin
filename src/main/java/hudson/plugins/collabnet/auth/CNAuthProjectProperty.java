package hudson.plugins.collabnet.auth;

import hudson.Extension;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.plugins.collabnet.util.CommonUtil;
import hudson.security.Permission;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Job property to associate a Hudson job with a CollabNet Project for
 * Authorization purposes (used with CollabNet Authorization).
 */
public class CNAuthProjectProperty extends JobProperty<Job<?, ?>> {
    public static Permission CONFIGURE_PROPERTY = Item.CONFIGURE;
    private transient boolean mIsNotLoadedFromDisk = false;
    private transient String project = null;
    private String projectId = null;
    private boolean createRoles = false;
    private boolean grantDefaultRoles = false;
    private static Logger log = Logger.getLogger("CNAuthProjectProperty");
    private static Collection<String> defaultAdminRoles = 
        Collections.emptyList();
    private static Collection<String> defaultUserRoles = 
        Collections.emptyList();

    /**
     * Constructor
     * @param project name of project to tie the auth to
     * @param createRoles true to create special hudson roles
     * @param grantDefaultRoles true to grant default roles to project members
     */
    @DataBoundConstructor
    public CNAuthProjectProperty(String project, boolean createRoles, String storedProjectId,
                                 boolean grantDefaultRoles) {
        this.project = project;
        this.createRoles = createRoles;
        this.grantDefaultRoles = grantDefaultRoles;
        if (this.createRoles || this.grantDefaultRoles) {
            this.loadRoles();
        }
        mIsNotLoadedFromDisk = true;

        // if the user who configured the job didn't have the authority to change the project binding,
        // revert it back to what it was before.
        loadProjectIdIfNecessary();
        if (!CommonUtil.isEmpty(getProject()) && CommonUtil.isEmpty(getProjectId())) {
            // means we can't find the specified project name - prevent overriding
            setProjectId(storedProjectId);
        }
    }

    /**
     * Determine the project id.
     */
    private void loadProjectIdIfNecessary() {
        if (CommonUtil.isEmpty(projectId) && !CommonUtil.isEmpty(project)) {
            CNConnection conn = CNConnection.getInstance();
            if (conn == null) {
                return;
            }

            projectId = conn.getProjectId(project);

            if (!mIsNotLoadedFromDisk) {
                if (this.owner != null) {
                    try {
                        mIsNotLoadedFromDisk = true;
                        this.owner.save(); // should save the conf file for the job
                    } catch (IOException e) {
                        log.info("Failed to modify config file for migration of project name to project id");
                    }
                }
            }
        }
    }

    /**
     * @return the name of the CollabNet project.
     */
    public String getProject() {
        loadProjectIdIfNecessary();

        if (!CommonUtil.isEmpty(projectId)) {
            // always use the name from project id if project id exists - this allows us to address scenario where
            // while the app is running, the project name was changed on the server
            CNConnection conn = CNConnection.getInstance();
            if (conn != null) {
                return conn.getProjectName(projectId);
            }
        }

        return "";
    }

    /**
     * @return the id of the TeamForge project.
     */
    public String getProjectId() {
        loadProjectIdIfNecessary();
        return projectId;
    }

    /**
     * Set the project id and reprocure the corresponding project name
     * @param projectId project id
     */
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    /**
     * @return true if creating the roles on the CollabNet server should be
     *         attempted.
     */
    public boolean getCreateRoles() {
        return this.createRoles;
    }

    /**
     * @return true if the default roles should be added.
     */
    public boolean getGrantDefaultRoles() {
        return this.grantDefaultRoles;
    }

    /**
     * @return the default user roles.  Lazily initialized.
     */
    public Collection<String> getDefaultUserRoles() {
        if (CNAuthProjectProperty.defaultUserRoles.isEmpty()) {
            CNAuthProjectProperty.defaultUserRoles = new ArrayList<String>();
            CNAuthProjectProperty.defaultUserRoles.add("Hudson Read");
        }
        return CNAuthProjectProperty.defaultUserRoles;
    }

    /**
     * @return the default admin roles.  Lazily initialized.
     */
    public Collection<String> getDefaultAdminRoles() {
        if (CNAuthProjectProperty.defaultAdminRoles.isEmpty()) {
            CNAuthProjectProperty.defaultAdminRoles = 
                CNProjectACL.CollabNetRoles.getNames();
        }
        return CNAuthProjectProperty.defaultAdminRoles;
    }

    /**
     * Load the roles into CSFE, if they are not already present.
     * Requires the logged in user to be a project admin in the
     * CollabNet project.
     *
     */
    private void loadRoles() {
        String projectIdStr = getProjectId();
        if (!CommonUtil.isEmpty(projectIdStr)) {
            CNConnection conn = CNConnection.getInstance();
            if (conn == null) {
                log.warning("Cannot loadRoles, incorrect authentication type.");
                return;
            }
            if (this.getCreateRoles()) {
                List<String> roleNames = new ArrayList<String>();
                List<String> descriptions = new ArrayList<String>();
                for (CollabNetRole role: CNProjectACL.CollabNetRoles.getAllRoles()) {
                    roleNames.add(role.getName());
                    descriptions.add(role.getDescription());
                }
                conn.addRoles(projectIdStr, roleNames, descriptions);
            }
            
            if (this.getGrantDefaultRoles()) {
                // load up some default roles
                // this should be an option later
                conn.grantRoles(projectIdStr, this.getDefaultUserRoles(), conn.getUsers(projectIdStr));
                conn.grantRoles(projectIdStr, this.getDefaultAdminRoles(), conn.getAdmins(projectIdStr));
            }
        }   
    }

    /**
     * Descriptor class.
     */
    @Extension
    public static class DescriptorImpl extends JobPropertyDescriptor {
        /**
         * @return string to display.
         */
        @Override
        public String getDisplayName() {
            return "Associated CollabNet Project";
        }
        
        /**
         * @param jobType
         * @return true when the CNAuthorizationStrategy is in effect.
         */
        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            // only applicable when using CNAuthorizationStrategy
            return Hudson.getInstance().getAuthorizationStrategy() 
                instanceof CNAuthorizationStrategy;
        }

        /**
         * Form validation for the project field.
         */
        public FormValidation doCheckProject(@QueryParameter String value) {
            String project = value;
            if (CommonUtil.isEmpty(project)) {
                return FormValidation.warning("If left empty, all users will be able to configure and access this " +
                    "build");
            }

            CNConnection conn = CNConnection.getInstance();
            if (conn == null) {
                return FormValidation.warning("Cannot check project name, improper" +
                        " authentication type.");
            }
            String projectId = conn.getProjectId(project);
            boolean superUser = conn.isSuperUser();
            boolean hudsonAdmin = Hudson.getInstance().getACL()
                .hasPermission(Hudson.ADMINISTER);
            if (CommonUtil.isEmpty(projectId)) {
                if (superUser) {
                    return FormValidation.error("This project does not exist.");
                } else {
                    return FormValidation.error("The current user does not have access " +
                          "to this project.  This setting change will not be saved.");
                }
            }
            FormValidation ok = FormValidation.ok("Currently selected project: " + projectId + ":" + project);
            if (superUser) {
                // all other errors should not be valid for a
                // superuser, since superusers are Hudson Admins
                // (so all-powerful in the Hudson realm) and also
                // all-powerful in the CollabNet server.
                return ok;
            }
            if (!conn.isProjectAdmin(projectId)) {
                return FormValidation.warning("The current user is not a project admin in " +
                     "the project, so he/she cannot create or " +
                     "grant roles.");
            }
            if (hudsonAdmin) {
                // no more errors apply to the Hudson Admin, since
                // admins will never be locked out of this page.
                return ok;
            }
            // check that the user will have configure permissions
            // on this page
            CNProjectACL acl = new CNProjectACL(projectId);
            if (!acl.hasPermission(CNAuthProjectProperty
                                   .CONFIGURE_PROPERTY)) {
                CollabNetRole roleNeeded =
                    CNProjectACL.CollabNetRoles
                    .getGrantingRole(CNAuthProjectProperty
                                     .CONFIGURE_PROPERTY);
                return FormValidation.warning("The current user does not have the '" +
                     roleNeeded.getName() + "' role in the " +
                     "project, which is required to configure " +
                     "this Hudson job.  If this project is chosen," +
                     " the current user will not have the power " +
                     "to change the project later, unless he/she " +
                     "is given this role.");
            }

            return ok;
        }

        /**
         * Get a list of projects to choose from.
         *
         * @return an array of project names.
         */
        public ComboBoxModel doFillProjectItems() {
            CNConnection conn = CNConnection.getInstance();
            if (conn == null) {
                return new ComboBoxModel();
            }
            return new ComboBoxModel(conn.getProjects());
        }

        /**
         * @return the CollabNet server url.
         */
        public String getCollabNetUrl() {
            CNConnection conn = CNConnection.getInstance();
            return conn.getCollabNetApp().getServerUrl();
        }
    }
}
