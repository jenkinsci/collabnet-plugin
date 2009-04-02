package hudson.plugins.collabnet.auth;

import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.plugins.promoted_builds.Promotion;
import hudson.security.ACL;
import hudson.security.Permission;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.acegisecurity.Authentication;

/**
 * An ACL that uses project roles to determine what Hudson permissions to give.
 */
public class CNProjectACL extends ACL {
    private String project;
    private String projectId = null;

    private static Logger log = Logger.getLogger("CNProjectACL");

    /**
     * Constructed with the CollabNet project name.
     */
    public CNProjectACL(String project) {
        this.project = project;
    }

    public String getProjectId(CNConnection conn) {
        if (this.projectId == null) {
            this.projectId = conn.getProjectId(this.project);
        }
        return this.projectId;
    }

    public boolean hasPermission(Authentication a, Permission permission) {
        CNConnection conn = CNConnection.getInstance(a);
        if (conn == null) {
            log.severe("Improper Authentication type used with " +
                       "CNAuthorizationStrategy!  CNAuthorization " +
                       "strategy cannot be used without " +
                       "CNAuthentication.  Please re-configure your " +
                       "Hudson instance.");
            return false;
        }
        String projId = this.getProjectId(conn);
        if (projId == null) {
            log.severe("hasPerission: project id could not be found for " +
                      "project: " + this.project + ".");
            return false;
        }
        Collection<CollabNetRole> userRoles = 
            CollabNetRoles.getRoles(conn.getUserRoles(projId, 
                                                      conn.getUsername()));
        log.info("hasPermission: user " + conn.getUsername() + " has roles: "
                   + userRoles.toString());
        for(; permission!=null; permission=permission.impliedBy) {
            for (CollabNetRole role: userRoles) {
                if (role.hasPermission(permission)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static class CollabNetRoles {
        private static Collection<CollabNetRole> roles = 
            Collections.emptyList();

        /**
         * Get the role which matches this name, if one exists.
         * This is a bit inefficient, but should be ok for small numbers
         * of roles.  If we ever have performance problems, we might
         * want to consider putting this in a name->role HashMap.
         *
         * @param name to match
         * @return the matching CollabNetRole or null if none is found.
         */
        public static CollabNetRole getRole(String name) {
            for (CollabNetRole role: CollabNetRoles.getAllRoles()) {
                if (role.getName().equals(name)) {
                    return role;
                }
            }
            return null;
        }

        /**
         * Get the roles matching a set of role names.
         *
         * @param names to match
         * @return a collection of matching roles.  If a name doesn't match
         *         any CollabNetRole, it will be skipped.
         */
        public static Collection<CollabNetRole> getRoles(Collection<String> 
                                                         names) {
            Collection<CollabNetRole> matchRoles = 
                new ArrayList<CollabNetRole>();
            for (String name: names) {
                CollabNetRole role = CollabNetRoles.getRole(name);
                if (role != null) {
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
                Collection<Permission> tempPermission = 
                    new ArrayList<Permission>();
                tempPermission.add(Hudson.READ);
                roles.add(new CollabNetRole("Hudson Read", "Allows users " +
                                            "read-access to Hudson jobs.", 
                                            tempPermission));
                tempPermission.clear();
                tempPermission.add(AbstractProject.BUILD);
                tempPermission.add(AbstractProject.ABORT);
                tempPermission.add(AbstractProject.WORKSPACE);
                roles.add(new CollabNetRole("Hudson Build/Cancel", "Allow " +
                                            "users to start a new build, or " +
                                            "to cancel a build.", 
                                            tempPermission));
                tempPermission.clear();
                tempPermission.add(Item.CONFIGURE);
                roles.add(new CollabNetRole("Hudson Configure", "Allow users" +
                                            " to configure a build.", 
                                            tempPermission));
                tempPermission.clear();
                tempPermission.add(Item.DELETE);
                roles.add(new CollabNetRole("Hudson Delete", "Allow users to " 
                                            + "delete builds.", 
                                            tempPermission));
                tempPermission.clear();
                // add build promotion as a permission, if the build promotion
                // plugin is present.
                if (Hudson.getInstance().
                    getPlugin("promoted-builds") != null) {
                    // check if we have the PROMOTE permission
                    Field promote = null;
                    Field[] promotionFields = Promotion.class.getFields();
                    for (Field field: promotionFields) {
                        if (field.getName().equals("PROMOTE")) {
                            promote = field;
                            break;
                        }
                    }
                    if (promote != null) {
                        Permission promotePermission = null;
                        try {
                            promotePermission = (Permission) promote.get(null);
                        } catch (IllegalAccessException iae) {}
                        if (promotePermission != null) {
                            // if we have the permission, add it
                            tempPermission.add(promotePermission);
                        }
                    }
                } 
                // We'll add the role whether or not there's a
                // permission to associated with it.
                roles.add(new CollabNetRole("Hudson Promote", 
                                            "Allow users to " +
                                            "promote builds.", 
                                            tempPermission));
                tempPermission.clear();
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
         * @return an ordered List of Role descriptions.
         */
        public static List<String> getDescriptions() {
            List<String> descriptions = new ArrayList<String>();
            for (CollabNetRole role: CollabNetRoles.getAllRoles()) {
                descriptions.add(role.getDescription());
            }
            return descriptions;
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
