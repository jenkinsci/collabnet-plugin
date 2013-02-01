package hudson.plugins.collabnet.util;

import com.collabnet.ce.webservices.CTFPackage;
import com.collabnet.ce.webservices.CTFProject;
import com.collabnet.ce.webservices.CTFRelease;
import com.collabnet.ce.webservices.CollabNetApp;
import com.collabnet.ce.webservices.ObjectWithTitle;
import hudson.util.ComboBoxModel;

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
                return toModel(cna.getProjects());
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

        return toModel(p.getPackages());
    }

    public static ComboBoxModel getReleases(CollabNetApp cna, String project, String rpackage) throws RemoteException {
        if (cna==null)    return EMPTY_MODEL;

        CTFProject p = cna.getProjectByTitle(project);
        if (p==null)    return EMPTY_MODEL;

        if (CommonUtil.unset(rpackage)) {
            return getReleaseList(p);
        } else {
            CTFPackage pkg = p.getPackages().byTitle(rpackage);
            return getReleaseList(pkg);
        }
    }

    /**
     * @return a list of releases in the package which has been sanitized.
     */
    public static ComboBoxModel getReleaseList(CTFPackage pkg) throws RemoteException {
        if (pkg == null)  return EMPTY_MODEL;
        return toModel(pkg.getReleases());
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

    public static ComboBoxModel getRepos(CollabNetApp cna, String project) throws RemoteException {
        if (cna==null)  return EMPTY_MODEL;
        CTFProject p =  cna.getProjectByTitle(project);
        if (p==null)    return EMPTY_MODEL;
        return toModel(p.getScmRepositories());
    }

    public static ComboBoxModel toModel(Collection<? extends ObjectWithTitle> list) {
        ComboBoxModel r = new ComboBoxModel();
        for (ObjectWithTitle t : list)
            r.add(t.getTitle());
        return r;
    }

    /**
     * @return a list of trackers which has been sanitized.
     */
    public static ComboBoxModel getTrackerList(CTFProject p) throws RemoteException {
        if (p!=null)
            return toModel(p.getTrackers());
        return EMPTY_MODEL;
    }

    public static ComboBoxModel getUsers(CollabNetApp cna, String project) throws RemoteException {
        if (cna!=null) {
            CTFProject p = cna.getProjectByTitle(project);
            if (p==null)    return EMPTY_MODEL;
            return toModel(p.getMembers());
        }
        return EMPTY_MODEL;
    }

    /**
     * @return a list of usernames which has been sanitized.
     */
    public static ComboBoxModel getUserList(CTFProject p) {
        if (p!=null) {
            try {
                return toModel(p.getMembers());
            } catch (RemoteException re) {
                CommonUtil.logRE(log, "getUserList", re);
            }
        }
        return EMPTY_MODEL;
    }

    private static final ComboBoxModel EMPTY_MODEL = new ComboBoxModel();
}
