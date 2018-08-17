package hudson.plugins.collabnet.orchestrate;

import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import java.io.IOException;

public class PushNotification {

    public void handle(Run build, String webhookUrl, TaskListener listener,
                       String status, boolean excludeCommitInfo) throws
            IOException {
        listener.getLogger().println("Send build notification to : " + webhookUrl);
        String message = getPayload(build, listener, status, excludeCommitInfo);
        int response = send(webhookUrl, message);
        if(response == 201 || response == 200){
            listener.getLogger().println("Build notification sent successfully.");
        } else{
            listener.getLogger().println("Build notification failed.");
            listener.getLogger().println("Build message - " + message);
        }
    }

    public String getPayload(Run build, TaskListener listener, String status, boolean excludeCommitInfo) throws
            IOException {
        return BuildEvent.constructJson(build, listener, status, excludeCommitInfo).toString();
    }

    private int send(String url, String buildData) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        StringEntity entity = new StringEntity(buildData);
        httpPost.setEntity(entity);
        //httpPost.setHeader("Authorization", token");
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");
        CloseableHttpResponse response;
        try{
            response = client.execute(httpPost);
        }finally {
            client.close();
        }
        return response.getStatusLine().getStatusCode();
    }
}
