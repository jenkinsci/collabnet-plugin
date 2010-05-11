package hudson.plugins.collabnet.share;

import hudson.plugins.collabnet.ConnectionFactory;
import hudson.plugins.collabnet.auth.CollabNetSecurityRealm;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 *
 * @author Kohsuke Kawaguchi
 */
public class TeamForgeShareTest extends HudsonTestCase {
    public void testConfigRoudntrip() throws Exception {
        TeamForgeShare.TeamForgeShareDescriptor d = hudson.getDescriptorByType(TeamForgeShare.TeamForgeShareDescriptor.class);

        ConnectionFactory orig = new ConnectionFactory("http://www.google.com/", "abc", "def");
        roundtrip(d, orig);
        assertEqualBeans(d.getConnectionFactory(),orig,FIELDS);

        roundtrip(d, null);
        assertNull(d.getConnectionFactory());
    }

    private void roundtrip(TeamForgeShare.TeamForgeShareDescriptor d, ConnectionFactory orig) throws Exception {
        d.setConnectionFactory(orig);
        submit(createWebClient().goTo("configure").getFormByName("config"));
    }

    /**
     * Makes sure that help link exists on all three options.
     */
    public void testHelpLink() throws Exception {
        assertHelpExists(TeamForgeShare.class, "connectionFactory");
    }

    private static final String FIELDS = "url,username,password";
}
