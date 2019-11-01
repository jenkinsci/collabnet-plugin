package hudson.plugins.collabnet.orchestrate;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.collabnet.orchestrate.BuildNotifier.OptionalWebhook;
import hudson.plugins.collabnet.util.CNFormFieldValidator;
import hudson.plugins.collabnet.util.Helper;
import net.sf.json.JSONObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import java.io.IOException;
import java.util.logging.Logger;

public class PushNotification {

    Logger logger = Logger.getLogger(getClass().getName());

    public void handle(Run build, OptionalWebhook webhook, TaskListener listener,
                       String status, boolean excludeCommitInfo) throws
            IOException {
        int response = 0;
        String token = null;
        listener.getLogger().println("Send build notification to : " + webhook.getWebhookUrl());
        JSONObject payload = getPayload(build, listener, status, excludeCommitInfo);
        if(verifyBuildMessage(payload)) {
            if (webhook.getWebhookUrl().indexOf("/v4/") == -1) {
                token = Helper.getWebhookToken(getWebhookLogin(webhook.getWebhookUrl()), webhook.getWebhookUsername(),
                        webhook.getWebhookPassword(), listener);
                if (token != null) {
                    response = send(webhook.getWebhookUrl(), token, payload.toString(), listener);
                }
            } else {
                response = send(webhook.getWebhookUrl(), token, payload.toString(), listener);
            }
        }
        if(response == 201 || response == 200){
            listener.getLogger().println("Build notification sent successfully.");
        }
        else if(response == 400 || token == null){
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

    private String getWebhookLogin(String webhookUrl) {
        String url = webhookUrl.substring(0, webhookUrl.indexOf("/inbox")).concat("/login");
        return url;
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

    private int send(String webhookUrl, String token, String buildData, TaskListener listener) throws IOException {
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        int status = 0;
        try {
            client = CNFormFieldValidator.getHttpClient();
            HttpPost httpPost = new HttpPost(webhookUrl);
            StringEntity entity = new StringEntity(buildData);
            httpPost.setEntity(entity);
            if (token != null) {
                httpPost.setHeader("Authorization", token);
            }
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
