package hudson.plugins.collabnet.share;

import hudson.Extension;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;

import hudson.plugins.collabnet.util.CNFormFieldValidator;

import hudson.plugins.collabnet.util.CNHudsonUtil;
import hudson.util.FormValidation;
import java.util.logging.Logger;

import hudson.util.Secret;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

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
    @Override
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
        private Secret password = null;
        private boolean useGlobal = false;
    
        public TeamForgeShareDescriptor() {
            super(TeamForgeShare.class);
            load();
        }

        @Override
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
                collabNetUrl = CNHudsonUtil.sanitizeCollabNetUrl(config.getString("collabneturl"));
                username = config.getString("username");
                password = Secret.fromString(config.getString("password"));
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
            return this.password==null ? null : this.password.toString();
        }
    }
}
