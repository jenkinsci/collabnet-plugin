package hudson.plugins.collabnet.documentuploader;

import com.collabnet.ce.webservices.CollabNetApp;
import com.collabnet.ce.webservices.DocumentApp;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause;

import hudson.plugins.collabnet.util.HudsonConstants;
import hudson.plugins.collabnet.util.Util;

import hudson.plugins.collabnet.util.CommonUtil;

import java.util.List;

import org.jvnet.hudson.test.HudsonTestCase;

public class DocUploadTest extends HudsonTestCase {
    private static final String DOC_UPLOADER_LABEL = "CollabNet Document " +
        "Uploader";
    private static final String URL_ID = "cndu.collabneturl";
    private static final String USER_ID = "cndu.username";
    private static final String PASSWORD_ID = "cndu.password";
    private static final String PROJECT_ID = "cndu_project";
    private static final String PATH_ID = "cndu.upload_path";
    private static final String DESC_ID = "cndu.description";
    private static final String FILE_NAME = "cndu.file";
    private static final String BUILDLOG_NAME = "cndu.buildlog";

    private static final String CN_URL = System.getProperty("teamforge_url");
    // this user needs access to the project and access to the projects
    // document creation/view
    private static final String TEST_USER = System.getProperty("admin_user");
    private static final String TEST_PW = System.getProperty("password");
    private static final String CN_PROJECT_NAME = 
        System.getProperty("teamforge_project");
    private static final String TEST_PATH = System.getProperty("doc_path");
    private static final String TEST_DESC = System.getProperty("doc_desc");
    private static final String TEST_FILE_PATTERN = 
        System.getProperty("doc_file_pattern");
    private static final String UPLOAD_BUILD_LOG = "true";

    private AbstractProject job = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        DocumentUploaderPlugin doc = new DocumentUploaderPlugin();
        doc.start();
        this.job = this.createFreeStyleProject();
        HtmlPage configurePage = setupProjectForDocUpload();
        this.submitForm(configurePage, HudsonConstants.CONFIGURE_FORM_NAME);
    }

    /**
     * Setup the document upload in the job configure page, then
     * check to make sure all the settings remain after submitting the
     * form.
     */
    public void testDocUploadSetup() throws Exception {
        WebClient wc = new WebClient();
        HtmlPage configurePage = (HtmlPage) wc.goTo(this.job.getShortUrl() 
                                                    + "configure");
        Util.checkText(configurePage, URL_ID, CN_URL);
        Util.checkText(configurePage, USER_ID, TEST_USER);
        Util.checkPassword(configurePage, PASSWORD_ID, TEST_PW);
        Util.checkText(configurePage, PROJECT_ID, CN_PROJECT_NAME);
        Util.checkText(configurePage, PATH_ID, TEST_PATH);
        Util.checkText(configurePage, DESC_ID, TEST_DESC);
        Util.checkTextByName(configurePage, FILE_NAME, TEST_FILE_PATTERN);
        Util.checkRadioSelected(configurePage, BUILDLOG_NAME, 
                                UPLOAD_BUILD_LOG);
    }

    /**
     * Setup the doc upload, run a build, check that the build log
     * is really on the CN server.
     */
    public void testDocUpload() throws Exception {
        this.setupProjectForDocUpload();
        // This will do a build and block until the build is done
        AbstractBuild build = (AbstractBuild) this.job.scheduleBuild2(0).get();
        this.verifyDocUploaded(build);
    }

    /**
     * Attempt to find the uploaded doc on the CN server. 
     */
    public void verifyDocUploaded(AbstractBuild build) throws Exception {
        CollabNetApp cna = new CollabNetApp(CN_URL, TEST_USER, TEST_PW);
        String projectId = cna.getProjectId(CN_PROJECT_NAME);
        assert(projectId != null);
        DocumentApp da = new DocumentApp(cna);
        String folderId = da.
            findOrCreatePath(projectId, CommonUtil.
                             getInterpreted(build.getEnvVars(), TEST_PATH));
        assert(folderId != null);
        String docId = da.findDocumentId(folderId, "log");
        assert(docId != null);
    }

    public HtmlPage setupProjectForDocUpload() throws Exception {
        WebClient wc = new WebClient();
        HtmlPage configurePage = (HtmlPage) wc.goTo(this.job.getShortUrl() 
                                                    + "configure");
        HtmlCheckBoxInput docCheck = (HtmlCheckBoxInput)Util
            .getFirstHtmlElementByName(configurePage, 
                                       "hudson-plugins-collabnet-" +
                                       "documentuploader-CNDocumentUploader");
        docCheck.click();
        configurePage = (HtmlPage) Util.setText(configurePage, URL_ID, CN_URL);
        configurePage = (HtmlPage) Util.setText(configurePage, USER_ID, 
                                                TEST_USER);
        configurePage = (HtmlPage) Util.setPassword(configurePage, 
                                                    PASSWORD_ID, TEST_PW);
        configurePage = (HtmlPage) Util.setText(configurePage, PROJECT_ID, 
                                                CN_PROJECT_NAME);
        configurePage = (HtmlPage) Util.setText(configurePage, PATH_ID, 
                                                TEST_PATH);
        configurePage = (HtmlPage) Util.setText(configurePage, DESC_ID, 
                                                TEST_DESC);
        configurePage = (HtmlPage) Util.setTextByName(configurePage, 
                                                      FILE_NAME, 
                                                      TEST_FILE_PATTERN);
        Util.clickRadio(configurePage, BUILDLOG_NAME, UPLOAD_BUILD_LOG);
        return configurePage;
    }

    /**
     * Handles submitting a form on a given page.
     */
    public Page submitForm(HtmlPage page, String formName) throws Exception {
        HtmlForm form = page.getFormByName(formName);
        return this.submit(form);
    }
}
