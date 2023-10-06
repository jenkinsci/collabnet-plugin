package com.collabnet.ce.webservices;

import hudson.plugins.collabnet.util.CNFormFieldValidator;
import net.sf.json.JSONObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kohsuke Kawaguchi
 */
public class CTFDocument extends CTFItem {

    private static final String DOCUMENT_URL = "/ctfrest/docman/v1/documents/";
    private final String description;

    static Logger logger = Logger.getLogger(CTFDocument.class.getName());

    public CTFDocument(CTFObject parent, JSONObject data) {
        super(parent, data);
        this.description = data.get("description").toString();
    }

    public String getDescription() {
        return description;
    }

    /**
     * Updates this document by a new file.
     */
    public void update(CTFFile file) throws IOException {
        String end_point =  app.getServerUrl() + DOCUMENT_URL + getId();
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpGet get = new HttpGet(end_point);
            get.setHeader("Accept", "application/json");
            get.setHeader("Authorization", "Bearer " + app.getSessionId());
            response = httpClient.execute(get);
            JSONObject docData = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
            int currentVersion = Integer.parseInt(docData.get("currentVersion").toString());
            HttpPatch patch = new HttpPatch(end_point);
            patch.setHeader("Accept", "application/json");
            patch.setHeader("Content-Type", "application/json");
            patch.setHeader("If-Match", "W/*");
            patch.setHeader("Authorization", "Bearer " + app.getSessionId());
            JSONObject docObj = new JSONObject()
                    .element("fileName", docData.get("fileName").toString())
                    .element("mimeType", docData.get("mimeType").toString())
                    .element("fileId", file.getId())
                    .element("currentVersion", currentVersion);
            StringEntity stringEntity = new StringEntity(docObj.toString(), ContentType.APPLICATION_JSON);
            patch.setEntity(stringEntity);
            response = httpClient.execute(patch);
        } catch (Exception e) {
            logger.log(Level.INFO,"Updating the document data failed - " + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        }
    }

    /**
     * Get the document's URL.
     *
     * @return an absolute URL to the document.
     */
    public String getURL() {
        return app.getServerUrl() + "/sf/go/" + getId();
    }
}
