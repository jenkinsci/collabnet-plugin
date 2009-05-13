package hudson.plugins.collabnet.util;

import com.collabnet.ce.webservices.CollabNetApp;
import com.collabnet.ce.webservices.FrsApp;
import com.collabnet.ce.webservices.TrackerApp;
import com.collabnet.ce.soap50.webservices.tracker.ArtifactSoapDO;

import hudson.plugins.collabnet.share.TeamForgeShare;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import org.kohsuke.stapler.StaplerRequest;

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
     * @param fa for accessing the FileReleaseSystem webservices.
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
}
