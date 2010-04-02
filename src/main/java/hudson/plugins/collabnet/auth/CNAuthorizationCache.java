package hudson.plugins.collabnet.auth;

import hudson.model.Hudson;
import hudson.security.AuthorizationStrategy;
import hudson.security.Permission;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Authorization cache.
 */
public class CNAuthorizationCache {

    private Map<String, Set<Permission>> mPermSetMap = new HashMap<String, Set<Permission>>();
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

        AuthorizationStrategy authStrategy = Hudson.getInstance().getAuthorizationStrategy();
        CNAuthorizationStrategy cnAuthStrategy = (CNAuthorizationStrategy) authStrategy;
        long permCacheTimeoutMs = cnAuthStrategy.getAuthCacheTimeoutMs();

        mCacheExpirationDate = System.currentTimeMillis() + permCacheTimeoutMs;
    }

    /**
     * Get a user's permission available for a given project.
     * @param username the username
     * @param projectId the project id
     * @return set containing all of the user's permissions
     */
    public synchronized Set<Permission> getUserProjectPermSet(String username, String projectId) {
        if (System.currentTimeMillis() >= mCacheExpirationDate) {
            clearCache();
        }
        String cacheKey = projectId + ":" + username;
        Set<Permission> userPermSet = mPermSetMap.get(cacheKey);
        if (userPermSet == null) {
            CNConnection conn = CNConnection.getInstance();
            Set<String> roleNameSet = new HashSet<String>(conn.getUserRoles(projectId, username));
            Collection<CollabNetRole> userRoles = CNProjectACL.CollabNetRoles.getMatchingRoles(roleNameSet);
            userPermSet = new HashSet<Permission>();
            for (CollabNetRole role : userRoles) {
                userPermSet.addAll(role.getPermissions());
            }
            mPermSetMap.put(cacheKey, userPermSet);
        }
        return userPermSet;
    }
}
