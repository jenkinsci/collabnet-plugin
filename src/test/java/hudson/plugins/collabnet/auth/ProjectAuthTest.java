package hudson.plugins.collabnet.auth;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;

import hudson.plugins.collabnet.auth.AuthPlugin;
import hudson.plugins.collabnet.util.BuildCompleteListener;
import hudson.plugins.collabnet.util.HudsonConstants;
import hudson.plugins.collabnet.util.Util;
import hudson.plugins.collabnet.util.WithLocalPlugin;

import java.util.List;

import org.jvnet.hudson.test.HudsonTestCase;

/**
 * Test authorization for a Hudson job associated with a CN project.
 */
public class ProjectAuthTest extends HudsonTestCase {
    private static final String CN_PROJECT_NAME = 
        System.getProperty("teamforge_project");
    private static final String CN_BUILDER = System.getProperty("build_user");
    private static final String CN_CONFIGER = 
        System.getProperty("config_user");
    private static final String CN_DELETER = System.getProperty("delete_user");
    private static final String CN_PROMOTER = 
        System.getProperty("promote_user");
    private static final String CN_READER = System.getProperty("read_user");

    private static final String PROJECT_ID = "capp_project";
    private static final String PROMOTION_NAME = "promote_name";
    private static final String JOB_FORM_NAME = "config";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AuthPlugin auth = new AuthPlugin();
        auth.start();
        HtmlPage configurePage = new WebClient().goTo(HudsonConstants.
                                                      CONFIGURE_PAGE);
        configurePage = (HtmlPage) AuthnTest.
            configureCNAuthentication(configurePage);
        configurePage = (HtmlPage) AuthzTest.
            configureCNAuthorization(configurePage);
        // save configuration
        this.submitForm(configurePage, HudsonConstants.CONFIGURE_FORM_NAME);
    }

    public void testProjectSetup() throws Exception {
        WebClient loggedInAdmin = new WebClient().
            login(AuthzTest.TEST_ADMIN_USER, AuthzTest.TEST_PW);
        FreeStyleProject job = this.setupProjectForAuth(loggedInAdmin);
        HtmlPage jobConfigPage = (HtmlPage) loggedInAdmin.
            goTo(job.getShortUrl() + "configure");
        Util.checkText(jobConfigPage, PROJECT_ID, CN_PROJECT_NAME);
    }
        
    @WithLocalPlugin (value="promoted-builds")
    public void testReadUsersAccess() throws Exception {
        WebClient loggedInAdmin = new WebClient().
            login(AuthzTest.TEST_ADMIN_USER, AuthzTest.TEST_PW);
        FreeStyleProject job = this.setupProjectForAuth(loggedInAdmin);
        WebClient logIn = new WebClient().login(CN_READER, AuthzTest.TEST_PW);
        Util.checkPageReachable(logIn, job.getShortUrl());
        Util.checkPageUnreachable(logIn, job.getShortUrl() + "configure");
        Util.checkPageUnreachable(logIn, job.getShortUrl() + "build");
        setupPromotionAndBuild(loggedInAdmin, job);
        assert (isBuildPromotable(logIn, job) == false);
        assert (isProjectDeletable(logIn, job) == false);
    }
    
    @WithLocalPlugin (value="promoted-builds")
    public void testBuildUsersAccess() throws Exception {
        WebClient loggedInAdmin = new WebClient().
            login(AuthzTest.TEST_ADMIN_USER, AuthzTest.TEST_PW);
        FreeStyleProject job = this.setupProjectForAuth(loggedInAdmin);
        WebClient logIn = new WebClient().login(CN_BUILDER, AuthzTest.TEST_PW);
        Util.checkPageReachable(logIn, job.getShortUrl());
        Util.checkPageUnreachable(logIn, job.getShortUrl() + "configure");
        Util.checkPageReachable(logIn, job.getShortUrl() + "build");
        setupPromotionAndBuild(loggedInAdmin, job);
        assert (isBuildPromotable(logIn, job) == false);
        assert (isProjectDeletable(logIn, job) == false);
    }
    
    @WithLocalPlugin (value="promoted-builds")
    public void testPromoteUsersAccess() throws Exception {
        WebClient loggedInAdmin = new WebClient().
            login(AuthzTest.TEST_ADMIN_USER, AuthzTest.TEST_PW);
        FreeStyleProject job = this.setupProjectForAuth(loggedInAdmin);
        WebClient logIn = new WebClient().login(CN_PROMOTER, 
                                                AuthzTest.TEST_PW);
        Util.checkPageReachable(logIn, job.getShortUrl());
        Util.checkPageUnreachable(logIn, job.getShortUrl() + "configure");
        Util.checkPageUnreachable(logIn, job.getShortUrl() + "build");
        setupPromotionAndBuild(loggedInAdmin, job);
        assert (isBuildPromotable(logIn, job) == true);
        assert (isProjectDeletable(logIn, job) == false);
    }
    
    @WithLocalPlugin (value="promoted-builds")
    public void testConfigureUsersAccess() throws Exception {
        WebClient loggedInAdmin = new WebClient().
            login(AuthzTest.TEST_ADMIN_USER, AuthzTest.TEST_PW);
        FreeStyleProject job = this.setupProjectForAuth(loggedInAdmin);
        WebClient logIn = new WebClient().login(CN_CONFIGER, 
                                                AuthzTest.TEST_PW);
        Util.checkPageReachable(logIn, job.getShortUrl());
        Util.checkPageReachable(logIn, job.getShortUrl() + "configure");
        Util.checkPageUnreachable(logIn, job.getShortUrl() + "build");
        setupPromotionAndBuild(loggedInAdmin, job);
        assert (isBuildPromotable(logIn, job) == false);
        assert (isProjectDeletable(logIn, job) == false);
    }

    @WithLocalPlugin (value="promoted-builds")
    public void testDeleteUsersAccess() throws Exception {
        WebClient loggedInAdmin = new WebClient().
            login(AuthzTest.TEST_ADMIN_USER, AuthzTest.TEST_PW);
        FreeStyleProject job = this.setupProjectForAuth(loggedInAdmin);
        WebClient logIn = new WebClient().login(CN_DELETER, AuthzTest.TEST_PW);
        Util.checkPageReachable(logIn, job.getShortUrl());
        Util.checkPageUnreachable(logIn, job.getShortUrl() + "configure");
        Util.checkPageUnreachable(logIn, job.getShortUrl() + "build");
        setupPromotionAndBuild(loggedInAdmin, job);
        assert (isBuildPromotable(logIn, job) == false);
        assert (isProjectDeletable(logIn, job) == true);
    }

    private boolean isProjectDeletable(WebClient logIn, FreeStyleProject job) 
        throws Exception {
        boolean deletable = true;
        try {
            HtmlPage jobDeletePage = (HtmlPage) logIn.goTo(job.getShortUrl() 
                                                           + "delete");
            this.submitDeleteForm(jobDeletePage);
        } catch (com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException 
                 fhsce) {
            deletable = false;
        } catch (FormNotFoundException fnfe) {
            deletable = false;
        }
        return deletable;
    }

    private boolean isBuildPromotable(WebClient logIn, FreeStyleProject job) 
        throws Exception {
        boolean promotable = true;
        try {
            HtmlPage buildPromotePage = (HtmlPage) logIn.
                goTo(job.getShortUrl() + "lastBuild/promotion/");
            this.submitPromoteForm(buildPromotePage);
        } catch (com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException 
                 fhsce) {
            promotable = false;
        } catch (FormNotFoundException fnfe) {
            promotable = false;
        }
        return promotable;
    }

    /**
     * Setup a new Hudson job to use authorization from the CN project.
     */
    public FreeStyleProject setupProjectForAuth(WebClient loggedInAdmin) 
        throws Exception {
        FreeStyleProject job = this.createFreeStyleProject();
        HtmlPage jobConfigPage = (HtmlPage) loggedInAdmin.
            goTo(job.getShortUrl() + "configure");
        jobConfigPage = (HtmlPage) Util.setText(jobConfigPage, PROJECT_ID, 
                                                CN_PROJECT_NAME);
        this.submitForm(jobConfigPage, JOB_FORM_NAME);
        return job;
    }

    /**
     * Setup the Hudson job with a promotion and run one build (so that 
     * promotion pages will show up).
     */
    public void setupPromotionAndBuild(WebClient loggedInAdmin, 
                                       FreeStyleProject job) throws Exception {
        HtmlPage jobConfigPage = (HtmlPage) loggedInAdmin.
            goTo(job.getShortUrl() + "configure");
        HtmlCheckBoxInput promoteCheck = (HtmlCheckBoxInput) Util.
            getFirstHtmlElementByName(jobConfigPage, "promotions");
        assert(promoteCheck != null);
        promoteCheck.click();
        HtmlTextInput promoteName = (HtmlTextInput) Util.
            getFirstHtmlElementByName(jobConfigPage, "config.name");
        promoteName.setValueAttribute(PROMOTION_NAME);
        HtmlCheckBoxInput manualCheck = (HtmlCheckBoxInput) Util.
            getFirstHtmlElementByName(jobConfigPage, 
                                      "hudson-plugins-promoted_builds-" +
                                      "conditions-ManualCondition");
        manualCheck.click();
        // sometimes this value gets lost, but it's not a real bug
        // this is a work around.
        jobConfigPage = (HtmlPage) Util.setText(jobConfigPage, PROJECT_ID, 
                                                CN_PROJECT_NAME);
        this.submitForm(jobConfigPage, JOB_FORM_NAME);
        loggedInAdmin.goTo(job.getShortUrl() + "build");
        // wait til the build completes
        new BuildCompleteListener(FreeStyleBuild.class, job)
            .waitForBuildToComplete(2*60*1000);
    }

    
    /**
     * Handles submitting a form on a given page.
     */
    public Page submitForm(HtmlPage page, String formName) throws Exception {
        HtmlForm form = page.getFormByName(formName);
        return this.submit(form);
    }

    /**
     * Handles submitting the Promote form.
     */
    public Page submitPromoteForm(HtmlPage page) throws Exception {
        String action = "forcePromotion?name=" + PROMOTION_NAME;
        HtmlForm submitForm = Util.getFormWithAction(page, action);
        if (submitForm != null) {
            HtmlInput input = submitForm.getInputByValue("Force promotion");
            return input.click();
        } else {
            throw new FormNotFoundException("Promote form not found.");
        }
    }

    /**
     * Handles submitting the Delete form.
     */
    public Page submitDeleteForm(HtmlPage page) 
        throws Exception {
        HtmlForm submitForm = Util.getFormWithAction(page, "doDelete");
        if (submitForm != null) {
            return this.submit(submitForm);
        } else {
            throw new FormNotFoundException("Form for delete " +
                                            " could not be found on page " + 
                                            page);
        }
    }

    private class FormNotFoundException extends Exception {
        public FormNotFoundException(String msg) {
            super(msg);
        }
    }
}
