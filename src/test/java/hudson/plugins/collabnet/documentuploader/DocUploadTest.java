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
import hudson.plugins.collabnet.util.CommonUtil;
import org.jvnet.hudson.test.TestBuilder;

import java.io.IOException;

public class DocUploadTest extends CNHudsonTestCase {
    private static final String TEST_PATH = System.getProperty("doc_path");

    /**
     * Setup the doc upload, run a build, check that the build log
     * is really on the CN server.
     */
    public void testDocUpload() throws Exception {
        if(!isOnline()) return; // skip if offline

        FreeStyleProject job = this.createFreeStyleProject();
        // create './abc'
        job.getBuildersList().add(new TestBuilder() {
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("abc").touch(0);
                return true;
            }
        });
        job.getPublishersList().add(new CNDocumentUploader(
                new ConnectionFactory(CN_URL,TEST_USER,TEST_PW),
                CN_PROJECT_NAME, TEST_PATH, "uploaded from hudson build #${BUILD_NUMBER}",
                new FilePattern[] {new FilePattern("./abc")}, true
        ));

        this.verifyDocUploaded(buildAndAssertSuccess(job));
    }

    /**
     * Attempt to find the uploaded doc on the CN server. 
     */
    private void verifyDocUploaded(AbstractBuild build) throws Exception {
        CollabNetApp cna = new CollabNetApp(CN_URL, TEST_USER, TEST_PW);
        String projectId = cna.getProjectId(CN_PROJECT_NAME);
        assert(projectId != null);
        DocumentApp da = new DocumentApp(cna);
        String folderId = da.
            findOrCreatePath(projectId, CommonUtil.
                             getInterpreted(build.getEnvironment(TaskListener.NULL), TEST_PATH));
        assert(folderId != null);
        String docId = da.findDocumentId(folderId, "log");
        assert(docId != null);
    }

}
