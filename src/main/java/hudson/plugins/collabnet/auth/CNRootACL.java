package hudson.plugins.collabnet.auth;

import hudson.model.Hudson;
import hudson.security.ACL;
import hudson.security.Permission;

import java.util.Collection;

import java.util.logging.Logger;

import org.acegisecurity.Authentication;

/**
 * Root ACL for the CollabNet Authorization.  It gives a set of users
 * Hudson admin privileges, another set read privileges, and if specified,
 * will wrap another ACL and extend those permissions.
 */
public class CNRootACL extends ACL {
    private Collection<String> adminUsers;
    private Collection<String> adminGroups;
    private Collection<String> readUsers;
    private Collection<String> readGroups;
    private ACL innerACL;

    private static Logger log = Logger.getLogger("CNRootACL");

    public CNRootACL(Collection<String> adminUsers, 
                     Collection<String> adminGroups, 
                     Collection<String> readUsers, 
                     Collection<String> readGroups) {
        this(adminUsers, adminGroups, readUsers, readGroups, null);
    }

    public CNRootACL(Collection<String> adminUsers, 
                     Collection<String> adminGroups, 
                     Collection<String> readUsers, 
                     Collection<String> readGroups, 
                     ACL innerACL) {
        this.adminUsers = adminUsers;
        this.adminGroups = adminGroups;
        this.readUsers = readUsers;
        this.readGroups = readGroups;
        this.innerACL = innerACL;
    }

    /**
     * If the user is included in the admins or readUsers sets, check
     * whether the permission is granted via those.  If the permission
     * is not settled, pass to any existing innerACL.
     *
     * @param a current authentication.
     * @param p permission to check
     * @return true if the user should have the permission.
     */
    @Override
    public boolean hasPermission(Authentication a, Permission p) {
        log.info("hasPermission: checking for " + a + " with permission " + p 
                 + ".");
        if (!(a instanceof CNAuthentication)) {
            // This can happen when we switch to this Auth but haven't logged
            // out yet.
            return false;
        }
        String username = (String)a.getPrincipal();
        if (!username.equals("anonymous")) {
            // allow all logged-in users to access the Hudson.READ
            if (p.equals(Hudson.READ)) {
                return true;
            }

            CNConnection conn = CNConnection.getInstance(a);
            if (conn == null) {
                // if the authentication is the wrong type, return true
                log.severe("Improper Authentication type used with " +
                           "CNAuthorizationStrategy!  CNAuthorization " +
                           "strategy cannot be used without " +
                           "CNAuthentication.  Please re-configure your " +
                           "Hudson instance.");
                return false;
            }
            if (conn.isSuperUser() || 
                this.adminUsers.contains(username) || 
                conn.isMemberOfAny(this.adminGroups)) {
                // admins have every permission
                return true;
            }
            if (this.readUsers.contains(username) ||
                conn.isMemberOfAny(this.readGroups)) {
                for(Permission permission = p; permission!=null; 
                    permission=permission.impliedBy) {
                    if (permission.equals(Permission.READ)) {
                        return true;
                    }
                }
            }
        }
        // try the inner ACL, if one exists
        if (this.innerACL != null) {
            return this.innerACL.hasPermission(a, p);
        }
        return false;
    }
    
}
