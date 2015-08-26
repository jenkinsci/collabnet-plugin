package hudson.plugins.collabnet.auth;

import hudson.plugins.collabnet.util.Util;
import hudson.security.AuthorizationStrategy;

/**
 * Base class for test cases.
 *
 * Unlike {@link AuthzTest}, this version doesn't have any setUp/tearDown.
 *
 * @author Kohsuke Kawaguchi
 */
public class AuthzTest extends AbstractSecurityTestCase {

    /**
     * Verifies that the UI is bound correctly to properties
     */
    public void testConfigRoundtrip() throws Exception {
        hudson.setAuthorizationStrategy(new AuthorizationStrategy.Unsecured());
        submit(createWebClient().goTo("configure").getFormByName("config"));
        roundtripAndAssert(new MockCNAuthorizationStrategy("foo,bar","dev,op","alice,boss,root","god,budda",35));
    }

    private void roundtripAndAssert(CNAuthorizationStrategy original) throws Exception {
        hudson.setAuthorizationStrategy(original);
        submit(createWebClient().goTo("configure").getFormByName("config"));
        assertEqualBeans(original, hudson.getAuthorizationStrategy(), FIELDS);
    }

    /**
     * Makes sure that help link exists on all three options.
     */
    public void testHelpLink() throws Exception {
        assertHelpExists(CNAuthorizationStrategy.class, FIELDS);
    }

    /**
     * Test that the admin user can log in and get to the configure page.
     */
    public void testAdminUserAccess() throws Exception {
        if (!verifyOnline())    return;
        installAuthorizationStrategy();
        // admin user should be able to see the system config page
        createAdminWebClient().goTo("configure");
    }

    /**
     * Test that the admin group can log in and get to the configure page.
     */
    public void testAdminGroupAccess() throws Exception {
        if (!verifyOnline())    return;
        installAuthorizationStrategy();
        new WebClient().login(admin_group_member, admin_group_member).goTo("configure");
    }

    /**
     * Test that the read user can log in and not get to the configure page.
     */
    public void testReadUserAccess() throws Exception {
        if (!verifyOnline())    return;
        installAuthorizationStrategy();
        WebClient loggedInRead = new WebClient().login(read_user,read_user);
        Util.checkPageUnreachable(loggedInRead, "configure");
    }

    /**
     * Test that the read group can log in and not get to the configure page.
     */
    public void testReadGroupAccess() throws Exception {
        if (!verifyOnline())    return;
        installAuthorizationStrategy();
        WebClient loggedInRead = new WebClient().login(read_group_member,read_group_member);
        Util.checkPageUnreachable(loggedInRead,"configure");
    }

    private static final String FIELDS = "readUsersStr,readGroupsStr,adminUsersStr,adminGroupsStr,authCacheTimeoutMin";
}
