package hudson.plugins.collabnet;

import com.collabnet.ce.webservices.CollabNetApp;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.plugins.collabnet.util.CNFormFieldValidator;
import hudson.plugins.collabnet.util.CNHudsonUtil;
import hudson.util.FormValidation;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * Represents the information about the connectivity to TeamForge.
 *
 * @author Kohsuke Kawaguchi
 */
public class ConnectionFactory extends AbstractDescribableImpl<ConnectionFactory> {
    private final String url;
    private final String username;
    private final Secret password;

    @DataBoundConstructor
    public ConnectionFactory(String url, String username, String password) {
        this(url,username,Secret.fromString(password));
    }

    public ConnectionFactory(String url, String username, Secret password) {
        this.url = CNHudsonUtil.sanitizeCollabNetUrl(url);
        this.username = username;
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public Secret getPassword() {
        return password;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<ConnectionFactory> {
        public String getDisplayName() { return ""; }

        /**
         * Form validation for the CollabNet URL.
         *
         * @param value url
         */
        public FormValidation doCheckUrl(@QueryParameter String value) {
            return CNFormFieldValidator.soapUrlCheck(value);
        }

        /**
         * Form validation for username.
         *
         * @param value to check
         */
        public FormValidation doCheckUsermame(@QueryParameter String value) {
            return CNFormFieldValidator.requiredCheck(value, "user name");
        }

        /**
         * Check that a password is present and allows login.
         */
        public FormValidation doCheckPassword(CollabNetApp app, @QueryParameter String value) {
            return CNFormFieldValidator.loginCheck(app,value);
        }
    }
}
