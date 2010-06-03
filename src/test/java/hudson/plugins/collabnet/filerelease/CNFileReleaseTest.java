package hudson.plugins.collabnet.filerelease;

import com.collabnet.ce.webservices.CTFPackage;
import com.collabnet.ce.webservices.CTFProject;
import com.collabnet.ce.webservices.CTFRelease;
import com.collabnet.ce.webservices.CTFReleaseFile;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.plugins.collabnet.CNHudsonTestCase;
import hudson.plugins.collabnet.ConnectionFactory;
import hudson.plugins.collabnet.TestParam;
import hudson.plugins.collabnet.documentuploader.FilePattern;
import org.jvnet.hudson.test.TestBuilder;

import java.io.IOException;
import java.rmi.RemoteException;

/**
 * Test document upload facility.
 */
public class CNFileReleaseTest extends CNHudsonTestCase {
    @TestParam
    private String fr_package = "aPackage";
    @TestParam
    private String fr_release = "aRelease";
    @TestParam
    private String FILE = "test.txt";
    @TestParam
    private String FILE_CONTENT = "Test file from FileRelease";

    public void testConfigRoundtrip() throws Exception {
        setGlobalConnectionFactory();

        roundtripAndAssertIntegrity(new CNFileRelease(
                createConnectionFactory(),
                "aaa", "bbb", "ccc", true, new FilePattern[]{new FilePattern("ddd")}),FIELDS);
        // note that because filePatterns is minimum 1, new FilePattern[0] test would fail

        roundtripAndAssertIntegrity(new CNFileRelease(
                null,
                "xxx", "yyy", "zzz", false, new FilePattern[]{new FilePattern("111"),new FilePattern("222")}),FIELDS);
    }

    /**
     * Makes sure that help link exists on all three options.
     */
    public void testHelpLink() throws Exception {
        assertHelpExists(CNFileRelease.class,FIELDS+",-connectionFactory");
    }

    /**
     * Test FileRelease upload by building and uploading a test file.
     */
    public void testUpload() throws Exception {
        if(!verifyOnline()) return; // skip if offline

        // make sure package and release exists
        CTFProject fr = connect().getProjectByTitle(teamforge_project);
        CTFPackage pkg = fr.getPackages().byTitle(fr_package);
        if (pkg==null)
            pkg = fr.createPackage(fr_package,"test for Hudson",true);
        CTFRelease r = pkg.getReleaseByTitle(fr_release);
        if (r==null)
            r = pkg.createRelease(fr_release,"test for Hudson","active","Prototype");

        FreeStyleProject job = this.createFreeStyleProject();
        job.getPublishersList().add(new CNFileRelease(
                new ConnectionFactory(teamforge_url, admin_user, password),
                teamforge_project, fr_package, fr_release, true,
                new FilePattern[]{new FilePattern(FILE)}
        ));
        job.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child(FILE).write(FILE_CONTENT,"UTF-8");
                return true;
            }
        });
        buildAndAssertSuccess(job);
        this.verifyFRUpload(r);
    }

    /**
     * Verify that an upload of the test file was successful.
     */
    public void verifyFRUpload(CTFRelease r) throws RemoteException {
        CTFReleaseFile f = r.getFileByTitle(FILE);
        assertTrue(f.getId()!=null);
    }

    private static final String FIELDS = "connectionFactory,project,pkg,release,filePatterns,overwrite";
}
