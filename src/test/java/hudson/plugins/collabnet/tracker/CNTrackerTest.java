package hudson.plugins.collabnet.tracker;

import com.collabnet.ce.webservices.CTFArtifact;
import com.collabnet.ce.webservices.CTFPackage;
import com.collabnet.ce.webservices.CTFProject;
import com.collabnet.ce.webservices.CTFRelease;
import com.collabnet.ce.webservices.CollabNetApp;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import hudson.plugins.collabnet.CNHudsonTestCase;
import hudson.plugins.collabnet.TestParam;
import hudson.plugins.collabnet.util.CNHudsonUtil;
import hudson.plugins.collabnet.util.CommonUtil;
import hudson.tasks.Shell;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Tests tracker integration.
 */
public class CNTrackerTest extends CNHudsonTestCase {
    @TestParam
    private String tracker = "testtracker";
    @TestParam
    private int priority = 1;
    @TestParam
    private String title = "Test bug for Hudson! Build id #${BUILD_ID}";
    @TestParam
    private String release = "test release";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (isOnline()) {
            // make sure the tracker exists
            CTFProject p = connect().getProjectByTitle(teamforge_project);
            if (p.getTrackers().byTitle(tracker) ==null)
                p.createTracker(tracker,tracker,"test tracker for Hudson");

            // make sure the admin user is a member of this project
            if (!p.hasMember(admin_user))
                p.addMember(admin_user);

            // make sure a release exists
            ensureReleaseExists(p);
        }
    }

    private void ensureReleaseExists(CTFProject p) throws RemoteException {
        List<CTFPackage> pkgs = p.getPackages();
        for (CTFPackage pkg : pkgs) {
            for (CTFRelease r : pkg.getReleases()) {
                if (r.getTitle().equals(release))
                    return; // yep, we have it.
            }
        }

        // if we don't have a release that matches the name...
        CTFPackage pkg;
        if (pkgs.isEmpty())     pkg = p.createPackage("aPackage","a description",true);
        else                    pkg = pkgs.get(0);

        pkg.createRelease(release,"a test release used by Hudson","active","Prototype");
    }

    public void testConfigRoundtrip() throws Exception {
        setGlobalConnectionFactory();

        roundtripAndAssertIntegrity(new CNTracker(
                createConnectionFactory(),
                "aaa", "bbb", "ccc", "ddd", Priority.P3, true, true, true, "eee"),FIELDS);
        // note that because filePatterns is minimum 1, new FilePattern[0] test would fail

        roundtripAndAssertIntegrity(new CNTracker(
                null,"abc","def","ghi","jkl",Priority.P5,false,false,false,"mno"),FIELDS);
    }

    /**
     * Makes sure that help link exists on all three options.
     */
    public void testHelpLink() throws Exception {
        assertHelpExists(CNTracker.class,FIELDS+",-connectionFactory");
    }

    public void testSuccessfulBuildWithTrackerAlwaysUpdate() throws Exception {
        if (!verifyOnline())    return;
        
        FreeStyleProject p = createProject(true);
        p.getBuildersList().add(new Shell("echo success"));
        AbstractBuild build = buildAndAssertSuccess(p);

        CTFArtifact artifact = this.getArtifact(build);
        assertEquals("Closed", artifact.getStatus());
        this.verifyArtifactValues(artifact);
    }

    public void testSuccessfulBuildWithTracker() throws Exception {
        if (!verifyOnline())    return;

        FreeStyleProject p = createProject(false);
        p.getBuildersList().add(new Shell("echo success"));
        AbstractBuild build = buildAndAssertSuccess(p);

        assertNull(this.getArtifact(build));
    }

    public void testBrokenBuildWithTracker() throws Exception {
        if (!verifyOnline())    return;

        FreeStyleProject p = createProject(true);
        p.getBuildersList().add(new Shell("echo 'Failed Build!'; exit 1"));

        FreeStyleBuild b = p.scheduleBuild2(0).get();

        CTFArtifact artifact = this.getArtifact(b);
        assertNotNull(artifact);
        assertEquals("Open",artifact.getStatus());

        verifyArtifactValues(artifact);
    }

    private FreeStyleProject createProject(boolean alwaysUpdate) throws IOException {
        setGlobalConnectionFactory();
        FreeStyleProject p = createFreeStyleProject();
        p.getPublishersList().add(new CNTracker(null,teamforge_project,tracker,title,admin_user,
                Priority.valueOf(priority),true,alwaysUpdate,true, release));
        return p;
    }

    private CTFArtifact getArtifact(AbstractBuild build) throws Exception {
        String title = CommonUtil.getInterpreted(build.getEnvironment(TaskListener.NULL), this.title);
        CollabNetApp cna = connect();
        List<CTFArtifact> r = cna.getProjectByTitle(teamforge_project).getTrackers().byTitle(tracker).getArtifactsByTitle(title);
        Collections.sort(r, new Comparator<CTFArtifact>() {
            public int compare(CTFArtifact o1, CTFArtifact o2) {
                return o2.getLastModifiedDate().compareTo(o1.getLastModifiedDate());
            }
        });
        if (r.isEmpty())    return null;
        return r.get(0);
    }

    public void verifyArtifactValues(CTFArtifact artifact) throws RemoteException {
        artifact.refill();
        assertEquals(artifact.getPriority(),priority);
        assertEquals(artifact.getAssignedTo(),admin_user);
        assertEquals(artifact.getReportedReleaseId(),this.getRelease().getId());
    }

    public CTFRelease getRelease() throws RemoteException {
        CollabNetApp cna = connect();
        return CNHudsonUtil.getProjectReleaseId(cna.getProjectByTitle(teamforge_project), release);
    }

    private static final String FIELDS = "connectionFactory,project,tracker,title,assignUser,priority,attachLog,alwaysUpdate,closeOnSuccess,release";
}
