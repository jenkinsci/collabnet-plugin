package jenkins.plugins.collabnet.security;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationException;
import org.acegisecurity.AuthenticationManager;
import org.acegisecurity.BadCredentialsException;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.acegisecurity.providers.anonymous.AnonymousAuthenticationToken;
import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.UserDetailsService;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.springframework.dao.DataAccessException;

import com.collabnet.ce.webservices.CTFGroup;
import com.collabnet.ce.webservices.CollabNetApp;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.User;
import hudson.plugins.collabnet.auth.CNAuthentication;
import hudson.plugins.collabnet.util.CNFormFieldValidator;
import hudson.plugins.collabnet.util.CNHudsonUtil;
import hudson.plugins.collabnet.util.CommonUtil;
import hudson.security.AbstractPasswordBasedSecurityRealm;
import hudson.security.GroupDetails;
import hudson.security.SecurityRealm;
import hudson.util.FormValidation;
import hudson.util.VersionNumber;
import jenkins.model.Jenkins;
import jenkins.security.SecurityListener;

/**
 * CollabNet security realm stores the user info in jenkins, but authenticates logins against the specified
 * CTF server. In case of API token usage, the secret stored as part of the local user info is used to login to
 * CTF server.
 */
public class CnSecurityRealm extends AbstractPasswordBasedSecurityRealm {

    private static final Logger LOGGER = Logger.getLogger(CnSecurityRealm.class.getName());
    
    private String collabNetUrl;

    /* viewing Jenkins page from CTF linked app should login to Jenkins */
    private boolean mEnableSSOAuthFromCTF;

    /* logging in to Jenkins should login to CTF */
    private boolean mEnableSSOAuthToCTF;

    private boolean mEnableSSORedirect = true;

    @DataBoundConstructor
    public CnSecurityRealm(String collabNetUrl, boolean enableSSOAuthFromCTF, boolean enableSSOAuthToCTF) {
        this.collabNetUrl = CNHudsonUtil.sanitizeCollabNetUrl(collabNetUrl);
        this.mEnableSSOAuthFromCTF = enableSSOAuthFromCTF;
        this.mEnableSSOAuthToCTF = enableSSOAuthToCTF;

        CollabNetApp cn = new CollabNetApp(this.collabNetUrl);
        try {
            VersionNumber apiVersion = new VersionNumber(cn.getApiVersion());
            if (apiVersion.compareTo(new VersionNumber("5.3.0.0")) >= 0) {
                // starting with CTF 5.3, redirect no longer works after login
                mEnableSSORedirect = false;
            }
        } catch (RemoteException re) {
            // ignore
            LOGGER.log(Level.WARNING, "Failed to retrieve the CTF version from "+this.collabNetUrl,re);
        }
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
    public SecurityComponents createSecurityComponents() {
        return new SecurityComponents(
                new AuthenticationManager() {

                    @Override
                    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                        if (authentication instanceof CNAuthentication) {
                            return authentication;
                        }
                        else if (authentication instanceof UsernamePasswordAuthenticationToken) {
                            String username = authentication.getName();
                            LOGGER.info("authenticating 1: " + username);
                            String password = (String) authentication.getCredentials();
                            CnUserDetails ud = (CnUserDetails) CnSecurityRealm.this.authenticate(username, password);
                            Authentication auth = ud.getAuthentication();
                            SecurityListener.fireAuthenticated(ud);
                            return auth;
                        }
                        throw new BadCredentialsException(
                                "Unexpected authentication type: " + authentication);
                    }
                },
                new UserDetailsService() {

                    @Override
                    public UserDetails loadUserByUsername(String username)
                            throws UsernameNotFoundException, DataAccessException {
                        return CnSecurityRealm.this.loadUserByUsername(username);
                    }
                }
                );    
    }

    @Override
    protected UserDetails authenticate(String username, String password) throws AuthenticationException {
        LOGGER.info("authenticating 2: " + username);
        Authentication authentication = createAuthentication(username, password);
        //SecurityContextHolder.getContext().setAuthentication(authentication);
        return new CnUserDetails((CNAuthentication) authentication);
    }

    private Authentication createAuthentication(String username, String password) throws AuthenticationException {
        LOGGER.info("creating authentication: " + username);
        try {
            CollabNetApp cna = new CollabNetApp(this.getCollabNetUrl(), username, password);
            Authentication authentication = new CNAuthentication(username, cna);
            User localUser = User.get(username, true, Collections.emptyMap());
            if (localUser != null) {
                CnUserSecretStorage.put(localUser, password);
            }
            return authentication;
        } catch (Exception e) {
            if (e instanceof AuthenticationException) {
                throw (AuthenticationException) e;
            }
            throw new BadCredentialsException("Failed to log into " + getCollabNetUrl() + ": " + e.getMessage(), e);
        }   
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LOGGER.info("Loading user: " + username);
        User localUser = User.get(username, false, Collections.emptyMap());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Exception exc = null;
        boolean loginToCollabnet = false;
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            loginToCollabnet = true;
        }
        if (loginToCollabnet) {
            if(localUser != null && CnUserSecretStorage.contains(localUser)){
                String secret = CnUserSecretStorage.retrieve(localUser);
                if (!CommonUtil.isEmpty(secret)) {
                    authentication = createAuthentication(username, secret);
                    //SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        if (authentication instanceof CNAuthentication) {
            CNAuthentication cnAuthentication = (CNAuthentication) authentication;
            return new CnUserDetails(cnAuthentication);
        }
        String msg = "Invalid authentication: " + authentication;
        LOGGER.info(msg);
        throw exc == null ? new BadCredentialsException(msg) : new BadCredentialsException(msg, exc);
    }

    @Override
    public GroupDetails loadGroupByGroupname(String groupname) throws UsernameNotFoundException, DataAccessException {
        Authentication auth = Jenkins.getAuthentication();
        if (auth instanceof CNAuthentication) {
            CNAuthentication cnAuth = (CNAuthentication) auth;
            try {
                CTFGroup group = cnAuth.getCredentials().getGroupByTitle(groupname);
                return new CnGroupDetails(group.getFullName(),
                        new HashSet<String>(cnAuth.getCredentials().getGroupUsers(group.getId()))
                        );
            } catch (RemoteException e) {
                LOGGER.log(Level.WARNING, "Failed to get group info: " + groupname, e);
            }
        }
        return null;
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
            return "CollabNet Security Realm (Experimental)";
        }

        /**
         * Form validation for the CollabNet URL.
         *
         * @param value url
         */
        public FormValidation doCheckCollabNetUrl(@QueryParameter String value) {
            if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
                return FormValidation.ok();
            }
            if (CommonUtil.isEmpty(value)) {
                return FormValidation.error("The CollabNet URL is required.");
            }
            return checkSoapUrl(value);
        }
        
        /**
         * Check that a URL has the expected SOAP service.
         *
         * @param collabNetUrl for the CollabNet server
         * @return returns true if we can get a wsdl from the url, which
         *         indicates that it's a working CollabNet server.
         */
        private FormValidation checkSoapUrl(String collabNetUrl) {
            String soapURL = collabNetUrl + CollabNetApp.SOAP_SERVICE +  "CollabNet?wsdl";
            return CNFormFieldValidator.checkUrl(soapURL);
        }    
    }
}
