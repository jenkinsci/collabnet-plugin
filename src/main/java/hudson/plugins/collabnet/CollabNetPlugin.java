package hudson.plugins.collabnet;

import com.collabnet.ce.webservices.CollabNetApp;
import hudson.plugins.collabnet.auth.CNFilter;

import hudson.Plugin;
import hudson.util.PluginServletFilter;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

/**
 * Entry point for the plugins.  Initializes each sub-plugin.
 */
public class CollabNetPlugin extends Plugin {
    @Override
    public void start() throws Exception {
        PluginServletFilter.addFilter(new CNFilter());
        super.start();
    }


    /**
     * Permantently damage the HTTP client by having it accept bogus certificates.
     */
    public static void allowSelfSignedCertificate() {
        Protocol.registerProtocol("https", new Protocol("https",
                     (ProtocolSocketFactory)new EasySSLProtocolSocketFactory(),
                     443));
    }

    static {
        if (!Boolean.getBoolean(CollabNetPlugin.class.getName()+".strictSSLCheck")) {
            allowSelfSignedCertificate();
            CollabNetApp.disableSSLCertificateCheck = true;
        }
    }
}
