package hudson.plugins.collabnet.util;

import com.collabnet.ce.webservices.CTFPackage;
import com.collabnet.ce.webservices.CTFProject;
import com.collabnet.ce.webservices.CTFRelease;
import com.collabnet.ce.webservices.CTFScmRepository;
import com.collabnet.ce.webservices.CollabNetApp;
import hudson.plugins.collabnet.share.TeamForgeShare;
import hudson.util.VersionNumber;
import org.kohsuke.stapler.StaplerRequest;

import java.rmi.RemoteException;
import java.util.logging.Logger;

/**
 * Class for methods that are useful across Jenkins plugins.
 */
public class CNHudsonUtil {
    private static Logger log = Logger.getLogger("CNHudsonUtil");

    /**
     * As a utility class, CNHudsonUtil should never be instantiated.
     */
    private CNHudsonUtil() {}

    /**
     * @param url
     * @param username
     * @param password
     * @return collabnet app, if one can be created; null otherwise.
     */
    public static CollabNetApp getCollabNetApp(String url, 
                                               String username, 
                                               String password) {
        if (CommonUtil.unset(url)) {
            return null;
        }
        try {
            return new CollabNetApp(url, username, password);
        } catch (RemoteException re) {
            CommonUtil.logRE(log, "getCollabNetApp", re);
            // more logging
            log.log(java.util.logging.Level.SEVERE, "getCollabNetApp failed", 
                    re);
            return null;
        }
    }

    /**
     * @param url
     * @param username
     * @param sessionId
     * @return collabnet app, if one can be created; null otherwise.
     */
    public static CollabNetApp recreateCollabNetApp(String url, String username, String sessionId) {
        if (CommonUtil.unset(url)) {
            return null;
        }
        return new CollabNetApp(url, username, null, sessionId);
    }

    /**
     * Get a CollabNetApp, given a StaplerRequest with url, username, and
     * password set.  If login fails, null will be returned.
     */
    public static CollabNetApp getCollabNetApp(StaplerRequest request) {
        String url = null;
        String username = null;
        String password = null;
        String override_auth = request.getParameter("override_auth");
        TeamForgeShare.TeamForgeShareDescriptor descriptor = 
            TeamForgeShare.getTeamForgeShareDescriptor();
        if (descriptor != null && descriptor.useGlobal() && 
            override_auth != null && override_auth.equals("false")) {
            url = descriptor.getCollabNetUrl();
            username = descriptor.getUsername();
            password = descriptor.getPassword();
        } else {
            url = request.getParameter("url");
            username = request.getParameter("username");
            password = request.getParameter("password");
        }

        if (CommonUtil.unset(url) || CommonUtil.unset(username) 
            || CommonUtil.unset(password)) {
            return null;
        }
        return getCollabNetApp(url, username, password);
    }

    /**
     * @return the username from the stapler request or the global value, 
     *         if applicable.
     */
    public static String getUsername(StaplerRequest request) {
        String username = null;
        String override_auth = request.getParameter("override_auth");
        TeamForgeShare.TeamForgeShareDescriptor descriptor = 
            TeamForgeShare.getTeamForgeShareDescriptor();
        if (descriptor != null && descriptor.useGlobal() && 
            override_auth != null && override_auth.equals("false")) {
            username = descriptor.getUsername();
        } else {
            username = request.getParameter("username");
        }
        return username;
    }

    /**
     * Logs off the CollabNetApp, if possible.
     *
     * @param cna CollabNetApp
     */
    public static void logoff(CollabNetApp cna) {
        if (cna != null) {
            try {
                cna.logoff();
            } catch (RemoteException re) {
                CommonUtil.logRE(log, "logoff", re);
            }
        }
    }

    /**
     * Get a releaseId, given a projectId and a release title.
     *
     * @param release
     * @return the releaseId in this project which matches the release name
     *         or null if none is found.
     */
    public static CTFRelease getProjectReleaseId(CTFProject project,  String release) throws RemoteException {
        if (project==null)  return null;

        for (CTFPackage pkg : project.getPackages()) {
            CTFRelease r = pkg.getReleaseByTitle(release);
            if (r!=null)    return r;
        }
        return null;
    }

    /**
     * @param cna for accessing the webservice methods.
     * @param username
     * @return true if the user can be found.
     */
    public static boolean isUserValid(CollabNetApp cna, String username) {
        boolean valid = false;
        try {
            valid = cna.isUsernameValid(username);
        } catch (RemoteException re) {
            CommonUtil.logRE(log, "userExists", re);
        }
        return valid;
    }

    /**
     * @param cna for accessing the webservice methods.
     * @param collabnetUrl the collabnet url
     * @param projectName name of the project
     * @param repoName name of the repository
     * @return the scm viewer url
     */
    public static String getScmViewerUrl(CollabNetApp cna, String collabnetUrl, String projectName, String repoName) throws RemoteException {
        String url = null;
        CTFScmRepository repo = CNHudsonUtil.getRepoData(cna, projectName, repoName);
        if (repo != null) {
            // normally, just use the defined scm viewer url
            url = repo.getScmViewerUrl();

            if (cna != null) {
                VersionNumber apiVersion;
                try {
                    apiVersion = new VersionNumber(cna.getApiVersion());
                } catch (RemoteException re) {
                    CommonUtil.logRE(log, "getScmViewerUrl", re);
                    return null;
                }

                if (new VersionNumber("5.3.0.0").compareTo(apiVersion)<=0
                &&  apiVersion.compareTo(new VersionNumber("6.0.0.0"))<0) {
                    // starting with CTF 5.3, you can use the new viewRepositorySource method that does auth for viewvc
                    url = collabnetUrl + "/sf/scm/do/viewRepositorySource/" + repo.getPath();
                }
            }
        }
        return url;
    }

    /**
     * Turn version string into an array, where each version is in its own index/pos.
     * @param apiVersionStr
     * @return
     */
    public static int[] getVersionArray(String apiVersionStr) {
        int[] versionNums = null;
        if (apiVersionStr != null) {
            String[] versionArr = apiVersionStr.split("\\.");
            versionNums = new int[versionArr.length];
            for (int i = 0; i < versionArr.length; i++) {
                versionNums[i] = Integer.parseInt(versionArr[i]);
            }
        }
        return versionNums;
    }

    /**
     * @param cna for accessing the webservice methods.
     * @param projectName
     * @param repoName
     */
    public static String getSystemId(CollabNetApp cna, String projectName, 
                                     String repoName) throws RemoteException {
        CTFProject p = cna.getProjectByTitle(projectName);
        if (p==null)    return null;
        CTFScmRepository r = p.getScmRepositories().byTitle(repoName);
        if (r==null)    return null;
        return r.getSystemId();
    }
    
    /**
     * @param cna for accessing the webservice methods.
     * @param projectName
     * @param repoName
     */
    private static CTFScmRepository getRepoData(CollabNetApp cna,
                                                 String projectName,
                                                 String repoName) throws RemoteException {
        CTFProject p = cna.getProjectByTitle(projectName);
        if (p==null) {
            return null;
        }
        return p.getScmRepositories().byTitle(repoName);
    }

    /**
     * Sanitizes a CollabNet url and make it appropriate to be used by this plugin.
     * @param url original url
     * @return sanitized collabnet url
     */
    public static String sanitizeCollabNetUrl(String url) {
        // strip the trailing "/" from the collabnet url, as this causes browser to log off session (artf51846)
        if (url != null && url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
