package hudson.plugins.collabnet.auth;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.plugins.collabnet.util.CNFormFieldValidator;
import hudson.plugins.collabnet.util.CNHudsonUtil;
import hudson.security.SecurityRealm;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.util.logging.Logger;


public class CollabNetSecurityRealm extends SecurityRealm {
    private String collabNetUrl;

    /* viewing Jenkins page from CTF linked app should login to Jenkins */
    private boolean mEnableSSOAuthFromCTF;

    /* logging in to Jenkins should login to CTF */
    private boolean mEnableSSOAuthToCTF;

    private boolean mEnableSSORedirect = true;

    @DataBoundConstructor
    public CollabNetSecurityRealm(String collabNetUrl, boolean enableSSOAuthFromCTF, boolean enableSSOAuthToCTF) {
        this.collabNetUrl = CNHudsonUtil.sanitizeCollabNetUrl(collabNetUrl);
        this.mEnableSSOAuthFromCTF = enableSSOAuthFromCTF;
        this.mEnableSSOAuthToCTF = enableSSOAuthToCTF;
    }

    public String getCollabNetUrl() {
        return this.collabNetUrl;
    }

    /**
     * Single sign on preference governing making Jenkins read CTF's SSO token
     * @return true to enable
     */
    public boolean getEnableSSOAuthFromCTF() {
        return mEnableSSOAuthFromCTF;
    }

    /**
     * Single sign on preference governing making Jenkins login to CTF upon authenticating
     * @return true to enable
     */
    public boolean getEnableSSOAuthToCTF() {
        return mEnableSSOAuthToCTF;
    }

    /**
     * Whether after singole singon into CTF, we should automatically redirect back to Jenkins.
     * @return true to enable
     */
    public boolean getEnableSSORedirect() {
        return mEnableSSORedirect;
    }

    @Override
    public SecurityRealm.SecurityComponents createSecurityComponents() {
        return new SecurityRealm.SecurityComponents(new CollabNetAuthManager
                                                    (this.getCollabNetUrl()));
    }

    /**
     * The CollabNetSecurityRealm Descriptor class.
     */
    @Extension
    public static final class DescriptorImpl extends Descriptor<SecurityRealm> {
        /**
         * @return string to display for configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "Digital.ai Security Realm";
        }

        /**
         * Form validation for the CollabNet URL.
         *
         * @param value url
         */
        public FormValidation doCheckCollabNetUrl(@QueryParameter String value) {
            if (!Jenkins.getInstance().hasPermission(Hudson.ADMINISTER)) return FormValidation.ok();
            String collabNetUrl = value;
            if (collabNetUrl == null || collabNetUrl.equals("")) {
                return FormValidation.error("The CollabNet URL is required.");
            }
            return checkSoapUrl(collabNetUrl);
        }
        
        /**
         * Check that a URL has the expected SOAP service.
         *
         * @param collabNetUrl for the CollabNet server
         * @return returns true if we can get a wsdl from the url, which
         *         indicates that it's a working CollabNet server.
         */
        public FormValidation checkSoapUrl(String collabNetUrl) {
            return CNFormFieldValidator.checkSoapUrl(collabNetUrl);
        }    
    }

    private static final Logger LOGGER = Logger.getLogger(CollabNetSecurityRealm.class.getName());
}
