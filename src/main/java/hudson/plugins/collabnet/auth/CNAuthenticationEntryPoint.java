package hudson.plugins.collabnet.auth;

import hudson.model.Hudson;
import hudson.security.HudsonAuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.text.MessageFormat;

import org.acegisecurity.AuthenticationException;

import java.util.logging.Logger;

public class CNAuthenticationEntryPoint 
    extends HudsonAuthenticationEntryPoint {
    private static Logger log = Logger.getLogger("CNAuthenticationEntryPoint");

    /**
     * Override the parent's commence so that the returned status is not
     * 403.
     */
    @Override
    public void commence(ServletRequest request, ServletResponse response, 
                         AuthenticationException authException) 
        throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse rsp = (HttpServletResponse) response;

        String requestedWith = req.getHeader("X-Requested-With");
        if("XMLHttpRequest".equals(requestedWith)) {
            // container authentication normally relies on session attribute to
            // remember where the user came from, so concurrent AJAX requests
            // often ends up sending users back to AJAX pages after successful login.
            // this is not desirable, so don't redirect AJAX requests to the user.
            // this header value is sent from Prototype.
            rsp.sendError(SC_FORBIDDEN);
        } else {
            // give the opportunity to include the target URL
            String loginForm = req.getContextPath() + getLoginFormUrl();
            loginForm = MessageFormat.
                format(loginForm, 
                       URLEncoder.encode(req.getRequestURI(),"UTF-8"));
            req.setAttribute("loginForm", loginForm);
            rsp.setStatus(SC_OK);
            rsp.setContentType("text/html;charset=UTF-8");
            PrintWriter out;
            try {
                ServletOutputStream sout = rsp.getOutputStream();
                out = new PrintWriter(new OutputStreamWriter(sout));
            } catch (IllegalStateException e) {
                out = rsp.getWriter();
            }
            out.printf(
                "<html><head>" +
                "<meta http-equiv='refresh' content='1;url=%1$s'/>" +
                "<script>window.location.replace('%1$s');</script>" +
                "</head>" +
                "<body style='background-color:white; color:white;'>" +
                "Authentication required</body></html>", loginForm
            );
            out.flush();
        }

    }
}
