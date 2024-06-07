package hudson.plugins.collabnet.auth;

import com.collabnet.ce.webservices.CTFProject;
import com.collabnet.ce.webservices.CTFUser;
import com.collabnet.ce.webservices.CollabNetApp;
import hudson.model.Hudson;
import hudson.security.Permission;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Authentication class for CollabNet.
 */
public class CNAuthentication implements Authentication {
    public static final String SUPER_USER = "SuperUser";
    private final String principal;
    private final CTFUser myself;
    private final CollabNetApp cna;
    private List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    private Collection<String> groups;
    private boolean authenticated = false;
    private boolean cnauthed = false;
    private CNAuthorizationCache mAuthCache = null;

    private static Logger log = Logger.getLogger("CNAuthentication");
    
    public CNAuthentication(Object principal, Object credentials) throws IOException {
        this.principal = (String) principal;
        this.cna = (CollabNetApp) credentials;
        this.myself = cna.getMyselfData();
        this.setupAuthorities();
        this.setupGroups();
        this.setAuthenticated(true);
        this.setCNAuthed(true);

        mAuthCache = new CNAuthorizationCache();
    }
    
    /**
     * The only granted authority is superuser-ness.
     */
    private void setupAuthorities() throws IOException {
        boolean isSuper = false;
        try {
            isSuper = myself.isSuperUser();
        } catch (IOException re) {
            log.info("setupAuthoritites: failed with RemoteException: " +
                    re.getMessage());
        }
        if (isSuper) {
            authorities.add(new SimpleGrantedAuthority(CNAuthentication.SUPER_USER));
        }
        authorities.add(new SimpleGrantedAuthority("authenticated"));
    }

    /**
     * Check which groups this user belongs to.
     */
    private void setupGroups() {
        this.groups = Collections.emptyList();
        try {
            this.groups = myself.getGroupNames();
        } catch (IOException re) {
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
    public List<SimpleGrantedAuthority> getAuthorities() {
        return authorities;
    }
    
    public String getPrincipal() {
        return this.principal;
    }
    
    public String getName() {
        return this.getPrincipal();
    }
    
    public Object getDetails() {
            return null;
    }
    
    public CollabNetApp getCredentials() {
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

    /**
     * Determines if the authenticated user is a super user.
     * This is currently from data that's calculated once (on login).
     * If this ever turns out to be insufficient, we could change this
     * method to get the data on the fly.
     */
    public boolean isSuperUser() {
        for (SimpleGrantedAuthority authority: getAuthorities()) {
            if (authority.getAuthority().equals(CNAuthentication.SUPER_USER)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the authenticated user belongs to any of the groups.
     * This is currently from data that's calculated once (on login).
     * If this ever turns out to be insufficient, we could change this
     * method to get the data on the fly.
     *
     * @param groups collection of groups
     * @return true if the user is a member of any of the groups.
     */
    public boolean isMemberOfAny(Collection<String> groups) {
        for (String group: groups) {
            if (this.isMember(group)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the authenticated user is a project admin.
     *
     * @return true if the user is a project admin.
     */
    public boolean isProjectAdmin(CTFProject p) {
        try {
            return p.getAdmins().contains(cna.getMyself());
        } catch (IOException re) {
            log.info("isProjectAdmin: failed with RemoteException: " + re.getMessage());
            return false;
        }
    }

    public String getSessionId() {
        return this.getCredentials().getSessionId();
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

    /**
     * If the current thread carries the {@link CNAuthentication} object as the context,
     * returns it. Or else null.
     */
    public static CNAuthentication get() {
        return cast(Hudson.getAuthentication2());
    }

    public static CNAuthentication cast(Authentication a) {
        if (a instanceof CNAuthentication) {
            return (CNAuthentication) a;
        } else {
            return null;
        }
    }
}
