package hudson.plugins.collabnet.auth;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.plugins.collabnet.CNHudsonTestCase;
import hudson.security.ACL;
import hudson.security.AuthorizationStrategy;
import hudson.security.LegacyAuthorizationStrategy;
import hudson.security.SparseACL;

/**
 * Base class for test cases.
 *
 * Unlike {@link AuthzTest}, this version doesn't have any setUp/tearDown.
 *
 * @author Kohsuke Kawaguchi
 */
public class Authz2Test extends CNHudsonTestCase {
    /**
     * Verifies that the UI is bound correctly to properties
     */
    public void testConfigRoundtrip() throws Exception {
        hudson.setAuthorizationStrategy(new LegacyAuthorizationStrategy());
        roundtripAndAssert(new CNAuthorizationStrategy("foo,bar","dev,op","alice,boss,root","god,budda",35) {
            /**
             * Allow resubmission of the system config without logging in first.
             * @return
             */
            @Override
            public ACL getRootACL() {
                SparseACL acl = new SparseACL(null);
                acl.add(ACL.ANONYMOUS, Hudson.ADMINISTER, true);
                return acl;
            }

            @Override
            public Descriptor<AuthorizationStrategy> getDescriptor() {
                return Hudson.getInstance().getDescriptorOrDie(CNAuthorizationStrategy.class);
            }
        });

    }

    private void roundtripAndAssert(CNAuthorizationStrategy original) throws Exception {
        hudson.setAuthorizationStrategy(original);
        try {
            submit(createWebClient().goTo("configure").getFormByName("config"));
            fail(); // submission would succeed but the rendering of the top page would fail, so this should result in an error
        } catch (FailingHttpStatusCodeException e) {
            // if the submission succeeds, we should see a new instance
            assertNotSame(original,hudson.getAuthorizationStrategy());
            assertEqualBeans(original, hudson.getAuthorizationStrategy(), FIELDS);
        }
    }

    /**
     * Makes sure that help link exists on all three options.
     */
    public void testHelpLink() throws Exception {
        assertHelpExists(CNAuthorizationStrategy.class, FIELDS);
    }

    private static final String FIELDS = "readUsersStr,readGroupsStr,adminUsersStr,adminGroupsStr,authCacheTimeoutMin";
}
