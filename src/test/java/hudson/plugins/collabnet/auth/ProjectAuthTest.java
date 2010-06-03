package hudson.plugins.collabnet.auth;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import hudson.model.FreeStyleProject;

import hudson.plugins.collabnet.util.Util;
import hudson.plugins.collabnet.util.WithLocalPlugin;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.PromotionProcess;
import hudson.plugins.promoted_builds.conditions.ManualCondition;

/**
 * Test authorization for a Hudson job associated with a CN project.
 */
public class ProjectAuthTest extends AbstractSecurityTestCase {
    private static final String CN_BUILDER = System.getProperty("build_user");
    private static final String CN_CONFIGER = 
        System.getProperty("config_user");
    private static final String CN_DELETER = System.getProperty("delete_user");
    private static final String CN_PROMOTER = 
        System.getProperty("promote_user");
    private static final String CN_READER = System.getProperty("read_user");

    private static final String PROMOTION_NAME = "promote_name";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        installAuthorizationStrategy();
    }

    public void testConfigRoundtrip() throws Exception {
        assertRoundtrip(new CNAuthProjectProperty(teamforge_project, false, null, false));
        assertRoundtrip(new CNAuthProjectProperty(teamforge_project, true, null, true));
    }

    private void assertRoundtrip(CNAuthProjectProperty before) throws Exception {
        FreeStyleProject job = createFreeStyleProject();
        job.addProperty(before);
        submit(createAdminWebClient().getPage(job,"configure").getFormByName("config"));
        CNAuthProjectProperty after = job.getProperty(CNAuthProjectProperty.class);
        assertEqualBeans(before,after,"project,createRoles,grantDefaultRoles");
    }

    @WithLocalPlugin (value="promoted-builds")
    public void testReadUsersAccess() throws Exception {
        FreeStyleProject job = this.setupProjectForAuth();
        WebClient logIn = createWebClient().login(CN_READER, CN_READER);
        logIn.getPage(job);
        Util.checkPageUnreachable(logIn, job.getShortUrl() + "configure");
        Util.checkPageUnreachable(logIn, job.getShortUrl() + "build");
        setupPromotionAndBuild(job);
        assert (!isBuildPromotable(logIn, job));
        assert (!isProjectDeletable(logIn, job));
    }
    
    @WithLocalPlugin (value="promoted-builds")
    public void testBuildUsersAccess() throws Exception {
        FreeStyleProject job = this.setupProjectForAuth();
        WebClient logIn = new WebClient().login(CN_BUILDER, CN_BUILDER);
        logIn.goTo(job.getShortUrl());
        Util.checkPageUnreachable(logIn, job.getShortUrl() + "configure");
        logIn.goTo(job.getShortUrl() + "build");
        setupPromotionAndBuild(job);
        assert (!isBuildPromotable(logIn, job));
        assert (!isProjectDeletable(logIn, job));
    }
    
    @WithLocalPlugin (value="promoted-builds")
    public void testPromoteUsersAccess() throws Exception {
        FreeStyleProject job = this.setupProjectForAuth();
        WebClient logIn = new WebClient().login(CN_PROMOTER, CN_PROMOTER);
        logIn.goTo(job.getShortUrl());
        Util.checkPageUnreachable(logIn, job.getShortUrl() + "configure");
        Util.checkPageUnreachable(logIn, job.getShortUrl() + "build");
        setupPromotionAndBuild(job);
        assert (isBuildPromotable(logIn, job));
        assert (!isProjectDeletable(logIn, job));
    }
    
    @WithLocalPlugin (value="promoted-builds")
    public void testConfigureUsersAccess() throws Exception {
        FreeStyleProject job = this.setupProjectForAuth();
        WebClient logIn = new WebClient().login(CN_CONFIGER, CN_CONFIGER);
        logIn.goTo(job.getShortUrl());
        logIn.goTo(job.getShortUrl() + "configure");
        Util.checkPageUnreachable(logIn, job.getShortUrl() + "build");
        setupPromotionAndBuild(job);
        assert (!isBuildPromotable(logIn, job));
        assert (!isProjectDeletable(logIn, job));
    }

    @WithLocalPlugin (value="promoted-builds")
    public void testDeleteUsersAccess() throws Exception {
        FreeStyleProject job = this.setupProjectForAuth();
        WebClient logIn = new WebClient().login(CN_DELETER, CN_DELETER);
        logIn.goTo(job.getShortUrl());
        Util.checkPageUnreachable(logIn, job.getShortUrl() + "configure");
        Util.checkPageUnreachable(logIn, job.getShortUrl() + "build");
        setupPromotionAndBuild(job);
        assert (!isBuildPromotable(logIn, job));
        assert (isProjectDeletable(logIn, job));
    }

    private boolean isProjectDeletable(WebClient logIn, FreeStyleProject job) 
        throws Exception {
        boolean deletable = true;
        try {
            HtmlPage jobDeletePage = logIn.goTo(job.getShortUrl()
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
            HtmlPage buildPromotePage = logIn.
                goTo(job.getShortUrl() + "lastBuild/promotion/");
            this.submitPromoteForm(buildPromotePage);
        } catch (FailingHttpStatusCodeException fhsce) {
            promotable = false;
        } catch (FormNotFoundException fnfe) {
            promotable = false;
        }
        return promotable;
    }

    /**
     * Setup a new Hudson job to use authorization from the CN project.
     */
    public FreeStyleProject setupProjectForAuth() throws Exception {
        FreeStyleProject job = this.createFreeStyleProject();
        job.addProperty(new CNAuthProjectProperty(teamforge_project,false,null,false));
        return job;
    }

    /**
     * Setup the Hudson job with a promotion and run one build (so that 
     * promotion pages will show up).
     */
    public void setupPromotionAndBuild(FreeStyleProject job) throws Exception {
        JobPropertyImpl prop = new JobPropertyImpl(job);
        job.addProperty(prop);
        PromotionProcess proc = prop.addProcess(PROMOTION_NAME);
        proc.conditions.add(new ManualCondition());

        // wait til the build completes
        buildAndAssertSuccess(job);
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
