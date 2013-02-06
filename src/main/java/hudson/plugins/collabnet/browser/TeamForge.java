package hudson.plugins.collabnet.browser;

import com.collabnet.ce.webservices.CollabNetApp;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.plugins.collabnet.ConnectionFactory;
import hudson.plugins.collabnet.share.TeamForgeShare;
import hudson.plugins.collabnet.util.CNFormFieldValidator;
import hudson.plugins.collabnet.util.CNHudsonUtil;
import hudson.plugins.collabnet.util.ComboBoxUpdater;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SubversionChangeLogSet;
import hudson.scm.SubversionRepositoryBrowser;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.logging.Logger;

public class TeamForge extends SubversionRepositoryBrowser {
    private static Logger log = Logger.getLogger("TeamForge");

    private String collabneturl;
    private String username;
    private Secret password;
    private String project;
    private String repo;
    private boolean overrideAuth;

    /**
     * DataBoundConstructor for building the object from form data.
     */
    @DataBoundConstructor
    public TeamForge(ConnectionFactory connectionFactory, String project, String repo)
    { 
        if (connectionFactory != null) {
            this.overrideAuth = true;
            this.collabneturl = connectionFactory.getUrl();
            this.username = connectionFactory.getUsername();
            this.password = connectionFactory.getPassword();
        } else {
            this.overrideAuth = false;
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
            return Secret.toString(this.password);
        } else {
            return getTeamForgeShareDescriptor().getPassword();
        }
    }

    public ConnectionFactory getConnectionFactory() {
        if (this.overrideAuth())
            return new ConnectionFactory(getCollabNetUrl(),getUsername(),getPassword());
        return null;
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
        return TeamForgeShare.getTeamForgeShareDescriptor();
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

    private StringBuffer getViewerUrlWithPath(SubversionChangeLogSet.Path path) throws RemoteException {
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
    private String getViewerUrl() throws RemoteException {
        CollabNetApp cna = CNHudsonUtil.getCollabNetApp(this.getCollabNetUrl(), 
                                                        this.getUsername(), 
                                                        this.getPassword());
        return CNHudsonUtil.getScmViewerUrl(cna, getCollabNetUrl(), this.getProject(),
                                            this.getRepo());
    }

    private String getSystemId() throws RemoteException {
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
         */
        public FormValidation doCheckProject(CollabNetApp app, @QueryParameter String value) throws RemoteException {
            return CNFormFieldValidator.projectCheck(app,value);
        }

        /**
         * Form validation for the repo field.
         *
         * @param req contains parameters from 
         *            the config.jelly.
         * @throws ServletException
         */
        public FormValidation doCheckRepo(StaplerRequest req) throws RemoteException {
            return CNFormFieldValidator.repoCheck(req);
        }

        /**
         * Gets a list of projects to choose from and write them as a 
         * JSON string into the response data.
         */
        public ComboBoxModel doFillProjectItems(CollabNetApp cna) throws IOException {
            ComboBoxModel projects = ComboBoxUpdater.getProjectList(cna);
            CNHudsonUtil.logoff(cna);
            return projects;
        }

        /**
         * Gets a list of repos to choose from and write them as a 
         * JSON string into the response data.
         */
        public ComboBoxModel doFillRepoItems(CollabNetApp cna, @QueryParameter String project) throws RemoteException {
            ComboBoxModel repos = ComboBoxUpdater.getRepos(cna,project);
            CNHudsonUtil.logoff(cna);
            return repos;
        }
    }
}
