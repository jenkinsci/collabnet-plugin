package com.collabnet.ce.webservices;

import hudson.plugins.collabnet.util.Helper;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.apache.http.client.utils.DateUtils;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kohsuke Kawaguchi
 */
public class CTFArtifact extends CTFObject {

    static Logger logger = Logger.getLogger(CTFArtifact.class.getName());
    Helper helper = new Helper();
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
        actualEffort = getIntValue(artifactData.get("actualEffort"));
        estimatedEffort = getIntValue(artifactData.get("estimatedEffort"));
        assignedTo = getStringValue(artifactData.get("assignedTo"));
        category = getStringValue(artifactData.get("category"));
        closeDate = getDateValue(artifactData.get("closeDate"));
        createdBy = getStringValue(artifactData.get("createdBy"));
        createdDate = getDateValue(artifactData.get("createdDate"));
        customer = getStringValue(artifactData.get("customer"));
        description = getStringValue(artifactData.get("description"));
        title  = getStringValue(artifactData.get("title"));
        folderId = getStringValue(artifactData.get("folderId"));
        group = getStringValue(artifactData.get("group"));
        id = getStringValue(artifactData.get("id"));
        lastModifiedDate = getDateValue(artifactData.get("lastModifiedDate"));
        priority = getIntValue(artifactData.get("priority"));
        status = getStringValue(artifactData.get("status"));
        statusClass = getStringValue(artifactData.get("statusClass"));
        path = getStringValue(artifactData.get("path"));
    }

    /**
     * Obtains all the fields, not just those ones that are made available to us during the search.
     */
    public void refill() throws IOException {
        String end_point =  app.getServerUrl() + CTFConstants.ARTIFACT_URL + getId();
        Response response = helper.request(end_point, app.getSessionId(), null, HttpMethod.GET, null);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status == 200) {
            JSONObject artfData = null;
            try {
                artfData = (JSONObject) new JSONParser().parse(result);
                setExistingArtfData(artfData);
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in setArtifact() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING, "Error updating the artifact details - " + status  + ", Error Msg - " + result);
            throw new IOException("Error updating the artifact details - " + status + ", Error Msg - " + helper.getErrorMessage(result));
        }
        needsRefill = false;
    }

    public void setExistingArtfData(JSONObject currArtfData) {
        actualEffort = getIntValue(currArtfData.get("actualEffort"));
        estimatedEffort = getIntValue(currArtfData.get("estimatedEffort"));
        assignedTo = getStringValue(currArtfData.get("assignedTo"));
        category = getStringValue(currArtfData.get("category"));
        closeDate = getDateValue(currArtfData.get("closeDate"));
        createdBy = getStringValue(currArtfData.get("createdBy"));
        createdDate = getDateValue(currArtfData.get("createdDate"));
        customer = getStringValue(currArtfData.get("customer"));
        description = getStringValue(currArtfData.get("description"));
        folderId = getStringValue(currArtfData.get("folderId"));
        group = getStringValue(currArtfData.get("group"));
        id = getStringValue(currArtfData.get("id"));
        lastModifiedDate = getDateValue(currArtfData.get("lastModifiedDate"));
        priority = getIntValue(currArtfData.get("priority"));
        status = getStringValue(currArtfData.get("status"));
        statusClass = getStringValue(currArtfData.get("statusClass"));
        path = getStringValue(currArtfData.get("path"));
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
        String end_point =  app.getServerUrl() + CTFConstants.ARTIFACT_URL + getId();
        JSONObject attachmentObj = new JSONObject();
        attachmentObj.put("fileName",fileName);
        attachmentObj.put("mimeType", fileMimeType);
        attachmentObj.put("fileId", file!=null?file.getId():null);
        JSONArray attachArray = new JSONArray();
        attachArray.add(attachmentObj);
        JSONObject requestPayload = new JSONObject();
        requestPayload.put("comment", comment);
        requestPayload.put("attachments", attachArray);
        requestPayload.put("status", getStatus());
        Response response = helper.request(end_point, app.getSessionId(), requestPayload.toString(), HttpMethod.PATCH, null);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status == 200) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
            } catch (ParseException e) {
                logger.log(Level.WARNING,"Unable to parse the json content in update()");
            }
        } else {
            logger.log(Level.WARNING,"Updating the artifact data failed - " + status);
            throw new IOException("Error updating the artifact data failed - " + status + ", Error Msg - " + helper.getErrorMessage(result));
        }
    }

    private String getStringValue(Object value) {
        return value == null ?  null : value.toString();
    }

    private int getIntValue(Object value) {
        return value == null ?  0 : Integer.parseInt(value.toString());
    }

    private Date getDateValue(Object value) {
        return value == null ? null : DateUtils.parseDate(value.toString());
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
