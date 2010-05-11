package hudson.plugins.collabnet.auth;

import hudson.plugins.collabnet.CNHudsonTestCase;

/**
 * Test CollabNet Authentication.
 *
 * Unlike {@link AuthnTest}, this version doesn't have any setUp/tearDown.
 */
public class Authn2Test extends CNHudsonTestCase {
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
        assertEqualBeans(original, hudson.getSecurityRealm(), FIELDS);
    }

    /**
     * Makes sure that help link exists on all three options.
     */
    public void testHelpLink() throws Exception {
        assertHelpExists(CollabNetSecurityRealm.class, FIELDS);
    }

    private static final String FIELDS = "collabNetUrl,enableSSOAuthFromCTF,enableSSOAuthToCTF";
}
