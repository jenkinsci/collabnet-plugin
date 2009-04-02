package hudson.plugins.collabnet.auth;

import com.gargoylesoftware.htmlunit.WebAssert;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

import java.util.List;

import hudson.plugins.collabnet.auth.AuthPlugin;
import hudson.plugins.collabnet.util.HudsonConstants;
import hudson.plugins.collabnet.util.Util;

import org.jvnet.hudson.test.HudsonTestCase;

/**
 * Test CollabNet Authentication.
 */
public class AuthnTest extends HudsonTestCase {
    private static final String COLLABNET_URL = 
        System.getProperty("teamforge_url");
    private static final String TEST_USER = System.getProperty("admin_user");
    private static final String TEST_PW = System.getProperty("password");
    // This user's login should fail.
    private static final String NON_USER = System.getProperty("invalid_user");
    
    // configure page variables
    private static final String SECURITY_CHECKBOX = "use_security";
    private static final String REALM_NAME = "realm";
    private static final String CN_SECURITY_REALM_LABEL = 
        "CollabNet Security Realm";
    private static final String CN_URL_ID = "cnauth.collabneturl";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AuthPlugin auth = new AuthPlugin();
        auth.start();
        HtmlPage configurePage = new WebClient().
            goTo(HudsonConstants.CONFIGURE_PAGE);
        configurePage = (HtmlPage) configureCNAuthentication(configurePage);
        // save configuration
        this.submitForm(configurePage, HudsonConstants.CONFIGURE_FORM_NAME);
    }

    /**
     * Turn on CN Authentication.  Save page.  Return to configure page
     * and make sure that Authentication is setup properly.
     */
    public void testCNAuthenticationSetup() throws Exception {
        checkCNAuthentication();
    }

    /**
     * Test bad login (i.e. a login that should fail).
     */
    public void testCNAuthenticationFailure() throws Exception {
        boolean failure = false;
        try {
            WebClient result = new WebClient().login(NON_USER, "");
        } catch (com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException 
                 fhsce) {
            // we should get a 401 here.
            failure = true;
        }
        assert(failure == true);
    }

    /**
     * Test good login (i.e. a login that should succeed).
     */
    public void testCNAuthentication() throws Exception {
        boolean failure = false;
        WebClient result = null;
        try {
            result = new WebClient().login(TEST_USER, TEST_PW);
        } catch (com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException 
                 fhsce) {
            // we should NOT get a 401 here.
            failure = true;
        }
        assert(failure != true);
    }

    /**
     * Go to the configure page and make sure that CollabNet Authentication
     * is setup.
     */
    private void checkCNAuthentication() throws Exception {
        // go to the configure page.
        HtmlPage page = new WebClient().goTo(HudsonConstants.CONFIGURE_PAGE);
        HtmlRadioButtonInput cnRealm = (HtmlRadioButtonInput) Util.
            getElementWithLabel(page, REALM_NAME, CN_SECURITY_REALM_LABEL);
        assert(cnRealm != null);
        assert(cnRealm.isChecked());
        Util.checkText(page, CN_URL_ID, COLLABNET_URL);
    }

    /**
     * Enable security on the configure screen, fill in the CollabNet
     * SecurityRealm section with appropriate value, and save config.
     */
    public static Page configureCNAuthentication(HtmlPage configurePage) 
        throws Exception {
        configurePage = enableSecurity(configurePage);
        return setupCNAuthentication(configurePage);
    }

    /**
     * Go to the configure Hudson page and enable security.  This method
     * does not save the form.  That will have to be done by a later method.
     */
    private static HtmlPage enableSecurity(HtmlPage configurePage) 
        throws Exception {
        // find and check the "Enable Security" check box.
        WebAssert.assertInputPresent(configurePage, SECURITY_CHECKBOX);
        HtmlElement enableSecurity = Util.
            getFirstHtmlElementByName(configurePage, SECURITY_CHECKBOX);
        assert(enableSecurity instanceof HtmlCheckBoxInput);
        HtmlCheckBoxInput enableSecurityCheck = (HtmlCheckBoxInput)
            enableSecurity;
        enableSecurityCheck.click();
        return configurePage;
    }

    /**
     * Given a configure page with security enabled, setup the Security Realm
     * to use CollabNet Security.
     */
    private static HtmlPage setupCNAuthentication(HtmlPage configurePage) 
        throws Exception {
        HtmlRadioButtonInput cnRealm = (HtmlRadioButtonInput) Util.getElementWithLabel(configurePage, REALM_NAME, CN_SECURITY_REALM_LABEL);
        assert(cnRealm != null);
        cnRealm.click();
        configurePage = (HtmlPage) Util.setText(configurePage, CN_URL_ID, 
                                                COLLABNET_URL);
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
