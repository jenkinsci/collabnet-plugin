package hudson.plugins.collabnet.tracker;

import com.collabnet.ce.webservices.CollabNetApp;
import com.collabnet.ce.soap50.webservices.tracker.ArtifactSoapDO;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.tasks.Builder;
import hudson.tasks.Shell;
import hudson.util.DescribableList;
import hudson.util.DescriptorList;

import hudson.plugins.collabnet.util.CommonUtil;
import hudson.plugins.collabnet.util.CNHudsonUtil;
import hudson.plugins.collabnet.util.HudsonConstants;
import hudson.plugins.collabnet.util.Util;

import org.jvnet.hudson.test.HudsonTestCase;

public class TrackerTest extends HudsonTestCase {
    private static final String URL_ID = "cntracker.collabneturl";
    private static final String USER_ID = "cntracker.username";
    private static final String PASSWORD_ID = "cntracker.password";
    private static final String PROJECT_ID = "cntracker_project";
    private static final String TRACKER_ID = "cntracker_tracker";
    private static final String ISSUE_ID = "cntracker.title";
    private static final String ASSIGN_ID = "cntracker_assign_user";
    private static final String PRIORITY_NAME = "cntracker.priority";
    private static final String ATTACHLOG_NAME = "cntracker.attach_log";
    private static final String UPDATE_NAME = "cntracker.always_update";
    private static final String CLOSE_NAME = "cntracker.close_issue";
    private static final String RELEASE_ID = "cntracker_release";

    private static final String CN_URL = System.getProperty("teamforge_url");
    // this user needs access to the project and access to the tracker
    private static final String TEST_USER = System.getProperty("admin_user");
    private static final String TEST_PW = System.getProperty("password");
    private static final String CN_PROJECT_NAME = 
        System.getProperty("teamforge_project");
    private static final String TRACKER = System.getProperty("tracker");
    private static final String ISSUE_TITLE = 
        System.getProperty("issue_title");
    private static final String ASSIGN = System.getProperty("assign_user");
    private static final String PRIORITY = System.getProperty("priority");
    private static final String ATTACH_LOG = System.getProperty("attach_log");
    private static final String UPDATE = System.getProperty("always_update");
    private static final String CLOSE = System.getProperty("close_on_success");
    private static final String RELEASE = 
        System.getProperty("tracker_release");

    private Project job = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        TrackerPlugin tp = new TrackerPlugin();
        tp.start();
        this.job = this.createFreeStyleProject();
        HtmlPage configurePage = setupPageForTracker();
        this.submitForm(configurePage, HudsonConstants.CONFIGURE_FORM_NAME);
    }

    public HtmlPage setupPageForTracker() throws Exception {
        WebClient wc = new WebClient();
        HtmlPage configurePage = (HtmlPage) wc.goTo(this.job.getShortUrl() + 
                                                    "configure");
        HtmlCheckBoxInput trackerCheck = (HtmlCheckBoxInput)Util
            .getFirstHtmlElementByName(configurePage, 
                                       "hudson-plugins-collabnet-" +
                                       "tracker-CNTracker");
        trackerCheck.click();
        Util.setText(configurePage, URL_ID, CN_URL);
        Util.setText(configurePage, USER_ID, TEST_USER);
        Util.setPassword(configurePage, PASSWORD_ID, TEST_PW);
        Util.setText(configurePage, PROJECT_ID, CN_PROJECT_NAME);
        Util.setText(configurePage, TRACKER_ID, TRACKER);
        Util.setText(configurePage, ISSUE_ID, ISSUE_TITLE);
        Util.setText(configurePage, ASSIGN_ID, ASSIGN);
        Util.chooseSelection(configurePage, PRIORITY_NAME, PRIORITY);
        Util.clickRadio(configurePage, ATTACHLOG_NAME, ATTACH_LOG);
        Util.clickRadio(configurePage, UPDATE_NAME, UPDATE);
        Util.clickRadio(configurePage, CLOSE_NAME, CLOSE);
        Util.setText(configurePage, RELEASE_ID, RELEASE);
        return configurePage;
    }

    public void testTrackerSetup() throws Exception {
        WebClient wc = new WebClient();
        HtmlPage configurePage = (HtmlPage) wc.goTo(this.job.getShortUrl() + 
                                                    "configure");
        Util.checkText(configurePage, URL_ID, CN_URL);
        Util.checkText(configurePage, USER_ID, TEST_USER);
        Util.checkPassword(configurePage, PASSWORD_ID, TEST_PW);
        Util.checkText(configurePage, PROJECT_ID, CN_PROJECT_NAME);
        Util.checkText(configurePage, TRACKER_ID, TRACKER);
        Util.checkText(configurePage, ISSUE_ID, ISSUE_TITLE);
        Util.checkText(configurePage, ASSIGN_ID, ASSIGN);
        Util.checkSelection(configurePage, PRIORITY_NAME, PRIORITY);
        Util.checkRadioSelected(configurePage, ATTACHLOG_NAME, ATTACH_LOG);
        Util.checkRadioSelected(configurePage, UPDATE_NAME, UPDATE);
        Util.checkRadioSelected(configurePage, CLOSE_NAME, CLOSE);
        Util.checkText(configurePage, RELEASE_ID, RELEASE);
    }

    public void testSuccessfulBuildWithTracker() throws Exception {
        Shell successCmd = new Shell("echo \"Successful Build!\"");
        DescribableList<Builder, Descriptor<Builder>> builderList = 
            this.job.getBuildersList();
        builderList.clear();
        builderList.add(successCmd);
        AbstractBuild build = (AbstractBuild) this.job.scheduleBuild2(0).get();
        this.verifySuccessfulTrackerUpdate(build);
    }

    private ArtifactSoapDO getArtifact(AbstractBuild build) throws Exception {
        String title = CommonUtil.getInterpreted(build.getEnvVars(), 
                                                 ISSUE_TITLE);
        CollabNetApp cna = CNHudsonUtil.getCollabNetApp(CN_URL, TEST_USER, 
                                                        TEST_PW);
        ArtifactSoapDO artifact = CNHudsonUtil.
            getTrackerArtifact(cna, CN_PROJECT_NAME, TRACKER, title);
        cna.logoff();
        return artifact;
    }

    public void verifySuccessfulTrackerUpdate(AbstractBuild build) 
        throws Exception {
        ArtifactSoapDO artifact = this.getArtifact(build);
        if (UPDATE.equals("true")) {
            assert(artifact != null);
            assert(artifact.getStatus().equals("Closed"));
            this.verifyArtifactValues(artifact);
        } else {
            assert(artifact == null);
        }
    }

    public String getReleaseId() {
        CollabNetApp cna = CNHudsonUtil.getCollabNetApp(CN_URL, TEST_USER, 
                                                        TEST_PW);
        String projectId = CNHudsonUtil.getProjectId(cna, CN_PROJECT_NAME);
        return CNHudsonUtil.getProjectReleaseId(cna, projectId, RELEASE);
    }

    public void verifyArtifactValues(ArtifactSoapDO artifact) {
        assert(artifact.getPriority() == Integer.parseInt(PRIORITY));
        assert(artifact.getAssignedTo().equals(ASSIGN));
        assert(artifact.getReportedReleaseId().equals(this.getReleaseId()));
    }

    public void testBrokenBuildWithTracker() throws Exception {
        Shell failCmd = new Shell("echo \"Failed Build!\"; exit 1");
        DescribableList<Builder, Descriptor<Builder>> builderList = 
            this.job.getBuildersList();
        builderList.clear();
        builderList.add(failCmd);
        AbstractBuild build = (AbstractBuild) this.job.scheduleBuild2(0).get();
        this.verifyBrokenTrackerUpdate(build);
    }

    public void verifyBrokenTrackerUpdate(AbstractBuild build) 
        throws Exception {
        ArtifactSoapDO artifact = this.getArtifact(build);
        assert(artifact != null);
        assert(artifact.getStatus().equals("Open"));
        this.verifyArtifactValues(artifact);
    }

    /**
     * Handles submitting a form on a given page.
     */
    public Page submitForm(HtmlPage page, String formName) throws Exception {
        HtmlForm form = page.getFormByName(formName);
        return this.submit(form);
    }
}
