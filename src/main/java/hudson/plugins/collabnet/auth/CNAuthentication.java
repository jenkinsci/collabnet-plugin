package hudson.plugins.collabnet.auth;

import com.collabnet.ce.webservices.CTFProject;
import com.collabnet.ce.webservices.CollabNetApp;
import hudson.security.Permission;
import org.acegisecurity.Authentication;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.GrantedAuthorityImpl;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Authentication class for CollabNet.
 */
public class CNAuthentication implements Authentication {
    public static final String SUPER_USER = "SuperUser";
    private String principal;
    private CollabNetApp cna;
    private GrantedAuthority[] authorities;
    private Collection<String> groups;
    private boolean authenticated = false;
    private boolean cnauthed = false;
    private CNAuthorizationCache mAuthCache = null;

    private static Logger log = Logger.getLogger("CNAuthentication");
    
    public CNAuthentication(Object principal, Object credentials) {
        this.principal = (String) principal;
        this.cna = (CollabNetApp) credentials;
        this.setupAuthorities();
        this.setupGroups();
        this.setAuthenticated(true);

        mAuthCache = new CNAuthorizationCache();
    }
    
    /**
     * The only granted authority is superuser-ness.
     */
    private void setupAuthorities() {
        boolean isSuper = false;
        try {
            isSuper = this.cna.isUserSuper(this.principal);
        } catch (RemoteException re) {
            log.info("setupAuthoritites: failed with RemoteException: " + 
                     re.getMessage());
        }

        if (isSuper) {
            this.authorities = new GrantedAuthority[1];
            this.authorities[0] = 
                new GrantedAuthorityImpl(CNAuthentication.SUPER_USER);
        } else {
            this.authorities = new GrantedAuthority[0];
        }
    }

    /**
     * Check which groups this user belongs to.
     */
    private void setupGroups() {
        this.groups = Collections.emptyList();
        try {
            this.groups = cna.getUserGroups(this.principal);
        } catch (RemoteException re) {
            // not much we can do
        }
    }
    
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
    
    public boolean isAuthenticated() {
        return this.authenticated;
    }
    
    /**
     * @return a copy of the granted authorities.
     */
    public GrantedAuthority[] getAuthorities() {
        GrantedAuthority[] authCopy = 
            new GrantedAuthority[this.authorities.length];
        System.arraycopy(this.authorities, 0, authCopy, 0, 
                         this.authorities.length);
        return authCopy;
    }
    
    public Object getPrincipal() {
        return this.principal;
    }
    
    public String getName() {
        return (String)this.getPrincipal();
    }
    
    public Object getDetails() {
            return null;
    }
    
    public Object getCredentials() {
        return this.cna;
    }

    /**
     * @param group name of a CN group.
     * @return true if the user is a member of the given group.
     */
    public boolean isMember(String group) {
        return this.groups.contains(group);
    }

    public String toString() {
        return "CNAuthentication [for: " + this.getPrincipal() + 
            ", authenticated=" + this.isAuthenticated() + "]";
    }

    /**
     * Determine whether we have authenticated to CTF in the browser.
     * @return true if authenticated
     */
    public boolean isCNAuthed() {
        return this.cnauthed;
    }

    /**
     * Set whether we have authenticated to CTF in the browser.
     * @param cnauthed true if current session is CTF authenticated in browser
     */
    public void setCNAuthed(boolean cnauthed) {
        this.cnauthed = cnauthed;
    }

    public String getSessionId() {
        String sessionId = null;
        if (this.getCredentials() instanceof CollabNetApp) {
            sessionId = ((CollabNetApp)this.getCredentials()).getSessionId();
        }
        return sessionId;
    }

    /**
     * Get a set of all permission that a user has for a given project.
     * @param username user name
     * @param projectId project id
     * @return set of permissions
     */
    public Set<Permission> getUserProjectPermSet(String username, String projectId) {
        return mAuthCache.getUserProjectPermSet(username, projectId);
    }
}
