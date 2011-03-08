package hudson.plugins.collabnet;

import com.collabnet.ce.webservices.CollabNetApp;
import com.collabnet.ce.webservices.TrustAllSocketFactory;
import hudson.model.FreeStyleProject;
import hudson.plugins.collabnet.share.TeamForgeShare;
import hudson.tasks.Publisher;
import org.jvnet.hudson.test.HudsonTestCase;

import java.rmi.RemoteException;

/**
 * Base class for test cases.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class CNHudsonTestCase extends HudsonTestCase {
    @TestParam
    protected String teamforge_url;
    // this user needs access to the project and access to the projects
    // document creation/view
    @TestParam
    protected String admin_user;
    @TestParam
    protected String password;
    @TestParam
    protected String teamforge_project;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Configuration.INSTANCE.injectTo(this);

        if (isOnline()) {
            if (teamforge_project == null || teamforge_project.length() == 0) {
                fail("teamforge_project is not set");
            }
            CollabNetApp app = connect();
            if (app.getProjectByTitle(teamforge_project)==null) {
                app.createProject(teamforge_project, teamforge_project,"Test bed for the collab.net Jenkins plugin");
            }
        }
    }

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
        return teamforge_url !=null;
    }

    /**
     * Used to guard the tests that need to work with a live TeamForge instance.
     * This method fails the test if we are offline and the user didn't specify the offline mode.
     * Otherwise this method returns false when the caller should skip the test.
     */
    protected boolean verifyOnline() {
        if (System.getProperty("offline")!=null)
            return isOnline();

        assertTrue("This test requires a live TeamForge instance. Use -Doffline to skip this test",isOnline());
        return true;
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
        if (isOnline())
            return new ConnectionFactory(teamforge_url, admin_user, password);
        else
            return new ConnectionFactory("http://www.google.com/", "abc", "def");
    }

    protected CollabNetApp connect() throws RemoteException {
        TrustAllSocketFactory.install();
        return new CollabNetApp(teamforge_url, admin_user, password);
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
