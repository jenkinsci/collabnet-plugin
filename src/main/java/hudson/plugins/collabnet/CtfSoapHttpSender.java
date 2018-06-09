package hudson.plugins.collabnet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.soap.MimeHeader;
import javax.xml.soap.SOAPException;

import org.apache.axis.AxisFault;
import org.apache.axis.Constants;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.components.net.CommonsHTTPClientProperties;
import org.apache.axis.components.net.CommonsHTTPClientPropertiesFactory;
import org.apache.axis.handlers.BasicHandler;
import org.apache.axis.soap.SOAP12Constants;
import org.apache.axis.soap.SOAPConstants;
import org.apache.axis.transport.http.HTTPConstants;
import org.apache.axis.utils.JavaUtils;
import org.apache.axis.utils.NetworkUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;

public class CtfSoapHttpSender extends BasicHandler {

    private static final long serialVersionUID = 1L;
    protected CommonsHTTPClientProperties clientProperties;
    protected CloseableHttpClient httpClient;

    @Override
    public void init()
    {
        clientProperties = CommonsHTTPClientPropertiesFactory.create();
        httpClient = createHttpClient(createConnectionManager());
    }

    @Override
    public void cleanup()
    {
        try {
            httpClient.close();
            httpClient = null;
        } catch (IOException e) {
            throw new RuntimeException("Failed to close HTTP client: " + e.getMessage(), e);
        }
    }

    /**
     * Creates the registries of socket factories to be used to establish connection to SOAP servers.
     */
    protected Registry<ConnectionSocketFactory> createSocketFactoryRegistry()
    {
        SSLConnectionSocketFactory sslFactory = tryCreateAcceptAllSslSocketFactory();
        return RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslFactory)
                .build();
    }

    /**
     * Creates the connection manager to be used to manage connections to SOAP servers.
     */
    protected PoolingHttpClientConnectionManager createConnectionManager()
    {
        PoolingHttpClientConnectionManager cm =
                new PoolingHttpClientConnectionManager(createSocketFactoryRegistry());
        cm.setMaxTotal(clientProperties.getMaximumTotalConnections());
        cm.setDefaultMaxPerRoute(clientProperties.getMaximumConnectionsPerHost());
        SocketConfig.Builder socketOptions = SocketConfig.custom();
        if (clientProperties.getDefaultSoTimeout() > 0) {
            socketOptions.setSoTimeout(clientProperties.getDefaultSoTimeout());
        }
        socketOptions.setTcpNoDelay(true);
        cm.setDefaultSocketConfig(socketOptions.build());
        return cm;
    }

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

    /**
     * Creates the HttpContext for a particular call to a SOAP server.
     *
     * Called once per session.
     */
    protected HttpClientContext createHttpContext(MessageContext msgContext, URI uri)
    {
        HttpClientContext context = new HttpClientContext(new BasicHttpContext());
        // if UserID is not part of the context, but is in the URL, use
        // the one in the URL.
        String userID = msgContext.getUsername();
        String passwd = msgContext.getPassword();
        if ((userID == null) && (uri.getUserInfo() != null)) {
            String info = uri.getUserInfo();
            int sep = info.indexOf(':');
            if ((sep >= 0) && (sep + 1 < info.length())) {
                userID = info.substring(0, sep);
                passwd = info.substring(sep + 1);
            } else {
                userID = info;
            }
        }
        if (userID != null) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            // if the username is in the form "user\domain"
            // then use NTCredentials instead.
            int domainIndex = userID.indexOf('\\');
            if (domainIndex > 0 && userID.length() > domainIndex + 1) {
                String domain = userID.substring(0, domainIndex);
                String user = userID.substring(domainIndex + 1);
                credsProvider.setCredentials(AuthScope.ANY,
                                             new NTCredentials(user, passwd, NetworkUtils.getLocalHostname(), domain));
            } else {
                credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userID, passwd));
            }
            context.setCredentialsProvider(credsProvider);
        }
        return context;
    }

    /**
     * Creates a HttpRequest encoding a particular SOAP call.
     *
     * Called once per SOAP call.
     */
    protected HttpUriRequest createHttpRequest(MessageContext msgContext, URI url) throws AxisFault {
        boolean posting = true;
        // If we're SOAP 1.2, allow the web method to be set from the
        // MessageContext.
        if (msgContext.getSOAPConstants() == SOAPConstants.SOAP12_CONSTANTS) {
            String webMethod = msgContext.getStrProp(SOAP12Constants.PROP_WEBMETHOD);
            if (webMethod != null) {
                posting = webMethod.equals(HTTPConstants.HEADER_POST);
            }
        }

        HttpRequestBase request = posting ? new HttpPost(url) : new HttpGet(url);

        // Get SOAPAction, default to ""
        String action = msgContext.useSOAPAction() ? msgContext.getSOAPActionURI() : "";
        if (action == null) {
            action = "";
        }

        Message msg = msgContext.getRequestMessage();
        request.addHeader(HTTPConstants.HEADER_CONTENT_TYPE, msg.getContentType(msgContext.getSOAPConstants()));
        request.addHeader(HTTPConstants.HEADER_SOAP_ACTION, "\"" + action + "\"");

        String httpVersion = msgContext.getStrProp(MessageContext.HTTP_TRANSPORT_VERSION);
        if (httpVersion != null && httpVersion.equals(HTTPConstants.HEADER_PROTOCOL_V10)) {
            request.setProtocolVersion(HttpVersion.HTTP_1_0);
        }

        // Transfer MIME headers of SOAPMessage to HTTP headers.
        javax.xml.soap.MimeHeaders mimeHeaders = msg.getMimeHeaders();
        if (mimeHeaders != null) {
            Iterator i = mimeHeaders.getAllHeaders();
            while (i.hasNext()) {
                MimeHeader mimeHeader = (MimeHeader) i.next();
                // HEADER_CONTENT_TYPE and HEADER_SOAP_ACTION are already set.
                // Let's not duplicate them.
                String name = mimeHeader.getName();
                if (!name.equals(HTTPConstants.HEADER_CONTENT_TYPE) && !name.equals(HTTPConstants.HEADER_SOAP_ACTION)) {
                    request.addHeader(name, mimeHeader.getValue());
                }
            }
        }

        boolean isChunked = false;
        boolean isExpectContinueEnabled = false;
        Map<?,?> userHeaderTable = (Map) msgContext.getProperty(HTTPConstants.REQUEST_HEADERS);
        if (userHeaderTable != null) {
            for (Map.Entry<?,?> me : userHeaderTable.entrySet()) {
                Object keyObj = me.getKey();
                if (keyObj != null) {
                    String key = keyObj.toString().trim();
                    String value = me.getValue().toString().trim();
                    if (key.equalsIgnoreCase(HTTPConstants.HEADER_EXPECT)) {
                        isExpectContinueEnabled = value.equalsIgnoreCase(HTTPConstants.HEADER_EXPECT_100_Continue);
                    } else if (key.equalsIgnoreCase(HTTPConstants.HEADER_TRANSFER_ENCODING_CHUNKED)) {
                        isChunked = JavaUtils.isTrue(value);
                    } else {
                        request.addHeader(key, value);
                    }
                }
            }
        }

        RequestConfig.Builder config = RequestConfig.custom();
        // optionally set a timeout for the request
        if (msgContext.getTimeout() != 0) {
            /* ISSUE: these are not the same, but MessageContext has only one definition of timeout */
            config.setSocketTimeout(msgContext.getTimeout()).setConnectTimeout(msgContext.getTimeout());
        } else if (clientProperties.getConnectionPoolTimeout() != 0) {
            config.setConnectTimeout(clientProperties.getConnectionPoolTimeout());
        }
        config.setContentCompressionEnabled(msgContext.isPropertyTrue(HTTPConstants.MC_ACCEPT_GZIP));
        config.setExpectContinueEnabled(isExpectContinueEnabled);
        request.setConfig(config.build());

        if (request instanceof HttpPost) {
            HttpEntity requestEntity = new MessageEntity(request, msgContext.getRequestMessage(), isChunked);
            if (msgContext.isPropertyTrue(HTTPConstants.MC_GZIP_REQUEST)) {
                requestEntity = new GzipCompressingEntity(requestEntity);
            }
            ((HttpPost) request).setEntity(requestEntity);
        }

        return request;
    }

    /**
     * Extracts the SOAP response from an HttpResponse.
     */
    protected Message extractResponse(MessageContext msgContext, HttpResponse response) throws IOException
    {
        int returnCode = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();
        if (entity != null && returnCode > 199 && returnCode < 300) {
            // SOAP return is OK - so fall through
        } else if (entity != null && msgContext.getSOAPConstants() == SOAPConstants.SOAP12_CONSTANTS) {
            // For now, if we're SOAP 1.2, fall through, since the range of
            // valid result codes is much greater
        } else if (entity != null && returnCode > 499 && returnCode < 600 &&
                   Objects.equals(getMimeType(entity), "text/xml")) {
            // SOAP Fault should be in here - so fall through
        } else {
            String statusMessage = response.getStatusLine().getReasonPhrase();
            AxisFault fault = new AxisFault("HTTP", "(" + returnCode + ")" + statusMessage, null, null);
            fault.setFaultDetailString("Return code: " + String.valueOf(returnCode) +
                                       (entity == null ? "" : "\n" + EntityUtils.toString(entity)));
            fault.addFaultDetail(Constants.QNAME_FAULTDETAIL_HTTPERRORCODE, String.valueOf(returnCode));
            throw fault;
        }

        org.apache.http.Header contentLocation = response.getFirstHeader(HttpHeaders.CONTENT_LOCATION);
        Message outMsg = new Message(entity.getContent(), false,
                                     Objects.toString(ContentType.get(entity), null),
                                     (contentLocation == null) ? null : contentLocation.getValue());
        // Transfer HTTP headers of HTTP message to MIME headers of SOAP message
        javax.xml.soap.MimeHeaders responseMimeHeaders = outMsg.getMimeHeaders();
        for (org.apache.http.Header responseHeader : response.getAllHeaders()) {
            responseMimeHeaders.addHeader(responseHeader.getName(), responseHeader.getValue());
        }
        outMsg.setMessageType(Message.RESPONSE);
        return outMsg;
    }

    private static String getMimeType(HttpEntity entity)
    {
        ContentType contentType = ContentType.get(entity);
        return (contentType == null) ? null : contentType.getMimeType();
    }

    /**
     * Sends the request SOAP message and then reads the response SOAP message back from the SOAP server.
     */
    @Override
    public void invoke(MessageContext msgContext) throws AxisFault
    {
        if (this.httpClient == null) {
            init();
        }
        try {
            URI uri = new URI(msgContext.getStrProp(MessageContext.TRANS_URL));

            HttpClientContext context;
            if (msgContext.getMaintainSession()) {
                context = createHttpContext(msgContext, uri);
            } else {
                context = createHttpContext(msgContext, uri);
            }

            HttpUriRequest request = createHttpRequest(msgContext, uri);
            CloseableHttpResponse response = null;
            try  {
                response = httpClient.execute(request, context);
                Message outMsg = extractResponse(msgContext, response);
                msgContext.setResponseMessage(outMsg);
                outMsg.getSOAPEnvelope();
            }
            finally {
                if (response != null) {
                    response.close();
                }
            }
        } catch (AxisFault e) {
            throw e;
        } catch (Exception e) {
            throw AxisFault.makeFault(e);
        }
    }

    protected static class MessageEntity extends AbstractHttpEntity
    {
        private final HttpRequestBase method;
        private final Message message;

        public MessageEntity(HttpRequestBase method, Message message, boolean httpChunkStream)
        {
            this.message = message;
            this.method = method;
            setChunked(httpChunkStream);
        }

        protected boolean isContentLengthNeeded()
        {
            return method.getProtocolVersion().equals(HttpVersion.HTTP_1_0) || !isChunked();
        }

        @Override
        public boolean isRepeatable()
        {
            return true;
        }

        @Override
        public long getContentLength()
        {
            if (isContentLengthNeeded()) {
                try {
                    return message.getContentLength();
                } catch (AxisFault ignored) {
                }
            }
            return -1;
        }

        @Override
        public InputStream getContent() throws IOException, UnsupportedOperationException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void writeTo(OutputStream outstream) throws IOException
        {
            try {
                message.writeTo(outstream);
            } catch (SOAPException e) {
                throw new IOException(e.getMessage(), e);
            }
        }

        @Override
        public boolean isStreaming()
        {
            return false;
        }
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
