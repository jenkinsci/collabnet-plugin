package hudson.plugins.collabnet.browser;

import com.collabnet.ce.webservices.CollabNetApp;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.plugins.collabnet.share.TeamForgeShare;
import hudson.plugins.collabnet.util.CNFormFieldValidator;
import hudson.plugins.collabnet.util.CNHudsonUtil;
import hudson.plugins.collabnet.util.ComboBoxUpdater;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SubversionChangeLogSet;
import hudson.scm.SubversionRepositoryBrowser;
import hudson.util.FormValidation;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.logging.Logger;

public class TeamForge extends SubversionRepositoryBrowser {
    private static Logger log = Logger.getLogger("TeamForge");

    private String collabneturl;
    private String username;
    private Secret password;
    private String project;
    private String repo;
    private boolean overrideAuth;

    private transient static TeamForgeShare.TeamForgeShareDescriptor 
        shareDescriptor = null;

    /**
     * DataBoundConstructor for building the object from form data.
     */
    @DataBoundConstructor
    public TeamForge(String collabneturl, String username, String password, 
                     String project, String repo, OverrideAuth override_auth) 
    { 
        if (override_auth != null) {
            this.overrideAuth = true;
            this.collabneturl = override_auth.collabneturl;
            this.username = override_auth.username;
            this.password = override_auth.password;
        } else {
            if (collabneturl == null && username == null && password == null) {
                this.overrideAuth = false;
            } else {
                this.overrideAuth = true;
                this.collabneturl = CNHudsonUtil.sanitizeCollabNetUrl(collabneturl);
                this.username = username;
                this.password = Secret.fromString(password);
            }
        }
        this.project = project;
        this.repo = repo;
    }
    
    /**
     * Simple constructors for rebuilding the object from config data.
     */
    public TeamForge(String collabneturl, String username, String password, 
                     String project, String repo, boolean overrideAuth) {
        this.collabneturl = CNHudsonUtil.sanitizeCollabNetUrl(collabneturl);
        this.username = username;
        this.password = Secret.fromString(password);
        this.project = project;
        this.repo = repo;
        this.overrideAuth = overrideAuth;
    }

    public TeamForge(String project, String repo, boolean overrideAuth) {
        this(null, null, null, project, repo, overrideAuth);
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @Override
    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    /**
     * @return whether or not auth is overriden
     */
    public boolean overrideAuth() {
        return this.overrideAuth;
    }

    /**
     * @return the collabneturl for the CollabNet server.
     */
    public String getCollabNetUrl() {
        if (this.overrideAuth()) {
            return this.collabneturl;
        } else {
            return getTeamForgeShareDescriptor().getCollabNetUrl();
        }
    }

    /**
     * @return the username used for logging in.
     */
    public String getUsername() {
        if (this.overrideAuth()) {
            return this.username;            
        } else {
            return getTeamForgeShareDescriptor().getUsername();
        }
    }

    /**
     * @return the password used for logging in.
     */
    public String getPassword() {
        if (this.overrideAuth()) {
            return this.password==null ? null : this.password.toString();
        } else {
            return getTeamForgeShareDescriptor().getPassword();
        }
    }

    /**
     * Get the name of the project
     * @return project name
     */
    public String getProject() {
        return this.project;
    }

    /**
     * Get the name of the repository.
     * @return repository name
     */
    public String getRepo() {
        return this.repo;
    }

    /**
     * @return the TeamForge share descriptor.
     */
    public static TeamForgeShare.TeamForgeShareDescriptor 
        getTeamForgeShareDescriptor() {
        if (shareDescriptor == null) {
            shareDescriptor = TeamForgeShare.getTeamForgeShareDescriptor();
        }
        return shareDescriptor;
    }

    /**
     * @return the list of all possible projects, given the login data.
     */
    public String[] getProjects() {
        CollabNetApp cna = CNHudsonUtil.getCollabNetApp(this.getCollabNetUrl(),
                                                        this.getUsername(), 
                                                        this.getPassword());
        Collection<String> projects = ComboBoxUpdater.ProjectsUpdater
            .getProjectList(cna);
        CNHudsonUtil.logoff(cna);
        return projects.toArray(new String[0]);
    }

    /**
     * @return the list of all possible repos, given the login and project.
     */
    public String[] getRepos() {
        CollabNetApp cna = CNHudsonUtil.getCollabNetApp(getCollabNetUrl(), getUsername(), getPassword());
        String[] reposArray;
        Collection<String> repos;
        try {
            String projectId = cna.getProjectId(this.getProject());
            repos = ComboBoxUpdater.ReposUpdater.getRepoList(cna, projectId);
            reposArray = repos.toArray(new String[repos.size()]);
        } catch (RemoteException e) {
            reposArray = new String[0];
        }
        CNHudsonUtil.logoff(cna);
        return reposArray;
    }

    public URL getFileLink(SubversionChangeLogSet.Path path) 
        throws IOException {
        StringBuffer link = getViewerUrlWithPath(path).append("&system=")
            .append(this.getSystemId()).append("&view=markup");
        return new URL(link.toString());
    }

    public URL getDiffLink(SubversionChangeLogSet.Path path) 
        throws IOException {
        int revision = path.getLogEntry().getRevision();
        int r1 = revision - 1;
        int r2 = revision; 
        
        StringBuffer link = getViewerUrlWithPath(path).append("&system=")
            .append(this.getSystemId()).append("&r1=").append(r1)
            .append("&r2=").append(r2);
        return new URL(link.toString());
    }

    private StringBuffer getViewerUrlWithPath(SubversionChangeLogSet.Path path) {
        String[] urlParts = this.getViewerUrl().split("\\?");
        StringBuffer viewWithPath = new StringBuffer(urlParts[0]).append(path.getValue()).append("?");
        if (urlParts.length > 1) {
            viewWithPath.append(urlParts[1]);
        }
        return viewWithPath;
    }

    public URL getChangeSetLink(SubversionChangeLogSet.LogEntry changeSet) 
        throws IOException {
        int revision = changeSet.getRevision();
        StringBuffer link = new StringBuffer(this.getViewerUrl())
            .append("?view=revision&system=")
            .append(this.getSystemId()).append("&revision=").append(revision);
        return new URL(link.toString());
    }

    /**
     * Get the viewer url to display to the user.
     * @return the viewer url
     */
    private String getViewerUrl() {
        CollabNetApp cna = CNHudsonUtil.getCollabNetApp(this.getCollabNetUrl(), 
                                                        this.getUsername(), 
                                                        this.getPassword());
        return CNHudsonUtil.getScmViewerUrl(cna, getCollabNetUrl(), this.getProject(),
                                            this.getRepo());
    }

    private String getSystemId() {
        CollabNetApp cna = CNHudsonUtil.getCollabNetApp(this.getCollabNetUrl(), 
                                                        this.getUsername(), 
                                                        this.getPassword());
        return CNHudsonUtil.getSystemId(cna, this.getProject(),
                                        this.getRepo());
    }

    public static final class DescriptorImpl 
        extends Descriptor<RepositoryBrowser<?>> {

        public DescriptorImpl() {
            super(TeamForge.class);
        }

        public String getDisplayName() {
            return "Collabnet TeamForge";
        }

        /**
         * @return the url that contains the help files.
         */
        public static String getHelpUrl() {
            return "/plugin/collabnet/browser/";
        }

        /**
         * @return a relative url to the main help file.
         */
        @Override
        public String getHelpFile() {
            return getHelpUrl() + "help.html";
        }

        /**
         * @return true if there is auth data that can be inherited.
         */
        public boolean canInheritAuth() {
            return getTeamForgeShareDescriptor().useGlobal();
        }

        /**
         * Form validation for the project field.
         *
         * @param req contains parameters from 
         *            the config.jelly.
         */
        public FormValidation doCheckProject(CollabNetApp app, @QueryParameter String value) {
            return CNFormFieldValidator.projectCheck(app,value);
        }

        /**
         * Form validation for the repo field.
         *
         * @param req contains parameters from 
         *            the config.jelly.
         * @throws ServletException
         */
        public FormValidation doRepoCheck(StaplerRequest req) {
            return CNFormFieldValidator.repoCheck(req);
        }

        /**
         * Gets a list of projects to choose from and write them as a 
         * JSON string into the response data.
         *
         * @param req contains parameters from 
         *            the config.jelly.
         * @param rsp http response data.
         * @throws IOException
         */
        public void doGetProjects(StaplerRequest req, StaplerResponse rsp) 
            throws IOException {
            new ComboBoxUpdater.ProjectsUpdater(req, rsp).update();
        }

        /**
         * Gets a list of repos to choose from and write them as a 
         * JSON string into the response data.
         *
         * @param req contains parameters from 
         *            the config.jelly.
         * @param rsp http response data.
         * @throws IOException
         */
        public void doGetRepos(StaplerRequest req, StaplerResponse rsp) 
            throws IOException {
            new ComboBoxUpdater.ReposUpdater(req, rsp).update();
        }
    }
}
