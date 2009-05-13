package hudson.plugins.collabnet.share;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Hudson;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;

import hudson.plugins.collabnet.util.CNFormFieldValidator;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import net.sf.json.JSONObject;

/**
 * The TeamForgeShare descriptor holds global data to be shared with
 * other extension points.
 * It's not really a JobProperty, and it'd be neater to define it's own
 * ExtensionPoint class, but the Hudson configure page does not 
 * show global.jelly for arbitrary extension types.
 */
public class TeamForgeShare extends JobProperty<Job<?, ?>> {

    /**
     * {@inheritDoc}
     */
    public TeamForgeShareDescriptor getDescriptor() {
        return (TeamForgeShareDescriptor)Hudson.getInstance().
            getDescriptor(getClass());
    }

    /**
     * Static version of the above getDescriptor method.  The above can't 
     * be static because it's inherited.
     */
    public static TeamForgeShareDescriptor getTeamForgeShareDescriptor() {
        return (TeamForgeShareDescriptor)Hudson.getInstance().
            getDescriptor(TeamForgeShare.class);
    }

    @Extension
    public static final class TeamForgeShareDescriptor 
        extends JobPropertyDescriptor {
        private static Logger log = Logger.getLogger("TeamForgeShareDescriptor");
        private String collabNetUrl = null;
        private String username = null;
        private String password = null;
        private boolean useGlobal = false;
    
        public TeamForgeShareDescriptor() {
            super(TeamForgeShare.class);
            load();
        }

        public String getDisplayName() {
            return "Global CollabNet Teamforge Configuration";
        }

        /**
         * This should never show up in any jobs since it's only for
         * global configuration.
         */
        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return false;
        }
    
        @Override
        public boolean configure(StaplerRequest staplerRequest, JSONObject json) throws FormException {
            if (json.has("useglobal")) {
                this.useGlobal = true;
                JSONObject config = json.getJSONObject("useglobal");
                collabNetUrl = config.getString("collabneturl");
                username = config.getString("username");
                password = config.getString("password");
            } else {
                this.useGlobal = false;
                collabNetUrl = null;
                username = null;
                password = null;
            }
            save();
            return true; 
        }

        public boolean useGlobal() {
            return this.useGlobal;
        }

        public String getCollabNetUrl() {
            return this.collabNetUrl;
        }
        
        public String getUsername() {
            return this.username;
        }
        
        public String getPassword() {
            return this.password;
        }
        
        /**
         * Form validation for the CollabNet URL.
         *
         * @param req StaplerRequest which contains parameters from 
         *            the config.jelly.
         * @param rsp contains http response data.
         * @throws IOException
         * @throws ServletException
         */
        public void doCollabNetUrlCheck(StaplerRequest req, 
                                        StaplerResponse rsp) 
            throws IOException, ServletException {
            new CNFormFieldValidator.SoapUrlCheck(req, rsp).process();
        }

        /**
         * Form validation for username.
         *
         * @param req StaplerRequest which contains parameters from 
         *            the config.jelly.
         * @param rsp contains http response data.
         * @throws IOException
         * @throws ServletException
         */
        public void doRequiredCheck(StaplerRequest req, 
                                    StaplerResponse rsp) 
            throws IOException, ServletException {
            new CNFormFieldValidator.RequiredCheck(req, rsp).process();
        }
        
        /**
         * Check that a password is present and allows login.
         *
         * @param req StaplerRequest which contains parameters from 
         *            the config.jelly.
         * @param rsp contains http response data.
         * @throws IOException
         * @throws ServletException
         */
        public void doPasswordCheck(StaplerRequest req, 
                                    StaplerResponse rsp) 
            throws IOException, ServletException {
            new CNFormFieldValidator.LoginCheck(req, rsp).process();
        }
        
    }
}
