package hudson.plugins.collabnet.auth;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.text.MessageFormat;

public class CNAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    /**
     * Override the parent's commence so that the returned status is not
     * 403.
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException)
        throws IOException, ServletException {
        String requestedWith = request.getHeader("X-Requested-With");
        if("XMLHttpRequest".equals(requestedWith)) {
            // container authentication normally relies on session attribute to
            // remember where the user came from, so concurrent AJAX requests
            // often ends up sending users back to AJAX pages after successful login.
            // this is not desirable, so don't redirect AJAX requests to the user.
            // this header value is sent from Prototype.
            response.sendError(SC_FORBIDDEN);
        } else {
            // give the opportunity to include the target URL
            String loginForm = request.getContextPath() ;
            loginForm = MessageFormat.
                format(loginForm, 
                       URLEncoder.encode(request.getRequestURI(),"UTF-8"));
            request.setAttribute("loginForm", loginForm);
            response.setStatus(SC_OK);
            response.setContentType("text/html;charset=UTF-8");
            PrintWriter out;
            try {
                ServletOutputStream sout = response.getOutputStream();
                out = new PrintWriter(new OutputStreamWriter(sout));
            } catch (IllegalStateException e) {
                out = response.getWriter();
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
