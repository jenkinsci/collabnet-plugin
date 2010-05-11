package hudson.plugins.collabnet;

import hudson.model.Describable;
import hudson.model.Descriptor;
import org.jvnet.hudson.test.HudsonTestCase;

import java.util.concurrent.Callable;

/**
 * Base class for test cases.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class CNHudsonTestCase extends HudsonTestCase {
    /**
     * Asserts that help files exist for the specified properties of the given instance.
     *
     * TODO: once the plugin rebases to Hudson 1.355, this code will be no longer necessary.
     *
     * @param type
     *      The describable class type that should have the associated help files.
     * @param properties
     *      ','-separated list of properties whose help files should exist.
     */
    public void assertHelpExists(final Class<? extends Describable> type, final String properties) throws Exception {
        executeOnServer(new Callable<Object>() {
            public Object call() throws Exception {
                Descriptor d = hudson.getDescriptor(type);
                WebClient wc = createWebClient();
                for (String property : properties.split(",")) {
                    String url = d.getHelpFile(property);
                    assertNotNull("Help file for the property "+property+" is missing on "+type, url);
                    wc.goTo(url); // make sure it successfully loads
                }
                return null;
            }
        });
    }

}
