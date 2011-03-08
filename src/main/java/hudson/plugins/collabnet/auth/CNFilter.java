package hudson.plugins.collabnet.auth;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.net.URLEncoder;

import hudson.model.Hudson;
import hudson.plugins.collabnet.util.CommonUtil;
import hudson.security.SecurityRealm;

import org.acegisecurity.Authentication;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.GrantedAuthorityImpl;
import org.acegisecurity.context.SecurityContextHolder;

import org.acegisecurity.providers.anonymous.AnonymousAuthenticationToken;
import org.apache.commons.lang.StringUtils;

import com.collabnet.ce.webservices.CollabNetApp;

/**
 * Class for filtering CollabNet auth information for SSO.
 */
public class CNFilter implements Filter {
    private static Logger log = Logger.getLogger("CNFilter");

    public void init(FilterConfig filterConfig) {
    }

    /**
     * Filter for the CollabNet plugin.  Handles 2 separate tasks:  
     * 1. Attempts to use CollabNet tokens to login (if they are present and
     *    we're not currently authed.).
     * 2. If we have not yet logged into the CollabNet server, redirect
     *    to the CollabNet server and login.
     *
     * @param request the servlet request
     * @param response the servlet response
     * @param chain remaining filters to handle.
     */
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        if (Hudson.getInstance().isUseSecurity()) {
            // check if we're in the CollabNetSecurity Realm
            SecurityRealm securityRealm = Hudson.getInstance().getSecurityRealm();
            if (securityRealm instanceof CollabNetSecurityRealm) {
                CollabNetSecurityRealm cnRealm = (CollabNetSecurityRealm) securityRealm;
                boolean enableSSOFromCTF = cnRealm.getEnableSSOAuthFromCTF();
                boolean enableSSOToCTF = cnRealm.getEnableSSOAuthToCTF();

                Authentication auth = Hudson.getAuthentication();

                if (enableSSOFromCTF) {
                    HttpServletRequest httpRequest = (HttpServletRequest) request;
                    // first detect if we are accessing Jenkins through CTF
                    String username = request.getParameter("sfUsername");
                    if (username != null) {
                        // 'sfUsername' is used for CTF linked apps. if present make sure match the authenticated user
                        if (!username.equals(auth.getName())) {
                            auth.setAuthenticated(false);
                        }
                    }

                    if (!auth.isAuthenticated() || auth.getPrincipal().equals("anonymous")) {
                        loginHudsonUsingCTFSSO((CollabNetSecurityRealm)securityRealm, httpRequest);
                    }
                }

                if (enableSSOToCTF && auth instanceof CNAuthentication) {
                    CNAuthentication cnauth = (CNAuthentication) auth;
                    if (!cnauth.isCNAuthed()) {
                        loginToCTF(cnauth, (CollabNetSecurityRealm)securityRealm,
                            (HttpServletRequest) request, (HttpServletResponse) response);
                        return;
                    }
                }
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * Catch SSO data from CollabNet if data is present, and 
     * automatically login.  Used when the Jenkins server is setup as a 
     * linked application in the CollabNet server.
     * The CollabNet server will sent 2 parameters: sfUsername and 
     * sfLoginToken.
     * The token is a one-time token that can be used to initiate a SOAP
     * session and set authentication.
     *
     * @param securityRealm the security realm
     * @param request the huttp servlet reuqest
     */
    private void loginHudsonUsingCTFSSO(CollabNetSecurityRealm securityRealm, HttpServletRequest request) {
        String url = securityRealm.getCollabNetUrl();
        String username = request.getParameter("sfUsername");
        String token = request.getParameter("sfLoginToken");
        Authentication auth = null;
        boolean logoff = false;
        if (username != null && token != null) {
            CollabNetApp ca = new CollabNetApp(url, username);
            try {
                ca.loginWithToken(token);
                auth = new CNAuthentication(username, ca);
            } catch (RemoteException re) {
                // login failed, but continue
                log.severe("Login failed with RemoteException: " + 
                           re.getMessage());
                logoff = true; 
            }
        } else {
            logoff = true;
        }

        if (logoff) {
            auth = new AnonymousAuthenticationToken("anonymous","anonymous",
                new GrantedAuthority[]{new GrantedAuthorityImpl("anonymous")});
        }

        // ensure that a session exists before we set context in it
        // see artf42298
        request.getSession(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
    
    /**
     * Redirect to the CollabNet Server to login there, and then 
     * redirect back to our original location.
     *
     * @param cnauth the CNAuthentication object
     * @param securityRealm the security realm
     * @param request the http request
     * @param response the http response
     * @throws IOException
     * @throws ServletException
     */
    private void loginToCTF(CNAuthentication cnauth, CollabNetSecurityRealm securityRealm,
                          HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {

        cnauth.setCNAuthed(true);
        String reqUrl = getCurrentUrl(request);
        String collabNetUrl = securityRealm.getCollabNetUrl();
        String username = (String)cnauth.getPrincipal();
        String id = cnauth.getSessionId();
        String cnauthUrl = collabNetUrl + "/sf/sfmain/do/soapredirect?id=" 
            + URLEncoder.encode(id, "UTF-8") + "&user=" + 
            URLEncoder.encode(username,"UTF-8");

        if (securityRealm.getEnableSSORedirect()) {
            // append redirect only if enabled
            cnauthUrl = cnauthUrl + "&redirectUrl=" + URLEncoder.encode(reqUrl,"UTF-8");
        }

        // prepare a redirect
        response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        response.setHeader("Location", cnauthUrl);
        
    }

    /**
     * @param req the servlet request to pull data from, if root url is unset.
     * @return the best guess for the current base URL (i.e. just the scheme,
     *         server, port) plus the contextPath.
     */
    public static String getCurrentBaseUrl(HttpServletRequest req) {
        StringBuilder url = new StringBuilder();
        
        // Use the user configured url, if available.
        String rootUrl = Hudson.getInstance().getRootUrl();
        if (rootUrl != null) {
            url.append(rootUrl);
        }else {
            // otherwise, use the request
            url.append(req.getScheme());
            url.append("://");
            url.append(req.getServerName());
            if (req.getServerPort() != 80) {
                url.append(':').append(req.getServerPort());
            }
            url.append(req.getContextPath());
        }
        return url.toString();
    }
    
    /**
     * @return the best guess for the current full URL.
     *         It will use the "referer" field from the request
     *         to determine the url, if it is present.
     */
    public static String getCurrentUrl(HttpServletRequest req) {
        String curBaseUrl = getCurrentBaseUrl(req);
        // remove the contextPath from the url, since it will also
        // be present in the requestURI.
        curBaseUrl = CommonUtil.stripSlashes(curBaseUrl);
        curBaseUrl = StringUtils.removeEnd(curBaseUrl, req.getContextPath());
        StringBuilder url = new StringBuilder(curBaseUrl);
        if (req.getRequestURI() != null) {
            url.append(req.getRequestURI());
        }
        if (req.getQueryString() != null) {
            url.append("?" + req.getQueryString());
        }
        return url.toString();
    }
        
    // destroy is currently a no-op
    public void destroy() {
    }
}
