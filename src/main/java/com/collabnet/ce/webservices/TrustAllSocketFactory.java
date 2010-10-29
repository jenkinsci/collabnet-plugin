package com.collabnet.ce.webservices;

import org.apache.axis.AxisProperties;
import org.apache.axis.components.net.JSSESocketFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Hashtable;

/**
 * Makes Axis bypass SSL server certficate validation.
 * Useful if you need to talk to a bogus self-signed SSL server.
 *
 * @author Kohsuke Kawaguchi
 */
public class TrustAllSocketFactory extends JSSESocketFactory {
    /**
     * Note that this method needs to be called for each thread that uses Axis.
     */
    public static void install() {
        // TODO: figure out how to avoid VM-wide changes.
        AxisProperties.setProperty("axis.socketSecureFactory",TrustAllSocketFactory.class.getName());
    }

    public TrustAllSocketFactory(Hashtable attributes) {
        super(attributes);
    }

    @Override
    protected void initFactory() throws IOException {
        try {
            SSLContext context = SSLContext.getInstance("SSL");
            context.init(null, new TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new SecureRandom());
            sslFactory = context.getSocketFactory();
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        } catch (KeyManagementException e) {
            throw new Error(e);
        }
    }
}
