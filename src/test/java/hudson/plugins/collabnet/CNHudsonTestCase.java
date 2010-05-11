package hudson.plugins.collabnet;

import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.plugins.collabnet.share.TeamForgeShare;
import hudson.plugins.collabnet.tracker.CNTracker;
import hudson.tasks.Publisher;
import org.jvnet.hudson.test.HudsonTestCase;

import java.util.concurrent.Callable;

/**
 * Base class for test cases.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class CNHudsonTestCase extends HudsonTestCase {
    /**
     * Setting a global value would enable job configuration to choose the override or delegate to the default.
     */
    protected void setGlobalConnectionFactory() {
        TeamForgeShare.getTeamForgeShareDescriptor().setConnectionFactory(createConnectionFactory());
    }

    /**
     * Create some non-null instance of {@link ConnectionFactory}
     */
    protected ConnectionFactory createConnectionFactory() {
        return new ConnectionFactory("http://www.google.com/", "abc", "def");
    }

    /**
     * Roundtrips a publisher object via configuration and make sure they are still intact.
     */
    protected <T extends Publisher> void roundtripAndAssertIntegrity(T before, String fields) throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getPublishersList().add(before);
        submit(createWebClient().getPage(p,"configure").getFormByName("config"));
        T after = (T)p.getPublishersList().get(before.getClass());
        assertEqualBeans(before,after,fields);
    }

}
