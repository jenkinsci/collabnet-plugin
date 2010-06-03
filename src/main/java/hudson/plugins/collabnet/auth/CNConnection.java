package hudson.plugins.collabnet.auth;

import com.collabnet.ce.webservices.CTFList;
import com.collabnet.ce.webservices.CTFProject;
import com.collabnet.ce.webservices.CTFRole;
import com.collabnet.ce.webservices.CTFUser;
import com.collabnet.ce.webservices.CollabNetApp;
import hudson.model.Hudson;
import hudson.plugins.collabnet.util.CNHudsonUtil;
import hudson.plugins.collabnet.util.ComboBoxUpdater;
import hudson.util.VersionNumber;
import org.acegisecurity.Authentication;
import org.acegisecurity.GrantedAuthority;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * CNConnection encapsulates the CN webservice methods.
 */

public class CNConnection {
    private CNAuthentication auth;

    private static Logger log = Logger.getLogger("CNConnection");

    private CNConnection(CNAuthentication a) {
        this.auth = a;
    }

    /**
     * Gets an instance with the current authentication, or null if the auth
     * is the wrong type.
     */
    public static CNConnection getInstance() {
        return CNConnection.getInstance(Hudson.getAuthentication());
    }

    /**
     * Wraps the private constructor.  Will return null if the Authentication
     * is the wrong type (i.e. not CNAuthentication).
     */
    public static CNConnection getInstance(Authentication a) {
        if (a instanceof CNAuthentication) {
            return new CNConnection((CNAuthentication) a);
        } else {
            return null;
        }
    }

    /**
     * @return the CollabNetApp.
     */
    public CollabNetApp getCollabNetApp() {
        Object creds = this.getAuth().getCredentials();
        if (creds instanceof CollabNetApp) {
            return (CollabNetApp) creds;
        } else {
            throw new IllegalStateException ("Credentials are incorrect " +
                                             "type for CollabNetAutorization: "
                                             + creds.getClass().getName());
        }
    }

    /**
     * @param url for CollabNet server
     * @return the CollabNet version for the given url.
     */
    public static VersionNumber getVersion(String url) {
        String version;
        try {
            version = CollabNetApp.getApiVersion(url);
        } catch (RemoteException re) {
            log.info("getVersion: failed with RemoteException: " + 
                     re.getMessage());
            return null;
        }
        try {
            return new VersionNumber(version);
        } catch (IllegalArgumentException iae) {
            log.severe("getVersion: unexpected error when attempting to " +
                       "parse CollabNet version: " + iae.getMessage());
            return null;
        }
    }

    /**
     * @return the username.
     */
    public String getUsername() {
        Object principal = this.getAuth().getPrincipal();
        if (principal instanceof String) {
            return (String) principal;
        } else {
            throw new IllegalStateException("Pricipal is incorrect type " +
                                            "for CollabNetAutorization: " + 
                                            principal.getClass().getName());
        }
    }

    /**
     * @return the Authentication.
     */
    public CNAuthentication getAuth() {
        return this.auth;
    }

    /**
     * Determines if the authenticated user belongs to any of the groups.
     * This is currently from data that's calculated once (on login).
     * If this ever turns out to be insufficient, we could change this
     * method to get the data on the fly.
     *
     * @param groups
     * @return true if the user is a member of any of the groups.
     */
    public boolean isMemberOfAny(Collection<String> groups) {
        for (String group: groups) {
            if (this.getAuth().isMember(group)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the authenticated user is a super user.
     * This is currently from data that's calculated once (on login).
     * If this ever turns out to be insufficient, we could change this
     * method to get the data on the fly.
     */
    public boolean isSuperUser() {
        GrantedAuthority[] authorities = this.getAuth().getAuthorities();
        for (GrantedAuthority authority: authorities) {
            if (authority.getAuthority().equals(CNAuthentication.SUPER_USER)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the authenticated user is a project admin.
     *
     * @param projectId
     * @return true if the user is a project admin.
     */
    public boolean isProjectAdmin(String projectId) {
        boolean admin = false;
        try {
            admin = this.getCollabNetApp().
                isUserProjectAdmin(this.getUsername(), projectId);
        } catch (RemoteException re) {
            log.info("isProjectAdmin: failed with RemoteException: " + 
                          re.getMessage());
        }
        return admin;
    }

    /**
     * Get the project id
     * @param project name
     * @return projectId or null, if none found.
     */
    public String getProjectId(String project) {
        return CNHudsonUtil.getProjectId(this.getCollabNetApp(), project);
    }

    /**
     * Get the project name
     * @param projectId id
     * @return project name or null, if none found.
     */
    public String getProjectName(String projectId) {
        return CNHudsonUtil.getProjectName(this.getCollabNetApp(), projectId);
    }

    /**
     * @return all project names, sanitized for JS.
     */
    public Collection<String> getProjects() {
        return ComboBoxUpdater.getProjectList(this.getCollabNetApp());
    }

    /**
     * @param projectId
     * @return a Collection of all users that are members of the project.
     */
    public Collection<String> getUsers(String projectId) {
        return ComboBoxUpdater.getUserList(this.getCollabNetApp(), projectId);
    }

    /**
     * @param username
     * @return true if user exists.
     */
    public boolean isUsernameValid(String username) {
        return CNHudsonUtil.isUserValid(this.getCollabNetApp(), username);
    }

    /**
     * @param group
     * @return true if group exists.
     */
    public boolean isGroupnameValid(String group) {
        boolean valid = false;
        try {
            valid = this.getCollabNetApp().getGroups().containsKey(group);
        } catch (RemoteException re) {
            log.severe("isGroupnameValid: failed with RemoteException " + 
                       re.getMessage());
        }
        return valid;
    }

    /**
     * @param projectId
     * @return a Collection of all admins in the project.
     */
    public Collection<String> getAdmins(String projectId) {
        Collection<String> admins = Collections.emptyList();
        try {
            admins = this.getCollabNetApp().getAdmins(projectId);
        } catch (RemoteException re) {
            log.severe("getAdmins: failed with RemoteException " + 
                       re.getMessage());
        }
        return admins;
    }

    /**
     * @param projectId
     * @param roleNames
     * @param descriptions of the roles.
     */
    public boolean addRoles(String projectId, List<String> roleNames, 
                            List<String> descriptions) throws RemoteException {
        boolean added = false;
        CTFProject p = this.getCollabNetApp().getProjectById(projectId);
        CTFList<CTFRole> roles = p.getRoles();
        for (int i = 0; i < roleNames.size(); i++) {
            String name = roleNames.get(i);
            String description = descriptions.get(i);
            if (roles.byTitle(name)==null) {
                added = true;
                p.createRole(name,description);
            }
        }
        return added;
    }

    /**
     * Grant the specified roles to the users in the given project.
     *
     * @param roles
     * @param usernames
     */
    public void grantRoles(CTFProject p, Collection<String> roles,
                           Collection<String> usernames) throws RemoteException {
        if (usernames.size()==0)
            return;

        CTFList<CTFRole> existingRoles = p.getRoles();

        for (String roleName : roles) {
            CTFRole role = existingRoles.byTitle(roleName);
            // figure out which users already belongs to the role
            CTFList<CTFUser> roleMembers = role.getMembers();
            Set<String> alreadyMemberSet = new HashSet<String>(roleMembers.getTitles());

            // now iterate through username collection and grant role only to users without role
            for (String username: usernames) {
                if (!alreadyMemberSet.contains(username)) {
                    try {
                        role.grant(username);
                    } catch (RemoteException re) {
                        log.severe("grantRoles: failed with RemoteException: " +
                            re.getMessage());
                    }
                }
            }
        }
    }
}
