package com.collabnet.ce.webservices;

import hudson.plugins.collabnet.util.CNFormFieldValidator;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kohsuke Kawaguchi
 */
public class CTFArtifact extends CTFObject {

    private static final String ARTIFACT_URL = "/ctfrest/tracker/v2/artifacts/";

    static Logger logger = Logger.getLogger(CTFArtifact.class.getName());

    public String title;
    public String id;
    public String path;
    public String description;
    public String category;
    public String group;
    public String status;
    public String statusClass;
    public String customer;
    public String folderId;
    public int priority;
    public int estimatedEffort;
    public int actualEffort;
    public int remainingEffort;
    public int points;
    public int version;
    public Date closeDate;
    public Date createdDate;
    public Date lastModifiedDate;
    public String assignedTo;
    public String createdBy;
    public String lastModifiedBy;
    public String reportedReleaseId;
    public String resolvedReleaseId;
    public String planningFolderId;
    public boolean autosumming;
    public CTFFlexField[] flexFields;

    public int getEstimatedEffort() {
        return estimatedEffort;
    }

    public void setEstimatedEffort(int estimatedEffort) {
        this.estimatedEffort = estimatedEffort;
    }

    public int getRemainingEffort() {
        return remainingEffort;
    }

    public void setRemainingEffort(int remainingEffort) {
        this.remainingEffort = remainingEffort;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public String getPlanningFolderId() {
        return planningFolderId;
    }

    public void setPlanningFolderId(String planningFolderId) {
        this.planningFolderId = planningFolderId;
    }

    public boolean isAutosumming() {
        return autosumming;
    }

    public void setAutosumming(boolean autosumming) {
        this.autosumming = autosumming;
    }

    /**
     * If true, we need to fetch the full artifact details before we update.
     */
    private boolean needsRefill;

    CTFArtifact(CTFObject parent, JSONObject artifactData) {
        super(parent, artifactData.get("id").toString());
        needsRefill = true;
        actualEffort = Integer.parseInt(artifactData.get("actualEffort").toString());
        estimatedEffort = Integer.parseInt(artifactData.get("estimatedEffort").toString());
        assignedTo = artifactData.get("assignedTo") == null ? null : artifactData.get("assignedTo").toString();
        category = artifactData.get("category").toString();
        closeDate = DateUtils.parseDate(artifactData.get("closeDate").toString());
        createdBy = artifactData.get("createdBy").toString();
        createdDate = DateUtils.parseDate(artifactData.get("createdDate").toString());
        customer = artifactData.get("customer").toString();
        description = artifactData.get("description").toString();
        folderId = artifactData.get("folderId").toString();
        group = artifactData.get("group").toString();
        id = artifactData.get("id").toString();
        lastModifiedDate = DateUtils.parseDate(artifactData.get("lastModifiedDate").toString());
        priority = Integer.parseInt(artifactData.get("priority").toString());
        status = artifactData.get("status").toString();
        statusClass = artifactData.get("statusClass").toString();
        path = artifactData.get("path").toString();
    }

    /**
     * Obtains all the fields, not just those ones that are made available to us during the search.
     */
    public void refill() throws IOException {
        String end_point =  app.getServerUrl() + ARTIFACT_URL + getId();
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpGet get = new HttpGet(end_point);
            get.setHeader("Accept", "application/json");
            get.setHeader("Authorization", "Bearer " + app.getSessionId());
            response = httpClient.execute(get);
            JSONObject artfData = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
            setExistingArtfData(artfData);
        } catch (Exception e) {
            logger.log(Level.INFO,"Getting the artifact details failed - " + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        }
        needsRefill = false;
    }

    public void setExistingArtfData(JSONObject currArtfData) {
        actualEffort = Integer.parseInt(currArtfData.get("actualEffort").toString());
        estimatedEffort = Integer.parseInt(currArtfData.get("estimatedEffort").toString());
        assignedTo = currArtfData.get("assignedTo") == null ? null : currArtfData.get("assignedTo").toString();
        category = currArtfData.get("category").toString();
        closeDate = DateUtils.parseDate(currArtfData.get("closeDate").toString());
        createdBy = currArtfData.get("createdBy").toString();
        createdDate = DateUtils.parseDate(currArtfData.get("createdDate").toString());
        customer = currArtfData.get("customer").toString();
        description = currArtfData.get("description").toString();
        folderId = currArtfData.get("folderId").toString();
        group = currArtfData.get("group").toString();
        id = currArtfData.get("id").toString();
        lastModifiedDate = DateUtils.parseDate(currArtfData.get("lastModifiedDate").toString());
        priority = Integer.parseInt(currArtfData.get("priority").toString());
        status = currArtfData.get("status").toString();
        statusClass = currArtfData.get("statusClass").toString();
        path = currArtfData.get("path").toString();
    }

    public String getURL() {
        return app.getServerUrl() + "/sf/go/" + getId();

    }

    public void update(String comment) throws IOException {
        update(comment,null,null,null);
    }

    public void update(String comment, String fileName, String fileMimeType, CTFFile file) throws IOException {
        if (needsRefill)
            throw new IllegalStateException("CTFArtifact needs to be filled before it can be updated");
        String end_point =  app.getServerUrl() + ARTIFACT_URL + getId();
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpPatch patch = new HttpPatch(end_point);
            patch.setHeader("Accept", "application/json");
            patch.setHeader("Content-Type", "application/json");
            patch.setHeader("If-Match", "W/*");
            patch.setHeader("Authorization", "Bearer " + app.getSessionId());
            JSONObject attachmentObj = new JSONObject()
                    .element("fileName",fileName)
                    .element("mimeType", fileMimeType)
                    .element("fileId", file!=null?file.getId():null);
            JSONArray attachArray = new JSONArray();
            attachArray.add(attachmentObj);
            JSONObject artfObj = new JSONObject().
                    element("comment", comment).
                    element("attachments", attachArray).
                    element("status", getStatus());
            StringEntity stringEntity = new StringEntity(artfObj.toString(), ContentType.APPLICATION_JSON);
            patch.setEntity(stringEntity);
            response = httpClient.execute(patch);
        } catch (Exception e) {
            logger.log(Level.INFO,"Updating the artifact data failed - " + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        }
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return this.category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getGroup() {
        return this.group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusClass() {
        return this.statusClass;
    }

    public void setStatusClass(String statusClass) {
        this.statusClass = statusClass;
    }

    public String getCustomer() {
        return this.customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getEstimatedHours() {
        return this.estimatedEffort;
    }

    public void setEstimatedHours(int estimatedHours) {
        this.estimatedEffort = estimatedHours;
    }

    public int getActualHours() {
        return this.actualEffort;
    }

    public void setActualHours(int actualHours) {
        this.actualEffort = actualHours;
    }

    public Date getCloseDate() {
        return this.closeDate;
    }

    public void setCloseDate(Date closeDate) {
        this.closeDate = closeDate;
    }

    public String getAssignedTo() {
        return this.assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getReportedReleaseId() {
        return this.reportedReleaseId;
    }

    public void setReportedReleaseId(String reportedReleaseId) {
        this.reportedReleaseId = reportedReleaseId;
    }

    public String getResolvedReleaseId() {
        return this. resolvedReleaseId;
    }

    public void setResolvedReleaseId(String resolvedReleaseId) {
        this.resolvedReleaseId = resolvedReleaseId;
    }

    public CTFFlexField[] getFlexFields() {
        return this.flexFields;
    }

    public void setFlexFields(CTFFlexField[] flexFields) {
        this.flexFields = flexFields;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFolderId() {
        return this.folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public int getVersion() {
        return this.version;
    }

    public String getCreatedBy() {
        return this.createdBy;
    }

    public String getLastModifiedBy() {
        return this.lastModifiedBy;
    }

    public Date getCreatedDate() {
        return this.createdDate;
    }

    public Date getLastModifiedDate() {
        return this.lastModifiedDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
