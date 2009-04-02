package hudson.plugins.collabnet.auth;

import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.security.Permission;
import hudson.util.FormFieldValidator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Job property to associate a Hudson job with a CollabNet Project for
 * Authorization purposes (used with CollabNet Authorization).
 */
public class CNAuthProjectProperty extends JobProperty<Job<?, ?>> {
    public static Permission CONFIGURE_PROPERTY = Item.CONFIGURE;
    private String project;
    private boolean createRoles = false;
    private boolean grantDefaultRoles = false;
    private static Logger log = Logger.getLogger("CNAuthProjectProperty");
    private static Collection<String> defaultAdminRoles = 
        Collections.emptyList();
    private static Collection<String> defaultUserRoles = 
        Collections.emptyList();

    public CNAuthProjectProperty(String project, Boolean createRoles, 
                                 Boolean grantDefaults) {
        this.project = project;
        this.createRoles = createRoles.booleanValue();
        this.grantDefaultRoles = grantDefaults.booleanValue();
        if (this.createRoles || this.grantDefaultRoles) {
            this.loadRoles();
        }
    }

    /**
     * @return the name of the CollabNet project.
     */
    public String getProject() {
        return this.project;
    }

    /**
     * @return true if creating the roles on the CollabNet server should be
     *         attempted.
     */
    public boolean createRoles() {
        return this.createRoles;
    }

    /**
     * @return true if the default roles should be added.
     */
    public boolean grantDefaultRoles() {
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
        if (this.getProject() != null && !this.getProject().equals("")) {
            CNConnection conn = CNConnection.getInstance();
            if (conn == null) {
                log.warning("Cannot loadRoles, incorrect authentication type.");
                return;
            }
            String projectId = conn.getProjectId(this.getProject());
            if (this.createRoles()) {
                List<String> roleNames = CNProjectACL.CollabNetRoles.
                    getNames();
                List<String> descriptions = CNProjectACL.CollabNetRoles.
                    getDescriptions();
                conn.addRoles(projectId, roleNames, descriptions);
            }
            
            if (this.grantDefaultRoles()) {
                // load up some default roles
                // this should be an option later
                conn.grantRoles(projectId, this.getDefaultUserRoles(), 
                                conn.getUsers(projectId));
                conn.grantRoles(projectId, this.getDefaultAdminRoles(), 
                                conn.getAdmins(projectId));
            }
        }   
    }

    /**
     * @return the descriptor for CNAuthProjectProperty
     */
    public JobPropertyDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    /**
     * Descriptor should be singleton.
     */
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    /**
     * Descriptor class.
     */
    public static class DescriptorImpl extends JobPropertyDescriptor {

        protected DescriptorImpl() {
            super(CNAuthProjectProperty.class);
        }

        /**
         * @param req config page parameters.
         * @return new CNAuthProjectProperty object, instantiated from the 
         *         configuration form vars.
         * @throws FormException
         */
        @Override
        public JobProperty<?> newInstance(StaplerRequest req,
                                          JSONObject formData) 
            throws FormException {
            Boolean createRoles = Boolean.FALSE;
            Boolean grantDefaults = Boolean.FALSE;
            if (formData.get("createRoles") != null) {
                createRoles = Boolean.TRUE;
            }
            if (formData.get("grantDefaults") != null) {
                grantDefaults = Boolean.TRUE;
            }
            return new CNAuthProjectProperty((String)formData.get("project"),
                                             createRoles,
                                             grantDefaults);
        }

        /**
         * @return string to display.
         */
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
         *
         * @param req StaplerRequest which contains parameters from 
         *            the config.jelly.
         * @param rsp contains http response data (unused).
         * @throws IOException
         * @throws ServletException
         */
        public void doProjectCheck(StaplerRequest req, 
                                   StaplerResponse rsp) 
            throws IOException, ServletException {
            new FormFieldValidator(req,rsp,false) {
                protected void check() throws IOException, ServletException {
                    String project = request.getParameter("project");
                    if (project == null || project.equals("")) {
                        warning("If left empty, only Hudson admins have " +
                                "more than READ permissions in the project.");
                        return;
                    }
                    CNConnection conn = CNConnection.getInstance();
                    if (conn == null) {
                        warning("Cannot check project name, improper" +
                                " authentication type.");
                        return;
                    }
                    String projectId = conn.getProjectId(project);
                    boolean superUser = conn.isSuperUser();
                    boolean hudsonAdmin = Hudson.getInstance().getACL()
                        .hasPermission(Hudson.ADMINISTER);
                    if (projectId == null) {
                        if (superUser) {
                            error("This project does not exist.");
                            return;
                        } else {
                            error("The current user does not have access " +
                                  "to this project.  If this project is " +
                                  "chosen, the current user will be locked " +
                                  "out of this Hudson job.");
                            return;
                        }
                    }
                    if (superUser) {
                        // all other errors should not be valid for a 
                        // superuser, since superusers are Hudson Admins
                        // (so all-powerful in the Hudson realm) and also
                        // all-powerful in the CollabNet server.
                        ok();
                        return;
                    }
                    if (!conn.isProjectAdmin(projectId)) {
                        warning("The current user is not a project admin in " +
                             "the project, so he/she cannot create or " +
                             "grant roles.");
                    }
                    if (hudsonAdmin) {
                        // no more errors apply to the Hudson Admin, since
                        // admins will never be locked out of this page.
                        ok();
                        return;
                    }
                    // check that the user will have configure permissions
                    // on this page
                    CNProjectACL acl = new CNProjectACL(project);
                    if (!acl.hasPermission(CNAuthProjectProperty
                                           .CONFIGURE_PROPERTY)) {
                        CollabNetRole roleNeeded = 
                            CNProjectACL.CollabNetRoles
                            .getGrantingRole(CNAuthProjectProperty
                                             .CONFIGURE_PROPERTY);
                        warning("The current user does not have the '" + 
                             roleNeeded.getName() + "' role in the " +
                             "project, which is required to configure " +
                             "this Hudson job.  If this project is chosen," +
                             " the current user will not have the power " +
                             "to change the project later, unless he/she " +
                             "is given this role.");
                    }
                    
                    ok();
                }
            }.process();
        }

        /**
         * Get a list of projects to choose from.
         *
         * @return an array of project names.
         */
        public String[] getProjects() {
            log.info("getting projects");
            Collection<String> projects = Collections.emptyList();
            CNConnection conn = CNConnection.getInstance();
            if (conn == null) {
                return new String[0];
            }
            projects = conn.getProjects();
            return projects.toArray(new String[0]);
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
