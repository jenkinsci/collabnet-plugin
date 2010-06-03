package hudson.plugins.collabnet.util;

import com.collabnet.ce.webservices.CTFPackage;
import com.collabnet.ce.webservices.CTFProject;
import com.collabnet.ce.webservices.CTFRelease;
import com.collabnet.ce.webservices.CollabNetApp;
import com.collabnet.ce.webservices.ScmApp;
import com.collabnet.ce.webservices.TrackerApp;
import hudson.util.ComboBoxModel;

import java.rmi.RemoteException;
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
                ComboBoxModel r = new ComboBoxModel();
                for (CTFProject p : cna.getProjects())
                    r.add(p.getTitle());
                return r;
            } catch (RemoteException re) {
                CommonUtil.logRE(log, "getProjectList", re);
            }
        }
        return EMPTY_MODEL;
    }

    /**
     * Obtains the list of packages in the given project.
     */
    public static ComboBoxModel getPackages(CollabNetApp cna, String project) throws RemoteException {
        if (cna==null)    return EMPTY_MODEL;

        CTFProject p = cna.getProjectByTitle(project);
        if (p==null)    return EMPTY_MODEL;

        ComboBoxModel cbm = new ComboBoxModel();
        for (CTFPackage pkg : p.getPackages())
            cbm.add(pkg.getTitle());
        return cbm;
    }

    public static ComboBoxModel getReleases(CollabNetApp cna, String project, String rpackage) throws RemoteException {
        if (cna==null)    return EMPTY_MODEL;

        CTFProject p = cna.getProjectByTitle(project);
        if (p==null)    return EMPTY_MODEL;

        if (CommonUtil.unset(rpackage)) {
            return getReleaseList(p);
        } else {
            CTFPackage pkg = p.getPackageByTitle(rpackage);
            return getReleaseList(pkg);
        }
    }

    /**
     * @return a list of releases in the package which has been sanitized.
     */
    public static ComboBoxModel getReleaseList(CTFPackage pkg) throws RemoteException {
        if (pkg == null)  return EMPTY_MODEL;

        ComboBoxModel cbm = new ComboBoxModel();
        for (CTFRelease r : pkg.getReleases()) {
            cbm.add(r.getTitle());
        }
        return cbm;
    }

    /**
     * @return a list of all releases in the project which has been
     *         sanitized.
     */
    public static ComboBoxModel getReleaseList(CTFProject p) throws RemoteException {
        if (p == null)  return EMPTY_MODEL;

        ComboBoxModel cbm = new ComboBoxModel();
        for (CTFPackage pkg : p.getPackages()) {
            for (CTFRelease r : pkg.getReleases()) {
                cbm.add(r.getTitle());
            }
        }
        return cbm;
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
