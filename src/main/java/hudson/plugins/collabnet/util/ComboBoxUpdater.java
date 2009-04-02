package hudson.plugins.collabnet.util;

import com.collabnet.ce.webservices.CollabNetApp;
import com.collabnet.ce.webservices.FrsApp;
import com.collabnet.ce.webservices.TrackerApp;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * These classes are used to update the list of items for a combo box.
 */
public abstract class ComboBoxUpdater {
    protected static Logger log = Logger.getLogger("ComboBoxUpdater");

    protected final StaplerRequest request;
    protected final StaplerResponse response;

    protected ComboBoxUpdater(StaplerRequest req, StaplerResponse rsp) {
        this.request = req;
        this.response = rsp;
    }

    protected abstract Collection<String> getList();

    public void update() throws IOException {
        Collection<String> list = this.getList();
        this.response.setContentType("text/plain;charset=UTF-8");
        JSONObject jsonObj = new JSONObject();
        jsonObj.element("items", list);
        this.response.getWriter().print(jsonObj.toString());
    }

    /**
     * Class to update a list of projects.  Requires the login info (url,
     * username, password) be set.
     */
    public static class ProjectsUpdater extends ComboBoxUpdater {
        
        public ProjectsUpdater(StaplerRequest req, StaplerResponse rsp) {
            super(req, rsp);
        }

        protected Collection<String> getList() {
            CollabNetApp cna = CNHudsonUtil.getCollabNetApp(this.request);
            Collection<String> list = getProjectList(cna);
            CNHudsonUtil.logoff(cna);
            return list;
        }

        /**
         * @return a list of projects which has been sanitized.
         */
        public static Collection<String> getProjectList(CollabNetApp cna) {
            Collection<String> projects = Collections.emptyList();
            if (cna != null) {
                try {
                    projects = cna.getProjects();
                } catch (RemoteException re) {
                    CommonUtil.logRE(log, "getProjectList", re);
                }
            }
            return CommonUtil.sanitizeForJS(projects);
        }
    }

    /**
     * Class to update a list of packages.  Requires that the login info (url,
     * username, password) be set, as well as the project.
     */
    public static class PackagesUpdater extends ComboBoxUpdater {
        
        public PackagesUpdater(StaplerRequest req, StaplerResponse rsp) {
            super(req, rsp);
        }

        protected Collection<String> getList() {
            CollabNetApp cna = CNHudsonUtil.getCollabNetApp(this.request);
            String project = request.getParameter("project");
            String projectId = CNHudsonUtil.getProjectId(cna, project);
            Collection<String> list = getPackageList(cna, projectId);
            CNHudsonUtil.logoff(cna);
            return list;
        }

        /**
         * @return a list of packages which has been sanitized.
         */
        public static Collection<String> getPackageList(CollabNetApp cna, 
                                                        String projectId) {
            Collection<String> packages = Collections.emptyList();
            if (cna != null && projectId != null) {
                FrsApp fa = new FrsApp(cna);
                try {
                    packages = fa.getPackages(projectId);
                } catch (RemoteException re) {
                    CommonUtil.logRE(log, "getPackageList", re);
                }
            }
            return CommonUtil.sanitizeForJS(packages);
        }
    }

    /**
     * Class to update the list of releases.  Requires a StaplerRequest with
     * login info (url, username, password), project, and (optionally) package.
     */
    public static class ReleasesUpdater extends ComboBoxUpdater {
        
        public ReleasesUpdater(StaplerRequest req, StaplerResponse rsp) {
            super(req, rsp);
        }

        protected Collection<String> getList() {
            CollabNetApp cna = CNHudsonUtil.getCollabNetApp(this.request);
            String project = request.getParameter("project");
            String projectId = CNHudsonUtil.getProjectId(cna, project);
            String rpackage = request.getParameter("package");
            Collection<String> list = Collections.emptyList();
            if (CommonUtil.unset(rpackage)) {
                list = getProjectReleaseList(cna, projectId);
            } else {
                String packageId = CNHudsonUtil.getPackageId(cna, rpackage, 
                                                             projectId);
                list = getReleaseList(cna, packageId);
            }
            CNHudsonUtil.logoff(cna);
            return list;
        }

        /**
         * @return a list of releases in the package which has been sanitized.
         */
        public static Collection<String> getReleaseList(CollabNetApp cna, 
                                                        String packageId) {
            Collection<String> releases = Collections.emptyList();
            if (cna != null && packageId != null) {
                FrsApp fa = new FrsApp(cna);
                try {
                    releases = fa.getReleases(packageId);
                } catch (RemoteException re) {
                    CommonUtil.logRE(log, "getReleaseList", re);
                }
            }
            return CommonUtil.sanitizeForJS(releases);
        }

        /**
         * @return a list of all releases in the project which has been 
         *         sanitized.
         */
        public static Collection<String> getProjectReleaseList
            (CollabNetApp cna, String projectId) {
            Collection<String> releases = Collections.emptyList();
            if (cna != null && projectId != null) {
                FrsApp fa = new FrsApp(cna);
                try {
                    releases = fa.getProjectReleases(projectId);
                } catch (RemoteException re) {
                    CommonUtil.logRE(log, "getProjectReleaseList", re);
                }
            }
            return CommonUtil.sanitizeForJS(releases);
        }
    }

    /**
     * Class to update a list of trackers.  Requires that the login info (url,
     * username, password) be set, as well as the project.
     */
    public static class TrackersUpdater extends ComboBoxUpdater {
        
        public TrackersUpdater(StaplerRequest req, StaplerResponse rsp) {
            super(req, rsp);
        }

        protected Collection<String> getList() {
            CollabNetApp cna = CNHudsonUtil.getCollabNetApp(this.request);
            String project = request.getParameter("project");
            String projectId = CNHudsonUtil.getProjectId(cna, project);
            Collection<String> list = getTrackerList(cna, projectId);
            CNHudsonUtil.logoff(cna);
            return list;
        }

        /**
         * @return a list of trackers which has been sanitized.
         */
        public static Collection<String> getTrackerList(CollabNetApp cna, 
                                                        String projectId) {
            Collection<String> trackers = Collections.emptyList();
            if (cna != null && projectId != null) {
                TrackerApp ta = new TrackerApp(cna);
                try {
                    trackers = ta.getTrackers(projectId);
                } catch (RemoteException re) {
                    CommonUtil.logRE(log, "getTrackerList", re);
                }
            }
            return CommonUtil.sanitizeForJS(trackers);
        }
    }
    
    /**
     * Class to update a list of project users.  Requires that the login info 
     * (url, username, password) be set, as well as the project.
     */
    public static class UsersUpdater extends ComboBoxUpdater {
        
        public UsersUpdater(StaplerRequest req, StaplerResponse rsp) {
            super(req, rsp);
        }

        protected Collection<String> getList() {
            CollabNetApp cna = CNHudsonUtil.getCollabNetApp(this.request);
            String project = request.getParameter("project");
            String projectId = CNHudsonUtil.getProjectId(cna, project);
            Collection<String> list = getUserList(cna, projectId);
            CNHudsonUtil.logoff(cna);
            return list;
        }

        /**
         * @return a list of usernames which has been sanitized.
         */
        public static Collection<String> getUserList(CollabNetApp cna, 
                                                     String projectId) {
            Collection<String> users = Collections.emptyList();
            if (cna != null && projectId != null) {
                try {
                    users = cna.getUsers(projectId);
                } catch (RemoteException re) {
                    CommonUtil.logRE(log, "getUserList", re);
                }
            }
            return CommonUtil.sanitizeForJS(users);
        }
    }
}
