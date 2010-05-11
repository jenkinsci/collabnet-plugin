package hudson.plugins.collabnet.util;

import com.collabnet.ce.soap50.webservices.scm.Repository2SoapDO;
import com.collabnet.ce.soap50.webservices.tracker.ArtifactSoapDO;
import com.collabnet.ce.webservices.CollabNetApp;
import com.collabnet.ce.webservices.FrsApp;
import com.collabnet.ce.webservices.ScmApp;
import com.collabnet.ce.webservices.TrackerApp;
import hudson.model.Hudson;
import hudson.plugins.collabnet.auth.CollabNetSecurityRealm;
import hudson.plugins.collabnet.share.TeamForgeShare;
import hudson.security.SecurityRealm;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.rmi.RemoteException;
import java.util.logging.Logger;

/**
 * Class for methods that are useful across Hudson plugins.
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
     * Get the packageId for the package name.
     *
     * @param cna the collab net app instance to use
     * @param rpackage name of the package.
     * @param projectId the project id.
     * @return the package id if found, null otherwise.
     */
    public static String getPackageId(CollabNetApp cna, String rpackage, 
                                String projectId) {
        String packageId = null;
        if (cna != null && projectId != null) {
            FrsApp fa = new FrsApp(cna);
            try {
                packageId = fa.findPackageId(rpackage, projectId);
            } catch (RemoteException re) {
                CommonUtil.logRE(log, "getPackageId", re);
                return null;
            }
        }
        return packageId;
    }

    /**
     * Get the projectId for the project name.
     *
     * @param cna for accessing the webservice methods.
     * @param project name.
     * @return the id for the project or null, if no such project exist.
     */
    public static String getProjectId(CollabNetApp cna, String project) {
        String projectId = null;
        if (cna != null) {
            try {
                projectId = cna.getProjectId(project);
            } catch (RemoteException re) {
                CommonUtil.logRE(log, "getProjectId", re);
            }
        } 
        return projectId;
    }

    /**
     * Get the project name for the project with given id.
     *
     * @param cna for accessing the webservice methods.
     * @param projectId id.
     * @return the id for the project or null, if no such project exist.
     */
    public static String getProjectName(CollabNetApp cna, String projectId) {
        String projectName = null;
        if (cna != null) {
            projectName = cna.getProjectName(projectId);
        }
        return projectName;
    }

    /**
     * Get the release id.
     *
     * @param cna for accessing the webservice methods.
     * @param packageId the id of the package which contains this release.
     * @param release the name of the release
     * @return the release id, or null if none is found.
     */
    public static String getReleaseId(CollabNetApp cna, String packageId, 
                               String release) {
        if (cna == null || packageId == null) {
            return null;
        }
        String releaseId = null;
        FrsApp fa = new FrsApp(cna);
        try {
            releaseId = fa.findReleaseIdByPackage(release, packageId);
        } catch (RemoteException re) {
            CommonUtil.logRE(log, "getReleaseId", re);
        }
        return releaseId;
    }

    /**
     * Get the file id.
     *
     * @param cna for accessing the webservice methods.
     * @param releaseId the id of the release.
     * @param file name
     * @return the file id, or null if none is found.
     */
    public static String getFileId(CollabNetApp cna, String releaseId, 
                                   String file) {
        if (cna == null || releaseId == null) {
            return null;
        }
        String fileId = null;
        FrsApp fa = new FrsApp(cna);
        try {
            fileId = fa.findFrsFile(file, releaseId);
        } catch (RemoteException re) {
            CommonUtil.logRE(log, "getFileId", re);
        }
        return fileId;
    }

    /**
     * Get a releaseId, given a projectId and a release title.
     *
     * @param cna for accessing the webservice methods.
     * @param projectId
     * @param release
     * @return the releaseId in this project which matches the release name
     *         or null if none is found.
     */
    public static String getProjectReleaseId (CollabNetApp cna, 
                                              String projectId,
                                              String release) {
        if (cna == null || projectId == null) {
            return null;
        }
        FrsApp fa = new FrsApp(cna);
        String releaseId = null;
        try {
            releaseId = fa.findReleaseId(release, projectId);
        } catch (RemoteException re) {
            CommonUtil.logRE(log, "getProjectReleaseId", re);
        }
        return releaseId;
    }

    /**
     * Given a tracker title and a projectId, find the matching tracker id.
     * 
     * @param cna for accessing the webservice methods.
     * @param projectId
     * @param trackerName
     * @return the tracker id for the tracker that matches this name, or null
     *         if no matching tracker is found.
     */
    public static String getTrackerId(CollabNetApp cna, String projectId, 
                                      String trackerName) {
        if (cna == null || projectId == null) {
            return null;
        }
        TrackerApp ta = new TrackerApp(cna);
        String trackerId = null;
        try {
            trackerId = ta.getTrackerId(projectId, trackerName);
        } catch (RemoteException re) {
            CommonUtil.logRE(log, "getTrackerId", re);
            return null;
        }
        return trackerId;
    }

    /**
     * Given a project and issue title, find the most recent matching 
     * issue object, or null if none matches.
     */
    public static ArtifactSoapDO getTrackerArtifact(CollabNetApp cna, 
                                                    String project, 
                                                    String tracker, 
                                                    String issueTitle) {
        ArtifactSoapDO artifact = null;
        if (cna != null) {
            String projectId = CNHudsonUtil.getProjectId(cna, project);
            if (projectId != null) {
                String trackerId = CNHudsonUtil.getTrackerId(cna, projectId, 
                                                             tracker);
                if (trackerId != null) {
                    TrackerApp ta = new TrackerApp(cna);
                    try {
                        artifact = ta.findLastTrackerArtifact(trackerId, 
                                                              issueTitle);
                    } catch (RemoteException re) {}
                }
            }
        }
        return artifact;
    }
    
    /**
     * Given project, package, release, and file name, find the fileId
     * for a file in the system, or null if none is found.
     *
     * @param cna for accessing webservice methods
     * @param project name
     * @param rpackage name
     * @param release name
     * @param file name
     */
    public static String getFileId(CollabNetApp cna, String project, 
                                   String rpackage, String release, 
                                   String file) {
        String fileId = null;
        String projectId = CNHudsonUtil.getProjectId(cna, project);
        if (projectId != null) {
            String packageId = CNHudsonUtil.getPackageId(cna, rpackage, 
                                                         projectId);
            if (packageId != null) {
                String releaseId = CNHudsonUtil.getReleaseId(cna, packageId, 
                                                             release);
                if (releaseId != null) {
                    fileId = CNHudsonUtil.getFileId(cna, releaseId, file);
                }
            }
        }
        return fileId;
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
     * @param username
     * @param projectId
     * @return true if the user is a member of the project.
     */
    public static boolean isUserMember(CollabNetApp cna,
                                       String username,
                                       String projectId) {
        boolean member = false;
        try {
            member = cna.isUserMemberOfProject(username, projectId);
        } catch (RemoteException re) {
            CommonUtil.logRE(log, "userMember", re);
        }
        return member;
    }

    /**
     * @param cna for accessing the webservice methods.
     * @param collabnetUrl the collabnet url
     * @param projectName name of the project
     * @param repoName name of the repository
     * @return the scm viewer url
     */
    public static String getScmViewerUrl(CollabNetApp cna, String collabnetUrl, String projectName, String repoName) {
        String url = null;
        Repository2SoapDO repoData = CNHudsonUtil.getRepoData(cna, projectName, repoName);
        if (repoData != null) {
            // normally, just use the defined scm viewer url
            url = repoData.getScmViewerUrl();

            if (cna != null) {
                int apiVersion[] = null;
                try {
                    apiVersion = getVersionArray(cna.getApiVersion());
                } catch (RemoteException re) {
                    CommonUtil.logRE(log, "getScmViewerUrl", re);
                }

                if (isSupportedVersion(new int[] {5, 3, 0, 0}, new int[] {6, 0, 0, 0}, apiVersion)) {
                    // starting with CTF 5.3, you can use the new viewRepositorySource method that does auth for viewvc
                    url = collabnetUrl + "/sf/scm/do/viewRepositorySource/" + repoData.getPath();
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
     * Check if the actual version is within the range of the start/end support version
     * @param startSupportVersion the start version, inclusive. null to ignore check.
     * @param endSupportVersion the ending version, not inclusive. null to ignore check.
     * @param actualVersion the actual version
     * @return true if actual version is between start version (inclusive) and end version (non inclusive)
     */
    public static boolean isSupportedVersion(int[] startSupportVersion, int[] endSupportVersion, int[] actualVersion) {
        if (actualVersion == null || actualVersion.length != 4) {
            log.warning("Unable to determine api version: isSupportedVersion returning false");
            return false;
        }

        if (startSupportVersion != null) {
            if (startSupportVersion.length != 4) {
                return false;
            }
            if (compareVersion(actualVersion, startSupportVersion) == -1) {
                // means actual version is before the start support version
                return false;
            }
        }

        if (endSupportVersion != null) {
            if (endSupportVersion.length != 4) {
                return false;
            }
            if (compareVersion(actualVersion, endSupportVersion) != -1) {
                // means actual version is either after or the same as endSupport version
                return false;
            }
        }
        return true;
    }

    /**
     * Compare two equal length version array
     * @param version1 first version
     * @param version2 second version
     * @return -1 if version1 is less than version2, 0 if they are the same, and 1 if version1 is greater than version2
     */
    public static int compareVersion(int[] version1, int[] version2) {
        for (int i=0; i < version1.length; i++) {
            int v1 = version1[i];
            int v2 = version2[i];
            if (v1 > v2) {
                return 1;
            } else if (v1 < v2) {
                return -1;
            }
        }
        return 0;
    }

    /**
     * @param cna for accessing the webservice methods.
     * @param projectName
     * @param repoName
     */
    public static String getSystemId(CollabNetApp cna, String projectName, 
                                     String repoName) {
        String systemId = null;
        Repository2SoapDO repoData = CNHudsonUtil.getRepoData(cna, projectName,
                                                              repoName);
        if (repoData != null) {
            systemId = repoData.getSystemId();
        }
        return systemId;
    }
    
    /**
     * @param cna for accessing the webservice methods.
     * @param projectName
     * @param repoName
     */
    private static Repository2SoapDO getRepoData(CollabNetApp cna, 
                                                 String projectName,
                                                 String repoName) {
        Repository2SoapDO repoData = null;
        String projectId = CNHudsonUtil.getProjectId(cna, projectName);
        if (projectId == null) {
            return null;
        }
        String repoId = CNHudsonUtil.getRepoId(cna, projectId, repoName);
        if (repoId == null) {
            return null;
        }
        ScmApp sa = new ScmApp(cna);
        try {
            repoData = sa.getRepoData(repoId);
        } catch (RemoteException re) {
            CommonUtil.logRE(log, "getScmViewerUrl", re);
        }
        return repoData;
    }

    /**
     * @param cna for accessing the webservice methods.
     * @param projectId
     * @param repoName
     */
    public static String getRepoId(CollabNetApp cna, String projectId, 
                                   String repoName) {
        String repoId = null;
        ScmApp sa = new ScmApp(cna);
        try {
            repoId = sa.getRepoId(projectId, repoName);
        } catch (RemoteException re) {
            CommonUtil.logRE(log, "getRepoId", re);
        }
        return repoId;
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
