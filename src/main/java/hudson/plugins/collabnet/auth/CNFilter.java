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
import hudson.security.SecurityRealm;

import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;

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
     * @param request
     * @param reponse
     * @param chain remaining filters to handle.
     */
    public void doFilter(ServletRequest request, ServletResponse response, 
                         FilterChain chain) throws IOException, 
                                                   ServletException {
        // check if we're already authenticated
        Authentication auth = Hudson.getAuthentication();
        // check if we're in the CollabNetSecurity Realm
        SecurityRealm securityRealm = Hudson.getInstance().getSecurityRealm();
        if (Hudson.getInstance().isUseSecurity() && 
            (!auth.isAuthenticated() || 
             auth.getPrincipal().equals("anonymous")) 
            && securityRealm instanceof CollabNetSecurityRealm) {
            this.attemptSFLogin((CollabNetSecurityRealm)securityRealm, 
                                request, response);
        } else if (Hudson.getInstance().isUseSecurity() && 
                   securityRealm instanceof CollabNetSecurityRealm &&
                   auth instanceof CNAuthentication &&
                   !((CNAuthentication) auth).isCNAuthed()) {
            this.doSFAuth((CNAuthentication) auth, 
                          (CollabNetSecurityRealm)securityRealm,
                          (HttpServletRequest) request, 
                          (HttpServletResponse) response, chain);
            return;            
        } 
 
        chain.doFilter(request, response);
    }

    /**
     * Catch SSO data from CollabNet if data is present, and 
     * automatically login.  Used when the Hudson server is setup as a 
     * linked application in the CollabNet server.
     * The CollabNet server will sent 2 parameters: sfUsername and 
     * sfLoginToken.
     * The token is a one-time token that can be used to initiate a SOAP
     * session and set authentication.
     *
     * @param securityRealm
     * @param request
     * @param response
     */
    private void attemptSFLogin(CollabNetSecurityRealm securityRealm, 
                                ServletRequest request, 
                                ServletResponse response) {
        String url = securityRealm.getCollabNetUrl();
        String username = request.getParameter("sfUsername");
        String token = request.getParameter("sfLoginToken");
        if (username != null && token != null) {
            CollabNetApp ca = new CollabNetApp(url, username);
            try {
                ca.loginWithToken(token);
                Authentication cnauthentication = 
                    new CNAuthentication(username, ca);
                SecurityContextHolder.getContext().
                    setAuthentication(cnauthentication);
            } catch (RemoteException re) {
                // login failed, but continue
                log.severe("Login failed with RemoteException: " + 
                           re.getMessage());
            }
        } 
    }
    
    /**
     * Redirect to the CollabNet Server to login there, and then 
     * redirect back to our original location.
     *
     * @param auth
     * @param securityRealm
     * @param req
     * @param rsp
     * @param chain
     * @throws IOException
     * @throws ServletException
     */
    private void doSFAuth(CNAuthentication auth, 
                          CollabNetSecurityRealm securityRealm, 
                          HttpServletRequest req, HttpServletResponse rsp, 
                          FilterChain chain) throws IOException, 
                                                    ServletException {
        auth.setCNAuthed(true);
        String reqUrl = getCurrentUrl(req);
        String collabNetUrl = securityRealm.getCollabNetUrl();
        String username = (String)auth.getPrincipal();
        String id = ((CNAuthentication) auth).getSessionId();
        String cnauthUrl = collabNetUrl + "/sf/sfmain/do/soapredirect?id=" 
            + URLEncoder.encode(id, "UTF-8") + "&user=" + 
            URLEncoder.encode(username,"UTF-8")+"&redirectUrl=" + 
            URLEncoder.encode(reqUrl,"UTF-8");

        // prepare a redirect
        rsp.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        rsp.setHeader("Location", cnauthUrl);
        
        chain.doFilter(req,rsp);
    }

    /**
     * @param req the servlet request to pull data from, if root url is unset.
     * @param useReferer if true, use the referer to get the base URL.
     * @return the best guess for the current base URL (i.e. just the scheme,
     *         server, port).
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
        }
        return url.toString();
    }
    
    /**
     * @return the best guess for the current full URL.
     *         It will use the "referer" field from the request
     *         to determine the url, if it is present.
     */
    public static String getCurrentUrl(HttpServletRequest req) {
        StringBuilder url = new StringBuilder(getCurrentBaseUrl(req));
        if (req.getContextPath() != null) {
            url.append(req.getContextPath());
        }
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
