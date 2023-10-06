package com.collabnet.ce.webservices;

import hudson.plugins.collabnet.util.Helper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kohsuke Kawaguchi
 */
public class CTFDocument extends CTFItem {

    private final String description;
    Helper helper = new Helper();

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
        String end_point = app.getServerUrl() + CTFConstants.DOCUMENT_URL + getId();
        int currentVersion = 0;
        JSONObject docObj = new JSONObject();
        Response response = helper.request(end_point, app.getSessionId(), null, HttpMethod.GET, null);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status == 200) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                currentVersion = Integer.parseInt(data.get("currentVersion").toString());
                docObj.put("fileName", data.get("fileName").toString());
                docObj.put("mimeType", data.get("mimeType").toString());
                docObj.put("fileId", file != null ? file.getId() : null);
                docObj.put("currentVersion", currentVersion);
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in getDocumentData() - " + e.getLocalizedMessage(), e);
            }
            Response patchResponse = helper.request(end_point, app.getSessionId(), docObj.toString(), HttpMethod.PATCH, null);
            String patchResult = patchResponse.readEntity(String.class);
            int patchStatus = patchResponse.getStatus();
            if (patchStatus == 200) {
                JSONObject patchData = null;
                try {
                    patchData = (JSONObject) new JSONParser().parse(patchResult);
                } catch (ParseException e) {
                    logger.log(Level.WARNING, "Unable to parse the json content in updateDocument() - " + e.getLocalizedMessage(), e);
                }
            } else {
                logger.log(Level.WARNING, "Updating the document data failed - " + patchStatus + ", Error Msg - " +  patchResult);
            }
        } else {
            logger.log(Level.WARNING, "Error getting the document details - " + status + ", Error Msg - " + result);
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
