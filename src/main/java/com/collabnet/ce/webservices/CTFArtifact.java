package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.tracker.ArtifactSoapDO;
import com.collabnet.ce.soap50.webservices.tracker.ArtifactSoapRow;

import java.util.Date;

/**
 * @author Kohsuke Kawaguchi
 */
public class CTFArtifact extends CTFObject {
    private int priority;
    private java.lang.String projectPathString;
    private java.lang.String projectTitle;
    private java.lang.String projectId;
    private java.lang.String folderPathString;
    private java.lang.String folderTitle;
    private java.lang.String folderId;
    private java.lang.String title;
    private java.lang.String description;
    private java.lang.String artifactGroup;
    private java.lang.String status;
    private java.lang.String statusClass;
    private java.lang.String category;
    private java.lang.String customer;
    private java.lang.String submittedByUsername;
    private java.lang.String submittedByFullname;
    private java.util.Date submittedDate;
    private java.util.Date closeDate;
    private java.lang.String assignedToUsername;
    private java.lang.String assignedToFullname;
    private java.util.Date lastModifiedDate;
    private int estimatedHours;
    private int actualHours;

    CTFArtifact(CTFObject parent, ArtifactSoapRow data) {
        super(parent, data.getId());
        this.priority = data.getPriority();
        this.projectPathString = data.getProjectPathString();
        this.projectTitle = data.getProjectTitle();
        this.projectId = data.getProjectId();
        this.folderPathString = data.getFolderPathString();
        this.folderTitle = data.getFolderTitle();
        this.folderId = data.getFolderId();
        this.title = data.getTitle();
        this.description = data.getDescription();
        this.artifactGroup = data.getArtifactGroup();
        this.status = data.getStatus();
        this.statusClass = data.getStatusClass();
        this.category = data.getCategory();
        this.customer = data.getCustomer();
        this.submittedByUsername = data.getSubmittedByUsername();
        this.submittedByFullname = data.getSubmittedByFullname();
        this.submittedDate = data.getSubmittedDate();
        this.closeDate = data.getCloseDate();
        this.assignedToUsername = data.getAssignedToUsername();
        this.assignedToFullname = data.getAssignedToFullname();
        this.lastModifiedDate = data.getLastModifiedDate();
        this.estimatedHours = data.getEstimatedHours();
        this.actualHours = data.getActualHours();
    }

    CTFArtifact(CTFObject parent, ArtifactSoapDO data) {
        super(parent, data.getId());
        this.priority = data.getPriority();
        this.folderId = data.getFolderId();
        this.title = data.getTitle();
        this.description = data.getDescription();
        this.artifactGroup = data.getGroup();
        this.status = data.getStatus();
        this.statusClass = data.getStatusClass();
        this.category = data.getCategory();
        this.customer = data.getCustomer();
        this.submittedByUsername = data.getCreatedBy();
        this.closeDate = data.getCloseDate();
        this.assignedToUsername = data.getAssignedTo();
        this.lastModifiedDate = data.getLastModifiedDate();
        this.estimatedHours = data.getEstimatedHours();
        this.actualHours = data.getActualHours();
    }

    public int getPriority() {
        return priority;
    }

    public String getProjectPathString() {
        return projectPathString;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getFolderPathString() {
        return folderPathString;
    }

    public String getFolderTitle() {
        return folderTitle;
    }

    public String getFolderId() {
        return folderId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getArtifactGroup() {
        return artifactGroup;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusClass() {
        return statusClass;
    }

    public String getCategory() {
        return category;
    }

    public String getCustomer() {
        return customer;
    }

    public String getSubmittedByUsername() {
        return submittedByUsername;
    }

    public String getSubmittedByFullname() {
        return submittedByFullname;
    }

    public Date getSubmittedDate() {
        return submittedDate;
    }

    public Date getCloseDate() {
        return closeDate;
    }

    public String getAssignedToUsername() {
        return assignedToUsername;
    }

    public String getAssignedToFullname() {
        return assignedToFullname;
    }

    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public int getEstimatedHours() {
        return estimatedHours;
    }

    public int getActualHours() {
        return actualHours;
    }
}
