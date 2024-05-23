package hudson.plugins.collabnet.auth;

import com.collabnet.ce.webservices.CTFList;
import com.collabnet.ce.webservices.CTFProject;
import com.collabnet.ce.webservices.CTFRole;
import com.collabnet.ce.webservices.CollabNetApp;
import hudson.model.Hudson;
import hudson.security.AuthorizationStrategy;
import hudson.security.Permission;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Authorization cache.
 */
public class CNAuthorizationCache {

    private Map<String, Set<Permission>> mPermSetMap = new HashMap<String, Set<Permission>>();
    private Map<String, CTFProject> mProjects = new HashMap<>();
    private Map<String, CTFList<CTFRole>> mRoles = new HashMap<>();
    private long mCacheExpirationDate;

    /**
     * Constructor.
     */
    public CNAuthorizationCache() {
        mCacheExpirationDate = System.currentTimeMillis(); // first time we use the cache, we'd reset expiration
    }

    /**
     * Remove all cache entries and sets next expiration date
     */
    private void clearCache() {
        mPermSetMap.clear();
        mProjects.clear();
        mRoles.clear();

        AuthorizationStrategy authStrategy = Hudson.getInstance().getAuthorizationStrategy();
        CNAuthorizationStrategy cnAuthStrategy = (CNAuthorizationStrategy) authStrategy;
        long permCacheTimeoutMs = cnAuthStrategy.getAuthCacheTimeoutMs();

        mCacheExpirationDate = System.currentTimeMillis() + permCacheTimeoutMs;
    }

    /**
     * Get a user's permission available for a given project.
     * @return set containing all of the user's permissions
     */
    public synchronized Set<Permission> getUserProjectPermSet(String username, String projectId) {
        if (System.currentTimeMillis() >= mCacheExpirationDate) {
            clearCache();
        }
        String cacheKey = projectId + ":" + username;
        Set<Permission> userPermSet = mPermSetMap.get(cacheKey);
        if (userPermSet == null || userPermSet.size() == 0) {
            userPermSet = new HashSet<Permission>();
            try {
                CollabNetApp conn = CNConnection.getInstance();
                CTFProject ctfProject = mProjects.get(projectId);
                if (ctfProject == null) {
                    List<CTFProject> projects = conn.getProjects();
                    for (CTFProject p : projects) {
                        mProjects.put(p.getId(), p);
                        if (p.getId().equals(projectId)) {
                            userPermSet = getProjectRoles(p, cacheKey, projectId, username);
                        }
                    }
                } else {
                    userPermSet = getProjectRoles(ctfProject, cacheKey, projectId, username);
                }

            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to retrieve permissions for the user "+username+" on "+projectId);
                // fall back to zero permission
            }
            mPermSetMap.put(cacheKey, userPermSet);
        }
        return userPermSet;
    }

    private static final Logger LOGGER = Logger.getLogger(CNAuthorizationCache.class.getName());

    private Set<Permission> getProjectRoles(CTFProject ctfProject, String cacheKey, String projectId, String username) {
        Set<Permission> userPermSet = new HashSet<Permission>();
        try {
            if (ctfProject != null) {
                if (ctfProject.getId().equals(projectId)) {
                    CTFList<CTFRole> roleNameSet = mRoles.get(cacheKey);
                    if (roleNameSet == null) {
                        roleNameSet = ctfProject.getUserRoles(username);
                        mRoles.put(cacheKey, roleNameSet);
                    }
                    Collection<CollabNetRole> userRoles = CNProjectACL.CollabNetRoles.getMatchingRoles(roleNameSet);
                    for (CollabNetRole role : userRoles) {
                        userPermSet.addAll(role.getPermissions());
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to retrieve permissions for the user using getProjectRoles: + "+username+" on "+projectId);
            // fall back to zero permission
        }
    return userPermSet;
    }
}
