package hudson.plugins.collabnet.auth;

import com.collabnet.ce.webservices.CTFList;
import com.collabnet.ce.webservices.CTFProject;
import com.collabnet.ce.webservices.CTFRole;
import com.collabnet.ce.webservices.CollabNetApp;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import hudson.model.FreeStyleProject;

import hudson.plugins.collabnet.TestParam;
import hudson.plugins.collabnet.auth.CNProjectACL.CollabNetRoles;
import hudson.plugins.collabnet.util.Util;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.PromotionProcess;
import hudson.plugins.promoted_builds.conditions.ManualCondition;
import hudson.security.ACL;
import org.acegisecurity.context.SecurityContextHolder;

import java.rmi.RemoteException;

/**
 * Test authorization for a Jenkins job associated with a CN project.
 */
public class ProjectAuthTest extends AbstractSecurityTestCase {
    @TestParam
    private final String build_user = "hudsonBuild";
    @TestParam
    private final String config_user = "hudsonConfig";
    @TestParam
    private  final String delete_user = "hudsonDelete";
    @TestParam
    private  final String promote_user = "hudsonPromote";

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (!verifyOnline())    return;
        setGlobalConnectionFactory();
        installAuthorizationStrategy();

        CollabNetApp cna = connect();
        CTFProject p = cna.getProjectByTitle(teamforge_project);
        CTFList<CTFRole> existing = p.getRoles();
        CollabNetRole promote = null;
        for (CollabNetRole role: CNProjectACL.CollabNetRoles.getAllRoles()) {
            if (existing.byTitle(role.getName())==null)
                p.createRole(role.getName(), role.getDescription());
            if (role.getName().equals("Hudson Promote"))
                promote = role;
        }

        for (String name : new String[]{read_user,build_user,config_user,delete_user,promote_user}) {
            createUserIfNotExist(cna,name);
            p.addMember(name);
        }

        existing = p.getRoles();
        grant(existing, CollabNetRoles.HUDSON_BUILD_ROLE, build_user);
        grant(existing, CollabNetRoles.HUDSON_CONFIGURE_ROLE, config_user);
        grant(existing, CollabNetRoles.HUDSON_DELETE_ROLE, delete_user);
        grant(existing, promote, promote_user);
        for (String name : new String[]{read_user,build_user,config_user,delete_user,promote_user}) {
            grant(existing,CollabNetRoles.HUDSON_READ_ROLE,name);
        }
    }

    private void grant(CTFList<CTFRole> existing, CollabNetRole role, String member) throws RemoteException {
        CTFRole r = existing.byTitle(role.getName());
        if (r.getMembers().byTitle(member)==null)
            r.grant(member);
    }

    private void createUserIfNotExist(CollabNetApp cna, String user) throws Exception {
        if (cna.getUser(user)==null)
            createUser(cna,user);
    }

    public void testConfigRoundtrip() throws Exception {
        if (!verifyOnline())    return;
        SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM); // so that the test code can see everything

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

    public void testReadUsersAccess() throws Exception {
        if (!verifyOnline())    return;
        FreeStyleProject job = this.setupProjectForAuth();
        WebClient logIn = createWebClient().login(read_user, read_user);
        logIn.getPage(job);
        Util.checkPageUnreachable(logIn, job.getShortUrl() + "configure");
        Util.checkPageUnreachable(logIn, job.getShortUrl() + "build");
        setupPromotionAndBuild(job);
        assert (!isBuildPromotable(logIn, job));
        assert (!isProjectDeletable(logIn, job));
    }
    
    public void testBuildUsersAccess() throws Exception {
        if (!verifyOnline())    return;
        FreeStyleProject job = this.setupProjectForAuth();
        WebClient logIn = new WebClient().login(build_user, build_user);
        logIn.goTo(job.getShortUrl());
        Util.checkPageUnreachable(logIn, job.getShortUrl() + "configure");
        buildAndAssertSuccess(job);
        setupPromotionAndBuild(job);
        assertFalse(isBuildPromotable(logIn, job));
        assertFalse(isProjectDeletable(logIn, job));
    }
    
    public void testPromoteUsersAccess() throws Exception {
        if (!verifyOnline())    return;
        FreeStyleProject job = this.setupProjectForAuth();
        WebClient logIn = new WebClient().login(promote_user, promote_user);
        logIn.goTo(job.getShortUrl());
        Util.checkPageUnreachable(logIn, job.getShortUrl() + "configure");
        Util.checkPageUnreachable(logIn, job.getShortUrl() + "build");
        setupPromotionAndBuild(job);
        assert (isBuildPromotable(logIn, job));
        assert (!isProjectDeletable(logIn, job));
    }
    
    public void testConfigureUsersAccess() throws Exception {
        if (!verifyOnline())    return;
        FreeStyleProject job = this.setupProjectForAuth();
        WebClient logIn = new WebClient().login(config_user, config_user);
        logIn.goTo(job.getShortUrl());
        logIn.goTo(job.getShortUrl() + "configure");
        Util.checkPageUnreachable(logIn, job.getShortUrl() + "build");
        setupPromotionAndBuild(job);
        assert (!isBuildPromotable(logIn, job));
        assert (!isProjectDeletable(logIn, job));
    }

    public void testDeleteUsersAccess() throws Exception {
        if (!verifyOnline())    return;
        FreeStyleProject job = this.setupProjectForAuth();
        WebClient logIn = new WebClient().login(delete_user, delete_user);
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
        } catch (FailingHttpStatusCodeException fhsce) {
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
     * Setup a new Jenkins job to use authorization from the CN project.
     */
    public FreeStyleProject setupProjectForAuth() throws Exception {
        FreeStyleProject job = this.createFreeStyleProject();
        job.addProperty(new CNAuthProjectProperty(teamforge_project,false,null,false));
        return job;
    }

    /**
     * Setup the Jenkins job with a promotion and run one build (so that 
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

    private static final String PROMOTION_NAME = "promote_name";
}
