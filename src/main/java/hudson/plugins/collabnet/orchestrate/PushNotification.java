package hudson.plugins.collabnet.orchestrate;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.collabnet.orchestrate.BuildNotifier.RadioConfig;
import hudson.plugins.collabnet.util.CNFormFieldValidator;
import hudson.plugins.collabnet.util.Helper;
import net.sf.json.JSONObject;
import org.apache.axis.utils.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PushNotification {

    Logger logger = Logger.getLogger(getClass().getName());

    public void handle(Run build, RadioConfig config, String ctfUrl, TaskListener listener,
                       String status, boolean excludeCommitInfo) throws
            IOException {
        int response = 0;
        String webhookEndpoint = null;
        JSONObject payload = getPayload(build, listener, status, excludeCommitInfo);
        if(verifyBuildMessage(payload)) {
        webhookEndpoint = config.getWebhookUrl();
        if (StringUtils.isEmpty(webhookEndpoint) || webhookEndpoint.indexOf("/v4/") == -1) {
            webhookEndpoint = Helper.getWebhookUrl(ctfUrl + ":3000");
        }
        config.setWebhookUrl(webhookEndpoint);
        logger.log(Level.INFO,"Webhook endpoint is registered successfully: " + webhookEndpoint);
        listener.getLogger().println("Send build notification to : " + webhookEndpoint);
        response = send(webhookEndpoint, payload.toString(), listener);
        }
        if(response == 201 || response == 200){
            listener.getLogger().println("Build notification sent successfully.");
        }
        else if(response == 400){
            Helper.markUnstable(
                    build,
                    listener.getLogger(),
                    "Build notification failed", getClass().getName());
            listener.getLogger().println("Response: 400 Bad Request- Check your webhook configuration or registered " +
                    "event" +
                    " publisher.");
            listener.getLogger().println("Build message - " + payload.toString());
        }
        else{
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

    private int send(String webhookUrl, String buildData, TaskListener listener) throws IOException {
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        int status = 0;
        try {
            client = CNFormFieldValidator.getHttpClient();
            HttpPost httpPost = new HttpPost(webhookUrl);
            StringEntity entity = new StringEntity(buildData);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            response = client.execute(httpPost);
            status = response.getStatusLine().getStatusCode();
        }catch (IOException e) {
            String errMsg = e.getMessage();
            Helper.logMsg(errMsg, listener, logger, e);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(response != null) {
                response.close();
            }
            if(client != null) {
                client.close();
            }
        }
        return status;
    }
}
