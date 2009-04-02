package hudson.plugins.collabnet.auth;

import groovy.lang.Binding;

import hudson.model.Descriptor;
import hudson.security.SecurityRealm;
import hudson.util.FormFieldValidator;
import hudson.util.spring.BeanBuilder;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.springframework.web.context.WebApplicationContext;

import com.collabnet.ce.webservices.CollabNetApp;


public class CollabNetSecurityRealm extends SecurityRealm {
    private String collabNetUrl;

    public CollabNetSecurityRealm(String collabNetUrl) {
        this.collabNetUrl = collabNetUrl;
    }

    public String getCollabNetUrl() {
        return this.collabNetUrl;
    }

    public SecurityRealm.SecurityComponents createSecurityComponents() {
        return new SecurityRealm.SecurityComponents(new CollabNetAuthManager
                                                    (this.getCollabNetUrl()));
    }

    /**
     * Override the default createFilter.  We want to use one that does not
     * return a 403 on login redirect because that may cause problems when
     * Hudson is run behind a proxy.
     */
    @Override
    public Filter createFilter(FilterConfig filterConfig) {
        Binding binding = new Binding();
        SecurityComponents sc = this.createSecurityComponents();
        binding.setVariable("securityComponents", sc);
        BeanBuilder builder = new BeanBuilder(getClass().getClassLoader());
        builder.parse(getClass().
                      getResourceAsStream("CNSecurityFilters.groovy"),binding);
        WebApplicationContext context = builder.createApplicationContext();
        return (Filter) context.getBean("filter");
    }


    /**
     * @return the descriptor for CollabNetSecurityRealm
     */
    public Descriptor<SecurityRealm> getDescriptor() {
        return DESCRIPTOR;
    }

    /**
     * Descriptor should be singleton.
     */
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    

    /**
     * The CollabNetSecurityRealm Descriptor class.
     */
    public static final class DescriptorImpl 
        extends Descriptor<SecurityRealm> {
        DescriptorImpl() {
            super(CollabNetSecurityRealm.class);
        }

        /**
         * @return string to display for configuration screen.
         */
        public String getDisplayName() {
            return "CollabNet Security Realm";
        }

        /**
         * @return the url for the help files.
         */
        public static String getHelpUrl() {
            return "/plugin/collabnet/auth/";
        }

        /**
         * @return the path to the help file.
         */
        @Override
        public String getHelpFile() {
            return getHelpUrl() + "help-securityRealm.html";
        }

        /**
         * @param req config page parameters.
         * @return new CollabNetSecurityRealm object, instantiated from the 
         *         configuration form vars.
         * @throws FormException
         */
        @Override
        public CollabNetSecurityRealm newInstance(StaplerRequest req, 
                                                  JSONObject formData) 
            throws FormException {
            return new CollabNetSecurityRealm((String)formData.
                                              get("collabneturl"));
        }

        /**
         * Form validation for the CollabNet URL.
         *
         * @param req StaplerRequest which contains parameters from 
         *            the config.jelly.
         * @param rsp contains http response data (unused).
         * @throws IOException
         * @throws ServletException
         */
        public void doCollabNetUrlCheck(StaplerRequest req, 
                                        StaplerResponse rsp) 
            throws IOException, ServletException {
            new FormFieldValidator(req,rsp,true) {
                protected void check() throws IOException, ServletException {  
                    String collabNetUrl = request.getParameter("value");
                    if (collabNetUrl == null || collabNetUrl.equals("")) {
                        error("The CollabNet URL is required.");
                        return;
                    }
                    if (!checkSoapUrl(collabNetUrl)) {
                        error("Invalid CollabNet URL.");
                        return;
                    }
                    ok();           
                }
            }.process();
        }
        
        /**
         * Check that a URL has the expected SOAP service.
         *
         * @param collabNetUrl for the CollabNet server
         * @return returns true if we can get a wsdl from the url, which
         *         indicates that it's a working CollabNet server.
         */
        private boolean checkSoapUrl(String collabNetUrl) {
            String soapURL = collabNetUrl + CollabNetApp.SOAP_SERVICE + 
                "CollabNet?wsdl";
            HttpClient client = new HttpClient();
            try {
                GetMethod get = new GetMethod(soapURL);
                int status = client.executeMethod(get);
                if (status == 200) {
                    return true;
                } else {
                    return false;
                }
            } catch (IOException e) {
                return false;
            } catch (IllegalArgumentException iae) {
                return false;
            }
        }    
    }
}
