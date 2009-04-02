package hudson.plugins.collabnet.auth;

import hudson.plugins.collabnet.CollabNetPlugin;

import hudson.model.Jobs;
import hudson.security.AuthorizationStrategy;
import hudson.security.SecurityRealm;
import hudson.util.PluginServletFilter;

/**
 * Entry point of the auth plugin.
 */
public class AuthPlugin extends CollabNetPlugin {
    public void start() throws Exception {
        SecurityRealm.LIST.add(CollabNetSecurityRealm.DESCRIPTOR);
        PluginServletFilter.addFilter(new CNFilter());
        AuthorizationStrategy.LIST.add(CNAuthorizationStrategy.DESCRIPTOR);
        Jobs.PROPERTIES.add(CNAuthProjectProperty.DESCRIPTOR);
    }
}
