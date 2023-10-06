package com.collabnet.ce.webservices;

import hudson.plugins.collabnet.util.CNFormFieldValidator;
import net.sf.json.JSONObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kohsuke Kawaguchi
 */
public class CTFReleaseFile extends CTFItem {
    private final String description, mimeType, filename;
    private final long size;

    private static final String FRS_URL = "/ctfrest/frs/v1/files/";

    static Logger logger = Logger.getLogger(CTFReleaseFile.class.getName());

    protected CTFReleaseFile(CTFObject parent, JSONObject data) {
        super(parent, data);
        this.description = data.get("description").toString();
        this.mimeType = data.get("mimeType").toString();
        this.filename = data.get("filename").toString();
        this.size = Integer.parseInt(data.get("size").toString());
    }

    public String getDescription() {
        return description;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFilename() {
        return filename;
    }

    public long getSize() {
        return size;
    }

    public String getURL() {
        return app.getServerUrl() + "/sf/frs/do/downloadFile/" + getPath();
    }

    public void delete() throws IOException {
        String end_point =  app.getServerUrl() + FRS_URL + getId();
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpDelete delete = new HttpDelete(end_point);
            delete.setHeader("Accept", "application/json");
            delete.setHeader("Authorization", "Bearer " + app.getSessionId());
            response = httpClient.execute(delete);
        } catch (Exception e) {
            logger.log(Level.INFO,"Error deleting a file in the file release - " + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        }
    }
}
