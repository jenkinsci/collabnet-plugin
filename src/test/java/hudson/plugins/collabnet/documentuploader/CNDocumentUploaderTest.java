package hudson.plugins.collabnet.documentuploader;

import com.collabnet.ce.webservices.CollabNetApp;
import com.collabnet.ce.webservices.DocumentApp;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import hudson.plugins.collabnet.CNHudsonTestCase;
import hudson.plugins.collabnet.ConnectionFactory;
import hudson.plugins.collabnet.TestParam;
import hudson.plugins.collabnet.util.CommonUtil;
import org.jvnet.hudson.test.TestBuilder;

import java.io.IOException;

/**
 * Test document upload facility.
 */
public class CNDocumentUploaderTest extends CNHudsonTestCase {
    @TestParam
    protected String doc_path = "upload/test";

    /**
     * Setup the doc upload, run a build, check that the build log
     * is really on the CN server.
     */
    public void testDocUpload() throws Exception {
        if(!isOnline()) return; // skip if offline

        // a test project that creates './abc' and uploads that file.
        FreeStyleProject job = this.createFreeStyleProject();
        job.getBuildersList().add(new TestBuilder() {
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("abc").touch(0);
                return true;
            }
        });
        job.getPublishersList().add(new CNDocumentUploader(
                new ConnectionFactory(teamforge_url, admin_user, password),
                teamforge_project, doc_path, "uploaded from hudson build #${BUILD_NUMBER}",
                new FilePattern[] {new FilePattern("./abc")}, true
        ));

        this.verifyDocUploaded(buildAndAssertSuccess(job));
    }

    /**
     * Attempt to find the uploaded doc on the CN server.
     */
    private void verifyDocUploaded(AbstractBuild build) throws Exception {
        CollabNetApp cna = connect();
        String projectId = cna.getProjectId(teamforge_project);
        assert(projectId != null);
        DocumentApp da = new DocumentApp(cna);
        String folderId = da.
            findOrCreatePath(projectId, CommonUtil.
                             getInterpreted(build.getEnvironment(TaskListener.NULL), doc_path));
        assert(folderId != null);
        String docId = da.findDocumentId(folderId, "log");
        assert(docId != null);

        // verify that the variable expansion worked
        assertEquals("uploaded from hudson build #1",da.getDocument(docId).getDescription());
    }

    /**
     * Verifies that the configuration round trip successfully.
     */
    public void testConfigRoundtrip() throws Exception {
        setGlobalConnectionFactory();

        roundtripAndAssertIntegrity(new CNDocumentUploader(
                createConnectionFactory(),
                "project", "path", "description", new FilePattern[]{new FilePattern("ddd")}, true),FIELDS);
        // note that because filePatterns is minimum 1, new FilePattern[0] test would fail

        roundtripAndAssertIntegrity(new CNDocumentUploader(
                null, "project", "path", "description",
                new FilePattern[]{new FilePattern("xxx"),new FilePattern("yyy")}, false),FIELDS);
    }

    /**
     * Makes sure that help link exists on all three options.
     */
    public void testHelpLink() throws Exception {
        assertHelpExists(CNDocumentUploader.class,FIELDS+",-connectionFactory");
    }

    private static final String FIELDS = "connectionFactory,project,uploadPath,description,filePatterns,includeBuildLog";
}
