package hudson.plugins.collabnet.auth;

import org.jvnet.hudson.test.HudsonTestCase;

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
        CollabNetSecurityRealm original = new CollabNetSecurityRealm("http://www.google.com/", true, true);
        hudson.setSecurityRealm(original);
        submit(createWebClient().goTo("configure").getFormByName("config"));
        assertEqualBeans(original, hudson.getSecurityRealm(), "collabNetUrl,enableSSOAuthFromCTF,enableSSOAuthToCTF");
    }
}
