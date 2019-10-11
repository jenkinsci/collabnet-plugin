package hudson.plugins.collabnet.util;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import static org.apache.commons.lang.StringUtils.isBlank;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Helper {

    static Logger logger = Logger.getLogger(Helper.class.getName());

    /** Prefix for messages appearing in the console log, for readability */
    private static String LOG_MESSAGE_PREFIX = "TeamForge/EventQ Build Notifier - ";

    private static final String queryParam = "{\"PublisherName\": \"TeamForgeJenkinsIntegrationPlugin\"}";
    private static final String eventName = "JenkinsBuildData";
    
    public static String getToken(URL ctfUrl, String ctfUserName, String ctfPassword) throws IOException {
        String end_point = ctfUrl.toString()+"/oauth/auth/token";
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        String token = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpPost post = new HttpPost(end_point);
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("client_id", "api-client"));
            params.add(new BasicNameValuePair("grant_type", "password"));
            params.add(new BasicNameValuePair("username", ctfUserName));
            params.add(new BasicNameValuePair("password", ctfPassword));
            params.add(new BasicNameValuePair("scope", "urn:ctf:services:ctf " +
                    "urn:ctf:services:svn urn:ctf:services:gerrit"));
            post.setEntity(new UrlEncodedFormEntity(params));
            response = httpClient.execute(post);
            token = JSONObject.fromObject(EntityUtils.toString(response.getEntity())).
                    get("access_token").toString();
        } catch (Exception e) {
            logger.log(Level.INFO,"TeamForge Associations - " + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        }
        return token;
    }

    public static String getWebhookUrl(String ctfUrl) throws IOException {
        String webhookUrl = null;
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        String webhookEndPoint = null;
        try {
            webhookUrl =  ctfUrl.concat("/publishers/v4");
            client = CNFormFieldValidator.getHttpClient();
            HttpGet httpGet = new HttpGet(webhookUrl);
            URI uri = new URIBuilder(httpGet.getURI()).addParameter("q",
                    queryParam).build();
            httpGet.setURI(uri);
            response = client.execute(httpGet);
            String responseMsg = EntityUtils.toString(response.getEntity());
            if(!responseMsg.isEmpty()) {
                JSONObject responseObject = JSONObject.fromObject(responseMsg);
                int responseCode = Integer.parseInt(responseObject.get("HTTPStatusCode").toString());
                if (responseCode == 200) {
                    JSONArray jsonArray = (JSONArray) responseObject.get("Response");
                    if (!jsonArray.isEmpty()) {
                        JSONObject jsonObject = (JSONObject) jsonArray.get(0);
                        String endpointUrl = jsonObject.get("WebhookEndpoint").toString();
                        webhookEndPoint = endpointUrl.replace("<<EventName>>", eventName);
                    }
                } else {
                    logger.log(Level.INFO,"Error in getting the webhook endpoint.");
                    logger.log(Level.INFO,responseObject.get("ErrorText").toString());
                }
            }
        } catch (Exception e) {
            logger.log(Level.INFO,"TeamForge Associations - " + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if(client != null) {
                client.close();
            }
        }
        return webhookEndPoint;
    }

    public static void logMsg(String errMsg, TaskListener listener, Logger logger, Exception e){
        if (errMsg
                .startsWith("; nested exception is: \n\tjavax.net.ssl.SSLHandshakeException: ")) {
            listener.getLogger().println("SSL configuration error. Please check your SSL certificate and retry.");
            logger.log(Level.INFO,
                    "TeamForge Associations - " + e.getLocalizedMessage(), e);
        } else if (errMsg
                .endsWith("java.net.SocketTimeoutException: connect timed out")) {
            listener.getLogger().println("Connection timed out. Please check the configuration.");
            logger.log(Level.INFO,
                    "TeamForge Associations - " + e.getLocalizedMessage(), e);
        } else if (errMsg
                .endsWith("java.net.ConnectException: Connection timed out")) {
            listener.getLogger().println("Connection timed out. Please check the configuration.");
            logger.log(Level.INFO,
                    "TeamForge Associations - " + e.getLocalizedMessage(), e);
        } else if (errMsg.endsWith(": Connection refused")) {
            listener.getLogger().println("Connection refused. Please check the configuration.");
            logger.log(Level.INFO,
                    "TeamForge Associatqions - " + e.getLocalizedMessage(), e);
        } else {
            listener.getLogger().println(e.getLocalizedMessage());
            logger.log(Level.INFO,
                    "TeamForge Associations - " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Marks the build as unstable and logs a message.
     *
     * @param build
     *            the build to mark unstable
     * @param consoleLogger
     *            the logger to log to
     * @param message
     *            the message to log
     */
    public static void markUnstable(Run build,
                              PrintStream consoleLogger, String message, String className) {
        if(consoleLogger !=null ){
            log(message, consoleLogger);
            Logger logger = Logger.getLogger(className);
            logger.warning(message);
            build.setResult(Result.UNSTABLE);
        }

    }

    /**
     * Logging helper that prepends the log message prefix
     *
     * @param msg
     * @param printStream
     */
    public static void log(String msg, PrintStream printStream) {
        printStream.print(LOG_MESSAGE_PREFIX);
        printStream.println(msg);
    }

    public static StandardUsernamePasswordCredentials getCredentials(Item owner,
                                                                     String credentialsId, String webhookUrl) {
        StandardUsernamePasswordCredentials result = null;
        if (!isBlank(credentialsId)) {
            for (StandardUsernamePasswordCredentials c : lookupCredentials(owner, webhookUrl)) {
                if (c.getId().equals(credentialsId)) {
                    result = c;
                    break;
                }
            }
        }
        return result;
    }

    public static List<StandardUsernamePasswordCredentials> lookupCredentials(Item owner, String webhookUrl) {
        URIRequirementBuilder rBuilder = isBlank(webhookUrl) ?
                URIRequirementBuilder.create() : URIRequirementBuilder.fromUri(webhookUrl);
        return CredentialsProvider.lookupCredentials(
                StandardUsernamePasswordCredentials.class, owner, null, rBuilder.build());
    }

}
