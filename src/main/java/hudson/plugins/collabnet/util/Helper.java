package hudson.plugins.collabnet.util;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import net.sf.json.JSONObject;
import static org.apache.commons.lang.StringUtils.isBlank;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.isBlank;

public class Helper {

    static Logger logger = Logger.getLogger(Helper.class.getName());

    private static final String FOUNDATION_URL = "/ctfrest/foundation/v1/";

    /** Prefix for messages appearing in the console log, for readability */
    private static String LOG_MESSAGE_PREFIX = "TeamForge Build Notifier - ";

    public static String getToken(URL ctfUrl, String ctfUserName, String ctfPassword) throws IOException {
        String end_point = ctfUrl.toString()+"/oauth/auth/token";
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        String ctfSessionId = null;
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
            if (response.getStatusLine().getStatusCode() == 200) {
                String token = JSONObject.fromObject(EntityUtils.toString(response.getEntity())).
                        get("access_token").toString();
                ctfSessionId = getSessionId(ctfUrl, getOneTimeToken(ctfUrl, token));
            } else {
                throw new IOException("Invalid username or credentials");
            }
        } catch (Exception e) {
            throw new IOException("Invalid username or credentials");
        } finally {
            if(response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        }
        return ctfSessionId;
    }

    /**
     * Gets OneTimeToken  for given acess token
     * @param accessToken the access token
     */
    public static String getOneTimeToken(URL ctfUrl, String accessToken) throws IOException {
        String oneTimeToken = null;
        String end_point = ctfUrl.toString() + FOUNDATION_URL + "auth/oneTimeToken";
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpGet get = new HttpGet(end_point);
            get.setHeader("Accept", "application/json");
            get.setHeader("Authorization", "Bearer " + accessToken);
            response = httpClient.execute(get);
            JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
            oneTimeToken = data.get("oneTimeToken").toString();
        } catch (Exception e) {
            logger.log(Level.INFO,"Error getting the oneTimeToken for the access token  - " + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        }
        return oneTimeToken;
    }

    /**
     * Get session id  for given OneTimeToken
     * @param ctfUrl the ctf url
     * @param oneTimeToken the one time token
     */
    public static String getSessionId(URL ctfUrl, String oneTimeToken) throws IOException {
        String sessionId = null;
        String end_point = ctfUrl.toString() + FOUNDATION_URL + "auth/sessionId/" + oneTimeToken;
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpGet get = new HttpGet(end_point);
            response = httpClient.execute(get);
            JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
            sessionId = data.get("sessionId").toString();
        } catch (Exception e) {
            logger.log(Level.INFO,"Error getting the sessionId  for the oneTimeToken  - " + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        }
        return sessionId;
    }

    public static String getWebhookToken(String webhookUrl, String webhookUsername, String webhookPassword,
                                         TaskListener listener)
            throws
            IOException {
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        String token = null;
        try {
            client = CNFormFieldValidator.getHttpClient();
            HttpPost httpPost = new HttpPost(webhookUrl);
            StringEntity entity = new StringEntity(getLoginData(webhookUsername, webhookPassword).toString());
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            response = client.execute(httpPost);
            if(response.getStatusLine().getStatusCode() == 200) {
                token = JSONObject.fromObject(EntityUtils.toString(response.getEntity())).getJSONObject("Response").
                        get("SessionToken").toString();
            }
        }catch (IOException e) {
            String errMsg = e.getMessage();
            logMsg(errMsg, listener, logger, e);
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
        return token;
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

    private static JSONObject getLoginData(String webhookUsername, String webhookPassword) {
        return new JSONObject().
                element("Username", webhookUsername).
                element("Password", webhookPassword);
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

    public static JSONObject getUserData(String url, String sessionId, String username) throws IOException {
        JSONObject data = null;
        String end_point =  url + FOUNDATION_URL + "users/by-username/" + username;
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpGet get = new HttpGet(end_point);
            List<NameValuePair> params = new ArrayList<>();
            get.setHeader("Accept", "application/json");
            get.setHeader("Content-type", "application/json");
            get.setHeader("Authorization", "Bearer " + sessionId);
            response = httpClient.execute(get);
            if (response.getStatusLine().getStatusCode() == 200) {
                data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
            } else {
                throw new IOException("No user found");
            }
        } catch (Exception e) {
            throw new IOException("No user found");
        } finally {
            if(response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        }
        return data;
    }
}
