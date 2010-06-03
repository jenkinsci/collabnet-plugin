package hudson.plugins.collabnet.auth;

/**
 * Test CollabNet Authentication.
 */
public class AuthnTest extends AbstractSecurityTestCase {
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

    /**
     * Test bad login (i.e. a login that should fail).
     */
    public void testCNAuthenticationFailure() throws Exception {
        if (!verifyOnline())    return;

        installSecurityRealm();
        try {
            createWebClient().login("invalid", "");
            fail("Expecting a login failure");
        } catch (com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException fhsce) {
            // we should get a 401 here.
            assertEquals(401,fhsce.getStatusCode());
        }
    }

    /**
     * Test good login (i.e. a login that should succeed).
     */
    public void testCNAuthentication() throws Exception {
        if (!verifyOnline())    return;

        installSecurityRealm();
        createWebClient().login(admin_user, password);
    }

    
    private static final String FIELDS = "collabNetUrl,enableSSOAuthFromCTF,enableSSOAuthToCTF";
}
