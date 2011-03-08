package hudson.plugins.collabnet.auth;

import com.collabnet.ce.webservices.CTFList;
import com.collabnet.ce.webservices.CTFRole;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.plugins.collabnet.util.CommonUtil;
import hudson.plugins.promoted_builds.Promotion;
import hudson.scm.SCM;
import hudson.security.ACL;
import hudson.security.Permission;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.acegisecurity.Authentication;

/**
 * An ACL that uses project roles to determine what Jenkins permissions to give.
 */
public class CNProjectACL extends ACL {
    private String projectId = null;

    private static Logger log = Logger.getLogger("CNProjectACL");

    /**
     * Constructor.
     * @param projectId the id of the TeamForge project associated with this ACL
     */
    public CNProjectACL(String projectId) {
        this.projectId = projectId;
    }

    public boolean hasPermission(Authentication a, Permission permission) {

        if (!(a instanceof CNAuthentication)) {
            log.severe("Improper Authentication type used with " +
                       "CNAuthorizationStrategy!  CNAuthorization " +
                       "strategy cannot be used without " +
                       "CNAuthentication.  Please re-configure your " +
                       "Jenkins instance.");
            return false;
        }

        if (CommonUtil.isEmpty(projectId)) {
            log.severe("hasPerission: project id could not be found for project: " + this.projectId + ".");
            return false;
        }

        CNAuthentication cnAuth = (CNAuthentication) a;
        String username = cnAuth.getPrincipal();
        Set<Permission> userPerms = cnAuth.getUserProjectPermSet(username, projectId);
        for(; permission!=null; permission=permission.impliedBy) {
            if (userPerms.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    public static class CollabNetRoles {
        private static Collection<CollabNetRole> roles = Collections.emptyList();

        public static final CollabNetRole HUDSON_READ_ROLE = new CollabNetRole("Hudson Read", "Allows users " +
                                    "read-access to Jenkins jobs.",
                                    Hudson.READ, Item.READ);
        public static final CollabNetRole HUDSON_BUILD_ROLE = new CollabNetRole("Hudson Build/Cancel", "Allow " +
                                    "users to start a new build, or " +
                                    "to cancel a build.",
                                    AbstractProject.BUILD, AbstractProject.ABORT, AbstractProject.WORKSPACE, Item.BUILD, SCM.TAG);
        public static final CollabNetRole HUDSON_CONFIGURE_ROLE = new CollabNetRole("Hudson Configure", "Allow users" +
                                    " to configure a build.",
                                    Item.CONFIGURE);
        public static final CollabNetRole HUDSON_DELETE_ROLE = new CollabNetRole("Hudson Delete", "Allow users to "
                                    + "delete builds.",
                                    Item.DELETE);

        /**
         * Get the applicable Jenkins roles matching a set of user role names
         *
         * @param userRoleSet names of roles to match
         * @return a collection of Jenkins roles with names that exist in user role set
         */
        public static Collection<CollabNetRole> getMatchingRoles(CTFList<CTFRole> userRoleSet) {
            Collection<CollabNetRole> matchRoles = new ArrayList<CollabNetRole>();
            for (CollabNetRole role : getAllRoles()) {
                if (userRoleSet.getTitles().contains(role.getName())) {
                    matchRoles.add(role);
                }
            }
            return matchRoles;
        }

        /**
         * @return all roles.  Lazily initialized.
         */
        public static Collection<CollabNetRole> getAllRoles() {
            if (CollabNetRoles.roles.isEmpty()) {
                roles = new ArrayList<CollabNetRole>();
                roles.add(HUDSON_READ_ROLE);
                roles.add(HUDSON_BUILD_ROLE);
                roles.add(HUDSON_CONFIGURE_ROLE);
                roles.add(HUDSON_DELETE_ROLE);
                // add build promotion as a permission, if the build promotion
                // plugin is present.
                if (Hudson.getInstance().getPlugin("promoted-builds") != null) {
                    // check if we have the PROMOTE permission
                    Field promote = null;
                    Field[] promotionFields = Promotion.class.getFields();
                    for (Field field: promotionFields) {
                        if (field.getName().equals("PROMOTE")) {
                            promote = field;
                            break;
                        }
                    }
                    Permission promotePermission = null;
                    if (promote != null) {
                        try {
                            promotePermission = (Permission) promote.get(null);
                        } catch (IllegalAccessException iae) {}
                    }

                    roles.add(new CollabNetRole("Hudson Promote",
                                            "Allow users to " +
                                            "promote builds.",
                                            promotePermission!=null ? new Permission[]{promotePermission} : new Permission[0] ));
                }
            }
            return CollabNetRoles.roles;
        }

        /**
         * @return an ordered List of Role names.
         */
        public static List<String> getNames() {
            List<String> names = new ArrayList<String>();
            for (CollabNetRole role: CollabNetRoles.getAllRoles()) {
                names.add(role.getName());
            }
            return names;
        }

        /**
         * Given a permission, return the CollabNet role which would grant
         * that permission (if any).  Returns the first permission granting
         * role found (but we expect only one).
         *
         * @param permission
         * @return the CollabNet role which would grant that permission, or
         *         null if none would.
         */
        public static CollabNetRole getGrantingRole(Permission permission) {
            Collection<Permission> implyingPermissions = 
                CollabNetRoles.expandPermissions(permission);
            for (CollabNetRole role: CollabNetRoles.getAllRoles()) {
                for (Permission p: implyingPermissions) {
                    if (role.hasPermission(p)) {
                        return role;
                    }
                }
            }
            return null;
        }

        /**
         * Given a permission, expand it into a collection, containing every
         * permission implied by this permission (including the starting 
         * permission).
         *
         * @param permission
         * @return the collection of implied permissions.
         */
        private static Collection<Permission> expandPermissions(Permission 
                                                                permission) {
            Collection<Permission> permissions = new ArrayList<Permission>();
            for(Permission p = permission; p != null; 
                p = permission.impliedBy) {
                permissions.add(p);
            }
            return permissions;
        }

    }

}
