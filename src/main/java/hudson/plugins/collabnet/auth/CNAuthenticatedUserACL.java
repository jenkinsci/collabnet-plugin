package hudson.plugins.collabnet.auth;

import hudson.security.ACL;
import hudson.security.Permission;
import org.acegisecurity.Authentication;

/**
 * Gives user permission if the user is authenticated.
 */
public class CNAuthenticatedUserACL extends ACL {
    /**
     * If the user is authenticated, return true.
     *
     * @param a current authentication.
     * @param p permission to check
     *
     * @return true if the user should have the permission.
     */
    @Override
    public boolean hasPermission(Authentication a, Permission p) {
        return a.isAuthenticated();
    }
}