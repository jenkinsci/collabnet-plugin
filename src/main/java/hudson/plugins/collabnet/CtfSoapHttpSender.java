package hudson.plugins.collabnet;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class CtfSoapHttpSender {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the HttpClient used to submit SOAP requests.
     */
    protected CloseableHttpClient createHttpClient(PoolingHttpClientConnectionManager connectionManager)
    {
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setUserAgent("CollabNet Jenkins Plugin")
                .build();
    }

    public static SSLConnectionSocketFactory tryCreateAcceptAllSslSocketFactory() {
        try {
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, new TrustManager[] { new X509TrustManager() {

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                }
            } }, new java.security.SecureRandom());

            return new SSLConnectionSocketFactory(sslcontext, NoopHostnameVerifier.INSTANCE);
        }
        catch (Exception e) {
            return SSLConnectionSocketFactory.getSocketFactory();
        }
    }
}
