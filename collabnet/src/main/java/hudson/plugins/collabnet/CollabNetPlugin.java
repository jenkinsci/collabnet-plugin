package hudson.plugins.collabnet;

import hudson.plugins.collabnet.auth.CNFilter;

import hudson.Plugin;
import hudson.util.PluginServletFilter;

/**
 * Entry point for the plugins.  Initializes each sub-plugin.
 */
public class CollabNetPlugin extends Plugin {
    @Override
    public void start() throws Exception {
        PluginServletFilter.addFilter(new CNFilter());
        super.start();
    }
}
