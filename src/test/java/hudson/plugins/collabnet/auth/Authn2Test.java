package hudson.plugins.collabnet.auth;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.security.SecurityRealm;
import org.jvnet.hudson.test.HudsonTestCase;

import java.util.concurrent.Callable;

/**
 * Test CollabNet Authentication.
 *
 * Unlike {@link AuthnTest}, this version doesn't have any setUp/tearDown.
 */
public class Authn2Test extends HudsonTestCase {
    /**
     * Verifies that the UI is bound correctly to properties
     */
    public void testConfigRoundtrip() throws Exception {
        roundtripAndAssert(new CollabNetSecurityRealm("http://www.google.com/", true, true));
        roundtripAndAssert(new CollabNetSecurityRealm("http://www.collab.net/", false, false));
    }

    private void roundtripAndAssert(CollabNetSecurityRealm original) throws Exception {
        hudson.setSecurityRealm(original);
        submit(createWebClient().goTo("configure").getFormByName("config"));
        assertEqualBeans(original, hudson.getSecurityRealm(), "collabNetUrl,enableSSOAuthFromCTF,enableSSOAuthToCTF");
    }

    /**
     * Makes sure that help link exists on all three options.
     */
    public void testHelpLink() throws Exception {
        assertHelpExists(CollabNetSecurityRealm.class,"collabNetUrl,enableSSOAuthFromCTF,enableSSOAuthToCTF");
    }

    /**
     * Asserts that help files exist for the specified properties of the given instance.
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
