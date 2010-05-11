package hudson.plugins.collabnet.util;

import com.collabnet.ce.webservices.CollabNetApp;
import com.collabnet.ce.webservices.FrsApp;
import com.collabnet.ce.webservices.ScmApp;
import com.collabnet.ce.webservices.TrackerApp;
import hudson.util.ComboBoxModel;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * These classes are used to update the list of items for a combo box.
 */
public abstract class ComboBoxUpdater {
    protected static Logger log = Logger.getLogger("ComboBoxUpdater");

    private ComboBoxUpdater() {
    }

    /**
     * @return a list of projects which has been sanitized.
     */
    public static ComboBoxModel getProjectList(CollabNetApp cna) {
        if (cna != null) {
            try {
                return new ComboBoxModel(cna.getProjects());
            } catch (RemoteException re) {
                CommonUtil.logRE(log, "getProjectList", re);
            }
        }
        return EMPTY_MODEL;
    }

    /**
     * Obtains the list of packages in the given project.
     */
    public static ComboBoxModel getPackages(CollabNetApp cna, String project) {
        if (cna==null)    return EMPTY_MODEL;

        String projectId = CNHudsonUtil.getProjectId(cna, project);
        if (projectId==null)    return EMPTY_MODEL;

        FrsApp fa = new FrsApp(cna);
        try {
            return new ComboBoxModel(fa.getPackages(projectId));
        } catch (RemoteException re) {
            return EMPTY_MODEL;
        }
    }

    public static ComboBoxModel getReleases(CollabNetApp cna, String project, String rpackage) {
        if (cna==null)    return EMPTY_MODEL;

        String projectId = CNHudsonUtil.getProjectId(cna, project);
        if (projectId==null)    return EMPTY_MODEL;

        if (CommonUtil.unset(rpackage)) {
            return getProjectReleaseList(cna, projectId);
        } else {
            String packageId = CNHudsonUtil.getPackageId(cna, rpackage, projectId);
            return getReleaseList(cna, packageId);
        }
    }

    /**
     * @return a list of releases in the package which has been sanitized.
     */
    public static ComboBoxModel getReleaseList(CollabNetApp cna, String packageId) {
        if (cna != null && packageId != null) {
            FrsApp fa = new FrsApp(cna);
            try {
                return new ComboBoxModel(fa.getReleases(packageId));
            } catch (RemoteException re) {
                CommonUtil.logRE(log, "getReleaseList", re);
            }
        }
        return EMPTY_MODEL;
    }

    /**
     * @return a list of all releases in the project which has been
     *         sanitized.
     */
    public static ComboBoxModel getProjectReleaseList(CollabNetApp cna, String projectId) {
        if (cna != null && projectId != null) {
            FrsApp fa = new FrsApp(cna);
            try {
                return new ComboBoxModel(fa.getProjectReleases(projectId));
            } catch (RemoteException re) {
                CommonUtil.logRE(log, "getProjectReleaseList", re);
            }
        }
        return EMPTY_MODEL;
    }

    public static ComboBoxModel getRepos(CollabNetApp cna, String project) {
        String projectId = CNHudsonUtil.getProjectId(cna, project);
        ComboBoxModel list = getRepoList(cna, projectId);
        CNHudsonUtil.logoff(cna);
        return list;
    }

    /**
     * @return a list of repos which has been sanitized.
     */
    public static ComboBoxModel getRepoList(CollabNetApp cna,
                                                 String projectId) {
        if (cna != null) {
            ScmApp sa = new ScmApp(cna);
            try {
                return new ComboBoxModel(sa.getRepos(projectId));
            } catch (RemoteException re) {
                CommonUtil.logRE(log, "getRepoList", re);
            }
        }
        return EMPTY_MODEL;
    }

    public static ComboBoxModel getTrackers(CollabNetApp cna, String project) {
        String projectId = CNHudsonUtil.getProjectId(cna, project);
        ComboBoxModel list = getTrackerList(cna, projectId);
        CNHudsonUtil.logoff(cna);
        return list;
    }

    /**
     * @return a list of trackers which has been sanitized.
     */
    public static ComboBoxModel getTrackerList(CollabNetApp cna,
                                                    String projectId) {
        if (cna != null && projectId != null) {
            TrackerApp ta = new TrackerApp(cna);
            try {
                return new ComboBoxModel(ta.getTrackers(projectId));
            } catch (RemoteException re) {
                CommonUtil.logRE(log, "getTrackerList", re);
            }
        }
        return EMPTY_MODEL;
    }

    public static ComboBoxModel getUsers(CollabNetApp cna, String project) {
        String projectId = CNHudsonUtil.getProjectId(cna, project);
        ComboBoxModel list = getUserList(cna, projectId);
        CNHudsonUtil.logoff(cna);
        return list;
    }

    /**
     * @return a list of usernames which has been sanitized.
     */
    public static ComboBoxModel getUserList(CollabNetApp cna,
                                                 String projectId) {
        if (cna != null && projectId != null) {
            try {
                return new ComboBoxModel(cna.getUsers(projectId));
            } catch (RemoteException re) {
                CommonUtil.logRE(log, "getUserList", re);
            }
        }
        return EMPTY_MODEL;
    }

    private static final ComboBoxModel EMPTY_MODEL = new ComboBoxModel();
}
