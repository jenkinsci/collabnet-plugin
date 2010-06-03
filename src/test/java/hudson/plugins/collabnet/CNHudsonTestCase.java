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
    protected final String CN_URL = System.getProperty("teamforge_url");
    // this user needs access to the project and access to the projects
    // document creation/view
    protected final String TEST_USER = System.getProperty("admin_user");
    protected final String TEST_PW = System.getProperty("password");
    protected final String CN_PROJECT_NAME = System.getProperty("teamforge_project");

    /**
     * Some of the test requires a working TeamForge instance to send a request to.
     * We call such tests "online tests." Other tests that can be run anywhere are called
     * offline tests.
     *
     * <p>
     * This method returns true if the current test environment is online. Online tests
     * should use this flag to decide if the test should be skipped or not.
     */
    protected boolean isOnline() {
        return CN_URL!=null;
    }

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
