package hudson.plugins.collabnet.filerelease;

import com.collabnet.ce.webservices.CollabNetApp;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

import hudson.plugins.collabnet.util.HudsonConstants;
import hudson.plugins.collabnet.util.CNHudsonUtil;
import hudson.plugins.collabnet.util.Util;

import java.util.List;

import org.jvnet.hudson.test.HudsonTestCase;

public class FileReleaseTest extends HudsonTestCase {
    private static final String FILE_RELEASE_LABEL = "CollabNet File Release";
    private static final String URL_ID = "cnfr.collabneturl";
    private static final String USER_ID = "cnfr.username";
    private static final String PASSWORD_ID = "cnfr.password";
    private static final String PROJECT_ID = "_cnfr_project";
    private static final String PACKAGE_ID = "_cnfr_package";
    private static final String RELEASE_ID = "_cnfr_release";
    private static final String OVERWRITE_NAME = "cnfr.overwrite";
    private static final String FILE_NAME = "cnfr.file";
    
    private static final String CN_URL = System.getProperty("teamforge_url");
    // this user needs access to the project and access to upload file
    // releases
    private static final String TEST_USER = System.getProperty("admin_user");
    private static final String TEST_PW = System.getProperty("password");
    private static final String CN_PROJECT_NAME = 
        System.getProperty("teamforge_project");
    private static final String PACKAGE = System.getProperty("fr_package");
    private static final String RELEASE = System.getProperty("fr_release");
    private static final String OVERWRITE = System.getProperty("fr_overwrite");
    private static final String FILE = "test.txt";
    private static final String FILE_CONTENT = "Test file from FileRelease";

    private AbstractProject job = null;

    @Override 
    public void setUp() throws Exception {
        super.setUp();
        FileReleasePlugin fr = new FileReleasePlugin();
        fr.start();
        this.job = this.createFreeStyleProject();
        HtmlPage configurePage = setupProjectForFRUpload();
        this.submitForm(configurePage, HudsonConstants.CONFIGURE_FORM_NAME);
    }

    public String getUrlId(int unique_id) {
        return unique_id + "." + URL_ID;
    }

    public String getUserId(int unique_id) {
        return unique_id + "." + USER_ID;
    }
    
    public String getPasswordId(int unique_id) {
        return unique_id + "." + PASSWORD_ID;
    }

    public String getProjectId(int unique_id) {
        return "_" + unique_id + PROJECT_ID;
    }

    public String getPackageId(int unique_id) {
        return "_" + unique_id + PACKAGE_ID;
    }

    public String getReleaseId(int unique_id) {
         return "_" + unique_id + RELEASE_ID;
    }

    public String getOverwriteName(int unique_id) {
        return unique_id + "." + OVERWRITE_NAME;
    }

    public int getUniqueId(HtmlPage configurePage) {
        // the unique id we want is the last that's present on the page
        List<HtmlElement> elems = configurePage.getHtmlElementsByName(URL_ID);
        int id = -1;
        int unique_id = -1;
        for (HtmlElement elem: elems) {
            System.out.println("id = " + elem.getId());
            String[] parts = elem.getId().split("\\.");
            if (parts.length > 0) {
                id = Integer.parseInt(parts[0]);
                if (id > unique_id) {
                    unique_id = id;
                }
            }
        }
        return unique_id;
    }

    public HtmlPage setupProjectForFRUpload() throws Exception {
        WebClient wc = new WebClient();
        HtmlPage configurePage = (HtmlPage) wc.goTo(this.job.getShortUrl() + 
                                                    "configure");
        HtmlCheckBoxInput frCheck = (HtmlCheckBoxInput)Util
            .getFirstHtmlElementByName(configurePage, 
                                       "hudson-plugins-collabnet-" +
                                       "filerelease-CNFileRelease");
        frCheck.click();
        int unique_id = this.getUniqueId(configurePage);
        configurePage = (HtmlPage) Util.setText(configurePage, 
                                                getUrlId(unique_id), 
                                                CN_URL);
        configurePage = (HtmlPage) Util.setText(configurePage, 
                                                getUserId(unique_id), 
                                                TEST_USER);
        configurePage = (HtmlPage) Util.setPassword(configurePage,
                                                    getPasswordId(unique_id),
                                                    TEST_PW);
        configurePage = (HtmlPage) Util.setText(configurePage,
                                                getProjectId(unique_id),
                                                CN_PROJECT_NAME);
        configurePage = (HtmlPage) Util.setText(configurePage,
                                                getPackageId(unique_id),
                                                PACKAGE);
        configurePage = (HtmlPage) Util.setText(configurePage,
                                                getReleaseId(unique_id),
                                                RELEASE);
        Util.clickRadio(configurePage, this.getOverwriteName(unique_id), 
                        OVERWRITE); 
        configurePage = (HtmlPage) Util.setTextByName(configurePage, FILE_NAME,
                                                      FILE);
        return configurePage;
    }

    /**
     * Test that setting up the File Release upload data worked.
     */
    public void testFRUploadSetup() throws Exception {
        WebClient wc = new WebClient();
        HtmlPage configurePage = (HtmlPage) wc.goTo(this.job.getShortUrl() + 
                                                    "configure");
        int unique_id = this.getUniqueId(configurePage);
        Util.checkText(configurePage, getUrlId(unique_id), CN_URL);
        Util.checkText(configurePage, getUserId(unique_id), TEST_USER);
        Util.checkPassword(configurePage, getPasswordId(unique_id), TEST_PW);
        Util.checkText(configurePage, getProjectId(unique_id), 
                       CN_PROJECT_NAME);
        Util.checkText(configurePage, getPackageId(unique_id), PACKAGE);
        Util.checkText(configurePage, getReleaseId(unique_id), RELEASE);
        Util.checkRadioSelected(configurePage, 
                                this.getOverwriteName(unique_id), 
                                OVERWRITE);
        Util.checkTextByName(configurePage, FILE_NAME, FILE);
    }

    /**
     * Test FileRelease upload by building and uploading a test file.
     */
    public void testUpload() throws Exception {
        Util.createFileInWorkspace(this.job, FILE, FILE_CONTENT);
        AbstractBuild build = (AbstractBuild) this.job.scheduleBuild2(0).get();
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
    

    /**
     * Handles submitting a form on a given page.
     */
    public Page submitForm(HtmlPage page, String formName) throws Exception {
        HtmlForm form = page.getFormByName(formName);
        return this.submit(form);
    }
}
