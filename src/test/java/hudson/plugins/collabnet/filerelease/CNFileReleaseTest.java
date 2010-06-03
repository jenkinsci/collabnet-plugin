package hudson.plugins.collabnet.filerelease;

import com.collabnet.ce.webservices.CollabNetApp;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.plugins.collabnet.CNHudsonTestCase;
import hudson.plugins.collabnet.ConnectionFactory;
import hudson.plugins.collabnet.documentuploader.FilePattern;
import hudson.plugins.collabnet.util.CNHudsonUtil;
import org.jvnet.hudson.test.TestBuilder;

import java.io.IOException;

/**
 * Test document upload facility.
 */
public class CNFileReleaseTest extends CNHudsonTestCase {
    private static final String PACKAGE = System.getProperty("fr_package");
    private static final String RELEASE = System.getProperty("fr_release");
    private static final String FILE = "test.txt";
    private static final String FILE_CONTENT = "Test file from FileRelease";

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
        if(!isOnline()) return; // skip if offline

        FreeStyleProject job = this.createFreeStyleProject();
        job.getPublishersList().add(new CNFileRelease(
                new ConnectionFactory(CN_URL,TEST_USER,TEST_PW),
                CN_PROJECT_NAME, PACKAGE, RELEASE, true,
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
        this.verifyFRUpload();
    }

    /**
     * Verify that an upload of the test file was successful.
     */
    public void verifyFRUpload() {
        CollabNetApp cna = CNHudsonUtil.getCollabNetApp(CN_URL, TEST_USER,
                                                        TEST_PW);
        assert(cna != null);
        String fileId = CNHudsonUtil.getFileId(cna, CN_PROJECT_NAME, PACKAGE,
                                               RELEASE, FILE);
        assert(fileId != null);
    }

    private static final String FIELDS = "connectionFactory,project,pkg,release,filePatterns,overwrite";
}
