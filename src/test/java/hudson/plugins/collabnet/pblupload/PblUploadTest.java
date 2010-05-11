package hudson.plugins.collabnet.pblupload;

import com.collabnet.cubit.api.CubitConnector;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.TaskListener;

import hudson.plugins.collabnet.util.CommonUtil;
import hudson.plugins.collabnet.util.HudsonConstants;
import hudson.plugins.collabnet.util.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jvnet.hudson.test.HudsonTestCase;

public class PblUploadTest extends HudsonTestCase {
    // ids and names for finding fields
    public static final String LABEL = "Lab Management Project Build " +
        "Library (PBL) Uploader";
    public static final String URL_ID = "pblupload.hostURL";
    public static final String URL_NAME = "pblupload.host_url";
    public static final String USER_ID = "pblupload.user";
    public static final String KEY_ID = "pblupload.key";
    public static final String PROJECT_NAME = "pblupload.project";
    public static final String PUB_OR_PRIV_NAME = "pblupload.pub_or_priv";
    public static final String FILE_NAME = "pblupload.file";
    public static final String PATH_NAME = "pblupload.path";
    public static final String FORCE_NAME = "pblupload.force";
    public static final String DESCRIPTION_NAME = "pblupload.description";
    public static final String COMMENT_NAME = "pblupload.comment";
    
    // values to set
    public static final String CUBIT_URL = System.getProperty("lm_url");
    public static final String USERNAME = System.getProperty("lm_user");
    public static final String KEY = System.getProperty("key");
    public static final String PROJECT = System.getProperty("lm_project");
    public static final String PUB_OR_PRIV = System.getProperty("pub_or_priv");
    public static final String FILE = System.getProperty("pbl_filename");
    public static final String PATH = System.getProperty("pbl_path");
    public static final String FORCE = System.getProperty("pbl_force");
    public static final String DESCRIPTION = "Here is ${BUILD_ID} test.";
    public static final String COMMENT = "Uploaded by hudson plugin for " +
        "${BUILD_ID} build.";
    public static final String FILE_CONTENT = "Test content for the pbl file.";

    private AbstractProject job = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.job = this.createFreeStyleProject();
        HtmlPage configurePage = setupProjectForPblUpload();
        this.submitForm(configurePage, HudsonConstants.CONFIGURE_FORM_NAME);
    }

    public String getUrlId(int unique_id) {
        return unique_id + "." + URL_ID;
    }

    public String getUserId(int unique_id) {
        return unique_id + "." + USER_ID;
    }

    public String getKeyId(int unique_id) {
        return unique_id + "." + KEY_ID;
    }

    public String getProjectName(int unique_id) {
        return unique_id + "." + PROJECT_NAME;
    }

    public String getPubOrPrivName(int unique_id) {
        return unique_id + "." + PUB_OR_PRIV_NAME;
    }

    public String getFileName(int unique_id) {
        return unique_id + "." + FILE_NAME;
    }

    public String getPathName(int unique_id) {
        return unique_id + "." + PATH_NAME;
    }

    public String getForceName(int unique_id) {
        return unique_id + "." + FORCE_NAME;
    }

    public String getDescriptionName(int unique_id) {
        return unique_id + "." + DESCRIPTION_NAME;
    }

    public String getCommentName(int unique_id) {
        return unique_id + "." + COMMENT_NAME;
    }

    public HtmlPage setupProjectForPblUpload() throws Exception {
        WebClient wc = new WebClient();
        HtmlPage configurePage = (HtmlPage) wc.goTo(this.job.getShortUrl() + 
                                                    "configure");
        HtmlCheckBoxInput pblCheck = (HtmlCheckBoxInput)Util
            .getFirstHtmlElementByName(configurePage, 
                                       "hudson-plugins-collabnet-" +
                                       "pblupload-PblUploader");
        pblCheck.click();
        int unique_id = this.getUniqueId(configurePage);
        configurePage = (HtmlPage) Util.setText(configurePage, 
                                                getUrlId(unique_id), 
                                                CUBIT_URL);
        configurePage = (HtmlPage) Util.setText(configurePage, 
                                                getUserId(unique_id), 
                                                USERNAME);
        configurePage = (HtmlPage) Util.setPassword(configurePage, 
                                                    getKeyId(unique_id), 
                                                    KEY);
        configurePage = (HtmlPage) Util.
            setTextByName(configurePage, getProjectName(unique_id), PROJECT);
        Util.clickRadio(configurePage, getPubOrPrivName(unique_id), 
                        PUB_OR_PRIV);
        configurePage = (HtmlPage) Util.setTextByName(configurePage, 
                                                      getFileName(unique_id), 
                                                      FILE);
        configurePage = (HtmlPage) Util.setTextByName(configurePage, 
                                                      getPathName(unique_id), 
                                                      PATH);
        Util.clickRadio(configurePage, getForceName(unique_id), FORCE);
        configurePage = (HtmlPage) Util.
            setTextByName(configurePage, getDescriptionName(unique_id), 
                          DESCRIPTION);
        configurePage = (HtmlPage) Util.
            setTextByName(configurePage, getCommentName(unique_id), 
                          COMMENT);
        return configurePage;
    }

    public void testPblUploadSetup() throws Exception {
        WebClient wc = new WebClient();
        HtmlPage configurePage = (HtmlPage) wc.goTo(this.job.getShortUrl() + 
                                                    "configure");
        int unique_id = this.getUniqueId(configurePage);
        Util.checkText(configurePage, getUrlId(unique_id), CUBIT_URL);
        Util.checkText(configurePage, getUserId(unique_id), USERNAME);
        Util.checkPassword(configurePage, getKeyId(unique_id), KEY);
        Util.checkTextByName(configurePage, getProjectName(unique_id), 
                             PROJECT);
        Util.checkRadioSelected(configurePage, getPubOrPrivName(unique_id), 
                                PUB_OR_PRIV);
        Util.checkTextByName(configurePage, getFileName(unique_id), FILE);
        Util.checkTextByName(configurePage, getPathName(unique_id), PATH);
        Util.checkRadioSelected(configurePage, getForceName(unique_id), FORCE);
        Util.checkTextByName(configurePage, getDescriptionName(unique_id), 
                             DESCRIPTION);
        Util.checkTextByName(configurePage, getCommentName(unique_id), 
                             COMMENT);        
    }

    public void testPblUpload() throws Exception {
        Util.createFileInWorkspace(this.job, FILE, FILE_CONTENT);
        AbstractBuild build = (AbstractBuild) this.job.scheduleBuild2(0).get();
        this.verifyPblUpload(build);
    }

    /**
     * To verify that the upload succeeded, we'll attempt to change the
     * description.
     */
    public void verifyPblUpload(AbstractBuild build) throws Exception {
        Map<String, String> args = new HashMap<String, String>();
        args.put("userid", USERNAME);
        args.put("desc", "Changed desc for test");
        args.put("comment", "Changing desc to test upload");
        args.put("path", CommonUtil.getInterpreted(build.getEnvironment(TaskListener.NULL), PATH)
                 + "/" + FILE);
        args.put("proj", PROJECT);
        args.put("sig", KEY);
        args.put("type", PUB_OR_PRIV);        
        CubitConnector cubitConnector = new CubitConnector(CUBIT_URL, USERNAME,
                                                           KEY);
        String xml = cubitConnector.callCubitApi("pbl_changedesc", args, true);
        String success = "<status>OK</status>";
        assert(xml.indexOf(success) != -1);
    }	   

    public int getUniqueId(HtmlPage configurePage) {
        // the unique id we want is the last that's present on the page
        List<HtmlElement> elems = configurePage.
            getElementsByName(URL_NAME);
        int id = -1;
        int unique_id = -1;
        for (HtmlElement elem: elems) {
            System.out.println("id = " + elem.getId());
            String[] parts = elem.getId().split("\\.");
            if (parts.length > 0 && !parts[0].equals("")) {
                id = Integer.parseInt(parts[0]);
                if (id > unique_id) {
                    unique_id = id;
                }
            }
        }
        return unique_id;
    }


    /**
     * Handles submitting a form on a given page.
     */
    public Page submitForm(HtmlPage page, String formName) throws Exception {
        HtmlForm form = page.getFormByName(formName);
        return this.submit(form);
    }
}
