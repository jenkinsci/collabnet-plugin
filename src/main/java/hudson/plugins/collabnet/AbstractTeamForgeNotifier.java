package hudson.plugins.collabnet;

import com.collabnet.ce.webservices.CollabNetApp;
import hudson.model.AbstractProject;
import hudson.plugins.collabnet.share.TeamForgeShare;
import hudson.plugins.collabnet.util.CNFormFieldValidator;
import hudson.plugins.collabnet.util.CNHudsonUtil;
import hudson.plugins.collabnet.util.ComboBoxUpdater;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;
import hudson.util.Secret;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.rmi.RemoteException;

/**
 * Base class for {@link Notifier}s that talk to CollabNet TeamForge.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractTeamForgeNotifier extends Notifier {
    // Variables from the form
    private boolean override_auth = true;
    private String url;
    private String username;
    private Secret password;
    private String project;

    public AbstractTeamForgeNotifier(ConnectionFactory connectionFactory, String project) {
        this.override_auth = connectionFactory!=null;
        if (override_auth) {// if this is null, it means we should be using globally configured one
            // for the compatibility reasons, these 3 params are stored directly whereas ideally we could have just
            // stored ConnectionFactory.
            this.url = connectionFactory.getUrl();
            this.username = connectionFactory.getUsername();
            this.password = connectionFactory.getPassword();
        }
        this.project = project;
    }

    /**
     * If this notifier is configured with a separate credential, return it. Otherwise null.
     * Used for form data binding.
     */
    public ConnectionFactory getConnectionFactory() {
        return override_auth ? new ConnectionFactory(url,username,password) : null;
    }

    /**
     * @return whether or not auth is overriden
     */
    public boolean overrideAuth() {
        return this.override_auth;
    }

    /**
     * @return the url for the CollabNet server.
     */
    public String getCollabNetUrl() {
        if (this.overrideAuth()) {
            return this.url;
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

    /**
     * Connects to the TeamForge.
     */
    public CollabNetApp connect() {
        return CNHudsonUtil.getCollabNetApp(getCollabNetUrl(),getUsername(),getPassword());
    }

    /**
     * @return the project where the build log is uploaded.
     */
    public String getProject() {
        return this.project;
    }

    public static abstract class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        /**
         * Implementation of the abstract isApplicable method from
         * BuildStepDescriptor.
         */
        @Override
         public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
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
         * @throws IOException
         * @throws ServletException
         */
        public FormValidation doCheckProject(CollabNetApp app, @QueryParameter String value) throws RemoteException {
            return CNFormFieldValidator.projectCheck(app,value);
        }

        /**
         * @return the list of all possible projects, given the login data.
         */
        public ComboBoxModel doFillProjectItems(CollabNetApp cna) {
            ComboBoxModel projects = ComboBoxUpdater.getProjectList(cna);
            CNHudsonUtil.logoff(cna);
            return projects;
        }
    }

    /**
     * @return the TeamForge share descriptor.
     */
    public static TeamForgeShare.TeamForgeShareDescriptor getTeamForgeShareDescriptor() {
        return TeamForgeShare.getTeamForgeShareDescriptor();
    }
}
