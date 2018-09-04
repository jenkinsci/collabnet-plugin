package hudson.plugins.collabnet.orchestrate;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.collabnet.util.CNFormFieldValidator;
import hudson.plugins.collabnet.util.Helper;
import net.sf.json.JSONObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PushNotification {

    Logger logger = Logger.getLogger(getClass().getName());

    public void handle(Run build, String webhookUrl, TaskListener listener,
                       String status, boolean excludeCommitInfo) throws
            IOException {
        int response = 0;
        listener.getLogger().println("Send build notification to : " + webhookUrl);
        JSONObject payload = getPayload(build, listener, status, excludeCommitInfo);
        if(verifyBuildMessage(payload)) {
            response = send(webhookUrl, payload.toString(), listener);
        }
        if(response == 201 || response == 200){
            listener.getLogger().println("Build notification sent successfully.");
        } else{
            Helper.markUnstable(
                    build,
                    listener.getLogger(),
                    "Build notification failed", getClass().getName());
            listener.getLogger().println("Build message - " + payload.toString());
        }
    }

    private boolean verifyBuildMessage(JSONObject payload) {
        if(payload.get("repository") != null || payload.getJSONObject("repository")
                .get("revisions") != null){
            return true;
        }
        return false;
    }

    public JSONObject getPayload(Run build, TaskListener listener, String status, boolean excludeCommitInfo) throws
            IOException {
        return BuildEvent.constructJson(build, listener, status, excludeCommitInfo);
    }

    private int send(String url, String buildData, TaskListener listener) throws IOException {
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        int status = 0;
        try {
            client = CNFormFieldValidator.getHttpClient();
            HttpPost httpPost = new HttpPost(url);
            StringEntity entity = new StringEntity(buildData);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            response = client.execute(httpPost);
            status = response.getStatusLine().getStatusCode();
        }catch (IOException e) {
            String errMsg = e.getMessage();
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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(response != null) {
                response.close();
            }
            client.close();
        }
        return status;
    }
}
