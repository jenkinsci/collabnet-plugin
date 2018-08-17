package hudson.plugins.collabnet.util;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Run;
import net.sf.json.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Helper {

    /** Prefix for messages appearing in the console log, for readability */
    private static String LOG_MESSAGE_PREFIX = "TeamForge WEBR Build Notifier - ";

    static Logger logger = Logger.getLogger("hudson.plugins.collab.orchestrate");

    public static String getToken(URL ctfUrl, String ctfUserName, String ctfPassword) throws IOException {
        String end_point = ctfUrl.toString()+"/oauth/auth/token";
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost post = new HttpPost(end_point);
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("client_id", "api-client"));
        params.add(new BasicNameValuePair("grant_type", "password"));
        params.add(new BasicNameValuePair("username", ctfUserName));
        params.add(new BasicNameValuePair("password", ctfPassword));
        params.add(new BasicNameValuePair("scope", "urn:ctf:services:ctf " +
                "urn:ctf:services:svn urn:ctf:services:gerrit"));
        post.setEntity(new UrlEncodedFormEntity(params));
        HttpResponse response = httpclient.execute(post);
        return JSONObject.fromObject(EntityUtils.toString(response.getEntity())).get("access_token").toString();
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

}
