package com.collabnet.ce.webservices;

import hudson.plugins.collabnet.util.Helper;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kohsuke Kawaguchi
 */
public class CTFTracker extends CTFFolder {
    private CTFProject project;

    static Logger logger = Logger.getLogger(CTFTracker.class.getName());

    Helper helper = new Helper();

    CTFTracker(CTFObject parent, JSONObject data) {
        super(parent, data, data.get("trackerId").toString(), data.get("parentFolderId").toString());
        this.project = (CTFProject) parent;
    }

    public CTFProject getProject() {
        return project;
    }

    public List<CTFArtifact> getArtifactsByTitle(String title) throws IOException {
        List<CTFArtifact> r = new ArrayList<CTFArtifact>();
        String end_point =  app.getServerUrl() + CTFConstants.TRACKER_URL + getId() + "/artifacts/filter";
        JSONArray filterArray = new JSONArray();
        JSONObject filterPayload = new JSONObject();
        filterPayload.put("column", "title");
        filterPayload.put("type", "String");
        filterPayload.put("value", title);
        filterArray.add(filterPayload);
        JSONObject requestPayload = new JSONObject();
        requestPayload.put("filter", filterArray);
        requestPayload.put("count", -1);
        Response response = helper.request(end_point, app.getSessionId(), requestPayload.toString(), HttpMethod.POST, null);
        String result = response.readEntity(String.class);
        int statusCode = response.getStatus();
        if (statusCode == 200) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                if (data != null & data.containsKey("items")) {
                    JSONArray dataArray = (JSONArray)data.get("items");
                    Iterator it = dataArray.iterator();
                    while (it.hasNext()) {
                        JSONObject jsonObject = (JSONObject) it.next();
                        if (jsonObject != null) {
                            r.add(new CTFArtifact(this, jsonObject));
                        }
                    }
                }
            } catch (ParseException e) {
                logger.log(Level.WARNING,"Unable to parse the json content in getArtifactsByTitle() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING,"Error getting the artifact details by title - " + statusCode + ", Error Msg - " + result);
            throw new IOException("Error getting the artifact details by title -" + statusCode + ", Error Msg - " + helper.getErrorMessage(result));
        }
        return r;
    }

    /**
     * Create a new tracker artifact with the given values.
     *
     * @param title for the new tracker
     * @param description of the tracker
     * @param group of the tracker
     * @param category of the tracker
     * @param status of the tracker (open, closed, etc).
     * @param customer this artifact affects.
     * @param priority of the artifact.
     * @param estimatedHours to fix the issue.
     * @param assignTo user to assign this issue to.
     * @param releaseId of the release this issue is associated with.
     * @param flexFields user-defined fields.
     * @param fileName of the attachment.
     * @param fileMimeType of the attachment.
     * @param file of the attachment (returned when attachment was uploaded).
     * @return the newly created Artifact object.
     * @throws RemoteException if any problems occurs while creating artifact
     */
    public CTFArtifact createArtifact(   String title,
                                                   String description,
                                                   String group,
                                                   String category,
                                                   String status,
                                                   String customer,
                                                   int priority,
                                                   int estimatedHours,
                                                   String assignTo,
                                                   String[] releaseId,
                                                   CTFFlexField flexFields,
                                                   String fileName,
                                                   String fileMimeType,
                                                   CTFFile file)
    throws IOException {
    	int remainingEffort = 0;
    	boolean autosumming = false;
    	int points = 0;
    	String planningFolderId = null;
        CTFArtifact ctfArtifact = null;
        String end_point =  app.getServerUrl() + CTFConstants.TRACKER_URL + getId() + "/artifacts";
        JSONArray attachmentArray = new JSONArray();
        JSONObject filePayload = new JSONObject();
        JSONArray releaseIds =  new JSONArray();
        for (String relId : releaseId) {
            releaseIds.add(relId);
        }
        if (file != null ) {
            filePayload.put("fileName", fileName);
            filePayload.put("mimeType", fileMimeType);
            filePayload.put("fileId", file != null ? file.getId() : null);
            attachmentArray.add(filePayload);
        }
        JSONObject requestPayload = new JSONObject();
        requestPayload.put("title", title);
        requestPayload.put("description", description);
        requestPayload.put("status", status);
        requestPayload.put("assignedTo", assignTo);
        requestPayload.put("releaseId", releaseIds);
        requestPayload.put("priority", String.valueOf(priority));
        requestPayload.put("attachments", attachmentArray);
        requestPayload.put("customer", customer);
        requestPayload.put("category", category);
        requestPayload.put("group", group);
        requestPayload.put("flexfields", flexFields);
        requestPayload.put("planningFolderId", planningFolderId);
        requestPayload.put("estimatedEffort", String.valueOf(estimatedHours));
        requestPayload.put("remainingEffort", String.valueOf(remainingEffort));
        requestPayload.put("autoSummingPoints", String.valueOf(autosumming));
        requestPayload.put("points", String.valueOf(points));

        Response response = helper.request(end_point, app.getSessionId(), requestPayload.toString(), HttpMethod.POST, null);
        String result = response.readEntity(String.class);
        int statusCode = response.getStatus();
        if ( statusCode == 201 ) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                ctfArtifact = new CTFArtifact(this, data);
            } catch (ParseException e) {
                logger.log(Level.WARNING,"Unable to parse the json content in createArtifact() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING,"Error creating an artifact - " + statusCode + ", Error Msg - " + result);
            throw new IOException("Error creating an artifact - " + statusCode + ", Error Msg - " + helper.getErrorMessage(result));
        }
        return ctfArtifact;
    }
}
