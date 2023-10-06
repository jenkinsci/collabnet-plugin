package com.collabnet.ce.webservices;

import com.collabnet.ce.soap60.types.SoapFieldValues;
import hudson.plugins.collabnet.util.CNFormFieldValidator;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

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

    private static final String TRACKER_URL = "/ctfrest/tracker/v2/trackers/";

    static Logger logger = Logger.getLogger(CTFTracker.class.getName());

    CTFTracker(CTFObject parent, JSONObject data) {
        super(parent, data, data.get("trackerId").toString(), data.get("parentFolderId").toString());
        this.project = (CTFProject) parent;
    }

    public CTFProject getProject() {
        return project;
    }

    public List<CTFArtifact> getArtifactsByTitle(String title) throws IOException {

        List<CTFArtifact> r = new ArrayList<CTFArtifact>();
        String end_point =  app.getServerUrl() + TRACKER_URL + getId() + "/artifacts/filter";
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpPost post = new HttpPost(end_point);
            post.setHeader("Content-Type", "application/json");
            post.setHeader("Accept", "application/json");
            post.setHeader("Authorization", "Bearer " + app.getSessionId());
            JSONArray jsonArray = new JSONArray();
            JSONObject filterObject = new JSONObject();
            filterObject.put("column", "title");
            filterObject.put("type", "String");
            filterObject.put("value", title);
            jsonArray.add(filterObject);
            JSONObject inp = new JSONObject();
            inp.put("filter", jsonArray);
            inp.put("count", -1);
            StringEntity stringEntity = new StringEntity(inp.toString(), ContentType.APPLICATION_JSON);
            post.setEntity(stringEntity);
            response = httpClient.execute(post);
            JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
            JSONArray dataArray = JSONArray.fromObject(data.get("items"));
            Iterator it = dataArray.iterator();
            while (it.hasNext()) {
                JSONObject jsonObject = (JSONObject) it.next();
                r.add(new CTFArtifact(this, jsonObject));
            }
        } catch (Exception e) {
            logger.log(Level.INFO,"Get artifact details by title - " + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
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
     * @param fileId of the attachment (returned when attachment was uploaded).
     * @return the newly created ArtifactSoapDO.
     * @throws RemoteException
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
        String end_point =  app.getServerUrl() + TRACKER_URL + getId() + "/artifacts";
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpPost post = new HttpPost(end_point);
            post.setHeader("Accept", "application/json");
            post.setHeader("Content-type", "application/json");
            post.setHeader("Authorization", "Bearer " + app.getSessionId());
            JSONObject jsonObj = new JSONObject();
            if (file != null) {
                jsonObj.put("fileId", file.getId());
                jsonObj.put("fileName", fileName);
                jsonObj.put("mimeType", fileMimeType);
            }
            JSONArray attachArray = new JSONArray();
            attachArray.add(jsonObj);
            JSONObject artfObj = new JSONObject()
                    .element("title", title)
                    .element("description", description)
                    .element("status", status)
                    .element("assignedTo", assignTo)
                    .element("releaseId", releaseId)
                    .element("priority", String.valueOf(priority))
                    .element("attachments", attachArray);
            StringEntity stringEntity = new StringEntity(artfObj.toString(), ContentType.APPLICATION_JSON);
            post.setEntity(stringEntity);
            response = httpClient.execute(post);
            JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
            ctfArtifact = new CTFArtifact(this, data);
        } catch (Exception e) {
            logger.log(Level.INFO,"Error creating an artifact - " + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        }
        return ctfArtifact;
    }
}
