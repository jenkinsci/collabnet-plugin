package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.cemain.FolderSoapDO;
import com.collabnet.ce.soap50.webservices.cemain.FolderSoapRow;

import java.rmi.RemoteException;

/**
 * Folder-like container object.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class CTFFolder extends CTFObject implements ObjectWithTitle {
    private String id;
    private String projectId;
    private String parentFolderId;
    private String path;
    private String title;
    private String description;

    protected CTFFolder(CTFObject parent, FolderSoapDO data) {
        super(parent,data.getId());
        this.id = data.getId();
        this.projectId = data.getProjectId();
        this.parentFolderId = data.getParentFolderId();
        this.path = data.getPath();
        this.title = data.getTitle();
        this.description = data.getDescription();
    }

    protected CTFFolder(CTFObject parent, FolderSoapRow data) {
        super(parent,data.getId());
        this.id = data.getId();
        this.projectId = data.getProjectId();
        this.parentFolderId = data.getParentFolderId();
        this.path = data.getPath();
        this.title = data.getTitle();
        this.description = data.getDescription();
    }

    public String getId() {
        return id;
    }

    /**
     * Gets the project that this belongs to.
     */
    public CTFProject getProject() throws RemoteException {
        return app.getProjectById(projectId);
    }

    public CTFFolder getParentFolder() {
//        return app.getFolderById(parentFolderId);
        throw new UnsupportedOperationException();
    }

    public String getPath() {
        return path;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
