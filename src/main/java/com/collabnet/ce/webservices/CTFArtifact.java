package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.types.SoapFieldValues;
import com.collabnet.ce.soap50.webservices.tracker.ArtifactSoapDO;
import com.collabnet.ce.soap50.webservices.tracker.ArtifactSoapRow;

import java.rmi.RemoteException;
import java.util.Date;

/**
 * @author Kohsuke Kawaguchi
 */
public class CTFArtifact extends CTFObject {
    private ArtifactSoapDO data;
    /**
     * If true, we need to fetch the full {@link ArtfifactSoapDO} before we update.
     */
    private boolean needsRefill;

    CTFArtifact(CTFObject parent, ArtifactSoapRow src) {
        super(parent, src.getId());
        needsRefill = true;
        this.data = new ArtifactSoapDO();
        data.setActualHours(src.getActualHours());
        data.setAssignedTo(src.getAssignedToUsername());
        data.setCategory(src.getCategory());
        data.setCloseDate(src.getCloseDate());
        data.setCreatedBy(src.getSubmittedByUsername());
        data.setCreatedDate(src.getSubmittedDate());
        data.setCustomer(src.getCustomer());
        data.setDescription(src.getDescription());
        data.setEstimatedHours(src.getEstimatedHours());
        data.setFolderId(src.getFolderId());
        data.setGroup(src.getArtifactGroup());
        data.setId(src.getId());
        data.setLastModifiedDate(src.getLastModifiedDate());
        data.setPriority(src.getPriority());
        data.setStatus(src.getStatus());
        data.setStatusClass(src.getStatusClass());
    }

    CTFArtifact(CTFObject parent, ArtifactSoapDO data) {
        super(parent, data.getId());
        this.data = data;
    }

    /**
     * Obtains all the fields, not just those ones that are made available to us during the search.
     */
    public void refill() throws RemoteException {
        data = app.getTrackerSoap().getArtifactData(app.getSessionId(),getId());
        needsRefill = false;
    }

    public String getURL() {
        return app.getServerUrl() + "/sf/go/" + getId();

    }

    public void update(String comment) throws RemoteException {
        update(comment,null,null,null);
    }

    public void update(String comment, String fileName, String fileMimeType, CTFFile file) throws RemoteException {
        if (needsRefill)
            throw new IllegalStateException("CTFArtifact needs to be filled before it can be updated");
        app.getTrackerSoap().setArtifactData(app.getSessionId(), data, comment, fileName, fileMimeType, file!=null?file.getId():null);
    }

    public String getDescription() {
        return data.getDescription();
    }

    public void setDescription(String description) {
        data.setDescription(description);
    }

    public String getCategory() {
        return data.getCategory();
    }

    public void setCategory(String category) {
        data.setCategory(category);
    }

    public String getGroup() {
        return data.getGroup();
    }

    public void setGroup(String group) {
        data.setGroup(group);
    }

    public String getStatus() {
        return data.getStatus();
    }

    public void setStatus(String status) {
        data.setStatus(status);
    }

    public String getStatusClass() {
        return data.getStatusClass();
    }

    public void setStatusClass(String statusClass) {
        data.setStatusClass(statusClass);
    }

    public String getCustomer() {
        return data.getCustomer();
    }

    public void setCustomer(String customer) {
        data.setCustomer(customer);
    }

    public int getPriority() {
        return data.getPriority();
    }

    public void setPriority(int priority) {
        data.setPriority(priority);
    }

    public int getEstimatedHours() {
        return data.getEstimatedHours();
    }

    public void setEstimatedHours(int estimatedHours) {
        data.setEstimatedHours(estimatedHours);
    }

    public int getActualHours() {
        return data.getActualHours();
    }

    public void setActualHours(int actualHours) {
        data.setActualHours(actualHours);
    }

    public Date getCloseDate() {
        return data.getCloseDate();
    }

    public void setCloseDate(Date closeDate) {
        data.setCloseDate(closeDate);
    }

    public String getAssignedTo() {
        return data.getAssignedTo();
    }

    public void setAssignedTo(String assignedTo) {
        data.setAssignedTo(assignedTo);
    }

    public String getReportedReleaseId() {
        return data.getReportedReleaseId();
    }

    public void setReportedReleaseId(String reportedReleaseId) {
        data.setReportedReleaseId(reportedReleaseId);
    }

    public String getResolvedReleaseId() {
        return data.getResolvedReleaseId();
    }

    public void setResolvedReleaseId(String resolvedReleaseId) {
        data.setResolvedReleaseId(resolvedReleaseId);
    }

    public SoapFieldValues getFlexFields() {
        return data.getFlexFields();
    }

    public void setFlexFields(SoapFieldValues flexFields) {
        data.setFlexFields(flexFields);
    }

    public String getPath() {
        return data.getPath();
    }

    public String getTitle() {
        return data.getTitle();
    }

    public void setTitle(String title) {
        data.setTitle(title);
    }

    public String getFolderId() {
        return data.getFolderId();
    }

    public void setFolderId(String folderId) {
        data.setFolderId(folderId);
    }

    public int getVersion() {
        return data.getVersion();
    }

    public String getCreatedBy() {
        return data.getCreatedBy();
    }

    public String getLastModifiedBy() {
        return data.getLastModifiedBy();
    }

    public Date getCreatedDate() {
        return data.getCreatedDate();
    }

    public Date getLastModifiedDate() {
        return data.getLastModifiedDate();
    }
}
