package com.collabnet.ce.webservices;

import hudson.plugins.collabnet.util.Helper;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A folder in the documents section.
 * @author Kohsuke Kawaguchi
 */
public class CTFDocumentFolder extends CTFFolder {

    static Logger logger = Logger.getLogger(CTFDocumentFolder.class.getName());

    Helper helper = new Helper();

    CTFDocumentFolder(CTFObject parent, JSONObject object) {
        super(parent, object, object.get("id").toString(), object.get("parentId").toString());
    }

    public CTFList<CTFDocumentFolder> getFolders() throws IOException {
        CTFList<CTFDocumentFolder> r = new CTFList<CTFDocumentFolder>();
        String end_point =  app.getServerUrl() + CTFConstants.DOCUMENT_FOLDERS_URL + getId() + "/documentfolders";
        Map<String, String> queryParam = new HashMap<>();
        queryParam.put("recursive", "false");
        queryParam.put("basic", "false");
        Response response = helper.request(end_point, app.getSessionId(), null, HttpMethod.GET, queryParam);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status == 200) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                if (data != null & data.containsKey("items")) {
                    JSONArray dataArray = (JSONArray)data.get("items");
                    Iterator it = dataArray.iterator();
                    while (it.hasNext()) {
                        JSONObject jsonObject = (JSONObject) it.next();
                        if (jsonObject != null) {
                            r.add(new CTFDocumentFolder(this, jsonObject));
                        }
                    }
                }
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in getFolders() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING, "Error getting the document folders - " + status  + ", Error Msg - " + result);
            throw new IOException("Error getting a document folders - " + status + ", Error Msg - " + helper.getErrorMessage(result));
        }
        return r;
    }

    public CTFDocumentFolder createFolder(String title, String description) throws IOException {
        String end_point =  app.getServerUrl() + CTFConstants.DOCUMENT_FOLDERS_URL + getId() + "/documentfolders";
        JSONObject requestPayload = new JSONObject();
        requestPayload.put("title", title);
        requestPayload.put("description", description);
        Response response = helper.request(end_point, app.getSessionId(), requestPayload.toString(), HttpMethod.POST, null);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status == 201) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                return new CTFDocumentFolder(this, data);
            } catch (ParseException e) {
                logger.log(Level.WARNING,"Unable to parse the json content in createFolder() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING,"Error creating a document folder - " + status  + ", Error Msg - " + result);
            throw new IOException("Error creating a document folder - " + status + ", Error Msg - " + helper.getErrorMessage(result));
        }
        return null;
    }

    public CTFList<CTFDocument> getDocuments() throws IOException {
        CTFList<CTFDocument> r = new CTFList<CTFDocument>();
        String end_point =  app.getServerUrl() + CTFConstants.DOCUMENT_FOLDERS_URL + getId() + "/documents";
        Map<String, String> queryParam = new HashMap<>();
        queryParam.put("offset", "0");
        queryParam.put("count", "25");
        Response response = helper.request(end_point, app.getSessionId(), null, HttpMethod.GET, queryParam);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status == 200) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                if (data != null & data.containsKey("items")) {
                    JSONArray dataArray = (JSONArray)data.get("items");
                    Iterator it = dataArray.iterator();
                    while (it.hasNext()) {
                        JSONObject jsonObject = (JSONObject) it.next();
                        if (jsonObject != null) {
                            r.add(new CTFDocument(this, jsonObject));
                        }
                    }
                }
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in getDocuments() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING, "Error getting the documents list - " + status  + ", Error Msg - " + result);
            throw new IOException("Error getting a documents list - " + status + ", Error Msg - " + helper.getErrorMessage(result));
        }
        return r;
    }

    public CTFDocument createDocument(java.lang.String title,
                              java.lang.String description,
                              java.lang.String versionComment,
                              java.lang.String status,
                              boolean createLocked,
                              java.lang.String fileName,
                              java.lang.String mimeType,
                              CTFFile file,
                              java.lang.String associationId,
                              java.lang.String associationDesc) throws IOException {
        String end_point =  app.getServerUrl() + CTFConstants.DOCUMENT_FOLDERS_URL + getId() + "/documents";
        JSONObject requestPayload = new JSONObject();
        requestPayload.put("title", title);
        requestPayload.put("description", description);
        requestPayload.put("status", status);
        requestPayload.put("fileName", fileName);
        requestPayload.put("mimeType", mimeType);
        requestPayload.put("fileId", file!=null?file.getId():null);
        Response response = helper.request(end_point, app.getSessionId(), requestPayload.toString(), HttpMethod.POST, null);
        String result = response.readEntity(String.class);
        int statusCode = response.getStatus();
        if (statusCode == 201) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                return new CTFDocument(this, data);
            } catch (ParseException e) {
                logger.log(Level.WARNING,"Unable to parse the json content in createDocument()  - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING,"Error creating a document - " + statusCode  + ", Error Msg - " + result);
            throw new IOException("Error creating a document - " + status + ", Error Msg - " + helper.getErrorMessage(result));
        }
        return null;
    }

    public String getURL() {
        return app.getServerUrl() + "/ctf/documents/list/" + getPath();
    }
}
