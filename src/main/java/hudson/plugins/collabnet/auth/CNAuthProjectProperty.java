package hudson.plugins.collabnet.auth;

import com.collabnet.ce.webservices.CTFList;
import com.collabnet.ce.webservices.CTFProject;
import com.collabnet.ce.webservices.CTFRole;
import com.collabnet.ce.webservices.CTFUser;
import com.collabnet.ce.webservices.CollabNetApp;
import hudson.Extension;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.plugins.collabnet.util.ComboBoxUpdater;
import hudson.plugins.collabnet.util.CommonUtil;
import hudson.security.Permission;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;

/**
 * Job property to associate a Jenkins job with a CollabNet Project for
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
     * @param createRoles true to create special Jenkins roles
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
            CollabNetApp conn = CNConnection.getInstance();
            if (conn == null) {
                return;
            }

            try {
                CTFProject p = conn.getProjectByTitle(project);
                projectId = p!=null ? p.getId() : null;
            } catch (RemoteException e) {
                projectId = null;
                log.log(WARNING,"Failed to load project ID of "+project,e);
            }

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
            CollabNetApp conn = CNConnection.getInstance();
            if (conn != null) {
                try {
                    CTFProject p = conn.getProjectById(projectId);
                    if (p!=null)
                        return p.getTitle();
                } catch (RemoteException e) {
                    // fall back to the stored project name
                }
            }
        }

        return project;
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
            try {
                CollabNetApp conn = CNConnection.getInstance();
                if (conn == null) {
                    log.warning("Cannot loadRoles, incorrect authentication type.");
                    return;
                }
                CTFProject p = conn.getProjectById(getProjectId());
                if (this.getCreateRoles()) {
                    CTFList<CTFRole> existing = p.getRoles();
                    for (CollabNetRole role: CNProjectACL.CollabNetRoles.getAllRoles()) {
                        if (existing.byTitle(role.getName())==null)
                            p.createRole(role.getName(), role.getDescription());
                    }
                }

                if (this.getGrantDefaultRoles()) {
                    // load up some default roles
                    // this should be an option later
                    grantRoles(p, this.getDefaultUserRoles(), p.getMembers());
                    grantRoles(p, this.getDefaultAdminRoles(), p.getAdmins());
                }
            } catch (RemoteException e) {
                log.log(WARNING, "Cannot loadRoles, incorrect authentication type.", e);
            }
        }   
    }

    private void grantRoles(CTFProject p, Collection<String> roleNames, List<CTFUser> members) throws RemoteException {
        CTFList<CTFRole> roles = p.getRoles();
        for (String name : roleNames) {
            CTFRole r = roles.byTitle(name);
            if (r==null)        continue; // this indicates an abstraction leakage

            CTFList<CTFUser> existing = r.getMembers();
            for (CTFUser m : members) {
                if (!existing.contains(m))
                    try {
                        r.grant(m);
                    } catch (RemoteException re) {
                        log.severe("grantRoles: failed with RemoteException: " +
                            re.getMessage());
                    }
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
        public FormValidation doCheckProject(@QueryParameter String value) throws RemoteException {
            String project = value;
            if (CommonUtil.isEmpty(project)) {
                return FormValidation.warning("If left empty, all users will be able to configure and access this " +
                    "build");
            }

            CNAuthentication auth = CNAuthentication.get();
            if (auth == null) {
                return FormValidation.warning("Cannot check project name, improper" +
                        " authentication type.");
            }
            CTFProject p = auth.getCredentials().getProjectByTitle(project);
            boolean superUser = auth.isSuperUser();
            boolean hudsonAdmin = Hudson.getInstance().getACL()
                .hasPermission(Hudson.ADMINISTER);
            if (p==null) {
                if (superUser) {
                    return FormValidation.error("This project does not exist.");
                } else {
                    return FormValidation.error("The current user does not have access " +
                          "to this project.  This setting change will not be saved.");
                }
            }
            FormValidation ok = FormValidation.ok("Currently selected project: " + p.getId() + ":" + project);
            if (superUser) {
                // all other errors should not be valid for a
                // superuser, since superusers are Jenkins Admins
                // (so all-powerful in the Jenkins realm) and also
                // all-powerful in the CollabNet server.
                return ok;
            }
            if (!auth.isProjectAdmin(p)) {
                return FormValidation.warning("The current user is not a project admin in " +
                     "the project, so he/she cannot create or " +
                     "grant roles.");
            }
            if (hudsonAdmin) {
                // no more errors apply to the Jenkins Admin, since
                // admins will never be locked out of this page.
                return ok;
            }
            // check that the user will have configure permissions
            // on this page
            CNProjectACL acl = new CNProjectACL(p.getId());
            if (!acl.hasPermission(CNAuthProjectProperty
                                   .CONFIGURE_PROPERTY)) {
                CollabNetRole roleNeeded =
                    CNProjectACL.CollabNetRoles
                    .getGrantingRole(CNAuthProjectProperty
                                     .CONFIGURE_PROPERTY);
                return FormValidation.warning("The current user does not have the '" +
                     roleNeeded.getName() + "' role in the " +
                     "project, which is required to configure " +
                     "this Jenkins job.  If this project is chosen," +
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
        public ComboBoxModel doFillProjectItems() throws RemoteException {
            CollabNetApp conn = CNConnection.getInstance();
            return ComboBoxUpdater.getProjectList(conn);
        }

        /**
         * @return the CollabNet server url.
         */
        public String getCollabNetUrl() {
            CollabNetApp conn = CNConnection.getInstance();
            return conn.getServerUrl();
        }
    }
}
