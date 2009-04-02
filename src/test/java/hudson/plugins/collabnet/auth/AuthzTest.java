package hudson.plugins.collabnet.auth;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;

import hudson.plugins.collabnet.auth.AuthPlugin;
import hudson.plugins.collabnet.util.HudsonConstants;
import hudson.plugins.collabnet.util.Util;

import org.jvnet.hudson.test.HudsonTestCase;


/**
 * Test CollabNet Authorization.
 */
public class AuthzTest extends HudsonTestCase {
    // TODO: write a script to set this up on a test instance.
    public static final String TEST_ADMIN_USER = 
        System.getProperty("admin_user");;
    private static final String TEST_ADMIN_GROUP = 
        System.getProperty("admin_group");
    private static final String TEST_ADMIN_GROUP_MEMBER = 
        System.getProperty("admin_group_member");
    private static final String TEST_READ_USER = 
        System.getProperty("read_user");
    private static final String TEST_READ_GROUP = 
        System.getProperty("read_group");
    private static final String TEST_READ_GROUP_MEMBER = 
        System.getProperty("read_group_member");
    // all test users share this pw
    public static final String TEST_PW = System.getProperty("password");

    private static final String AUTH_NAME = "authorization";
    private static final String CN_AUTHORIZATION_LABEL = 
        "CollabNet Authorization";
    private static final String ADMIN_USERS_ID = "cnas.adminUsers";
    private static final String ADMIN_GROUP_ID = "cnas.adminGroups";
    private static final String READ_USERS_ID = "cnas.readUsers";
    private static final String READ_GROUP_ID = "cnas.readGroups";
    

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AuthPlugin auth = new AuthPlugin();
        auth.start();
        HtmlPage configurePage = new WebClient().
            goTo(HudsonConstants.CONFIGURE_PAGE);
        configurePage = (HtmlPage) AuthnTest.
            configureCNAuthentication(configurePage);
        configurePage = (HtmlPage) configureCNAuthorization(configurePage);
        // save configuration
        this.submitForm(configurePage, HudsonConstants.CONFIGURE_FORM_NAME);
    }

    /**
     * Test that the CNAuthorization has been properly setup.
     */
    public void testCNAuthorizationSetup() throws Exception {
        WebClient loggedInAdmin = new WebClient().login(TEST_ADMIN_USER, 
                                                        TEST_PW);
        checkCNAuthorizationSetup(loggedInAdmin);
    }

    /**
     * Test that the admin user can log in and get to the configure page.
     */
    public void testAdminUserAccess() throws Exception {
        WebClient loggedInAdmin = new WebClient().login(TEST_ADMIN_USER, 
                                                        TEST_PW);
        Util.checkPageReachable(loggedInAdmin, HudsonConstants.CONFIGURE_PAGE);
    }

    /**
     * Test that the admin group can log in and get to the configure page.
     */
    public void testAdminGroupAccess() throws Exception {
        WebClient loggedInAdmin = new WebClient().
            login(TEST_ADMIN_GROUP_MEMBER, TEST_PW);
        Util.checkPageReachable(loggedInAdmin, HudsonConstants.CONFIGURE_PAGE);
    }

    /**
     * Test that the read user can log in and not get to the configure page.
     */
    public void testReadUserAccess() throws Exception {
        WebClient loggedInRead = new WebClient().login(TEST_READ_USER, 
                                                       TEST_PW);
        Util.checkPageUnreachable(loggedInRead, HudsonConstants.CONFIGURE_PAGE);
    }   

    /**
     * Test that the read group can log in and not get to the configure page.
     */
    public void testReadGroupAccess() throws Exception {
        WebClient loggedInRead = new WebClient().login(TEST_READ_GROUP_MEMBER, 
                                                       TEST_PW);
        Util.checkPageUnreachable(loggedInRead, 
                                  HudsonConstants.CONFIGURE_PAGE);
    }

    private void checkCNAuthorizationSetup(WebClient loggedInAdmin) 
        throws Exception {
        HtmlPage configurePage = loggedInAdmin.
            goTo(HudsonConstants.CONFIGURE_PAGE);
        HtmlRadioButtonInput cnAuth = (HtmlRadioButtonInput) Util.
            getElementWithLabel(configurePage, AUTH_NAME, 
                                CN_AUTHORIZATION_LABEL);
        assert(cnAuth != null);
        assert(cnAuth.isChecked());
        Util.checkText(configurePage, ADMIN_USERS_ID, TEST_ADMIN_USER);
        Util.checkText(configurePage, ADMIN_GROUP_ID, TEST_ADMIN_GROUP);
        Util.checkText(configurePage, READ_USERS_ID, TEST_READ_USER);
        Util.checkText(configurePage, READ_GROUP_ID, TEST_READ_GROUP);
    }

    public static Page configureCNAuthorization(HtmlPage configurePage) 
        throws Exception {
        HtmlRadioButtonInput cnAuth = (HtmlRadioButtonInput) Util.
            getElementWithLabel(configurePage, AUTH_NAME, 
                                CN_AUTHORIZATION_LABEL);
        assert(cnAuth != null);
        cnAuth.click();
        configurePage = (HtmlPage) Util.setText(configurePage, ADMIN_USERS_ID, 
                                                TEST_ADMIN_USER);
        configurePage = (HtmlPage) Util.setText(configurePage, ADMIN_GROUP_ID, 
                                                TEST_ADMIN_GROUP);
        configurePage = (HtmlPage) Util.setText(configurePage, READ_USERS_ID, 
                                                TEST_READ_USER);
        configurePage = (HtmlPage) Util.setText(configurePage, READ_GROUP_ID, 
                                                TEST_READ_GROUP);
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
