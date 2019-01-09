package jenkins.plugins.collabnet.security;

import javax.annotation.Nonnull;

import org.jenkinsci.Symbol;

import hudson.Extension;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.model.UserPropertyDescriptor;
import hudson.util.Secret;

public class CnUserSecretProperty extends UserProperty {

    private final Secret secret;

    public CnUserSecretProperty(String secret) {
        this.secret = Secret.fromString(secret);
    }

    public @Nonnull Secret getSecret() {
        return this.secret;
    }

    @Extension
    @Symbol("ctfUserSecret")
    public static final class DescriptorImpl extends UserPropertyDescriptor {
        @Override
        public boolean isEnabled() {
            // does not show elements in /<user>/configure/
            return false;
        }

        @Override
        public UserProperty newInstance(User user) {
            // no default property
            return null;
        }
    }
}
