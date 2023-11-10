package com.collabnet.ce.webservices;

import org.json.simple.JSONObject;

import java.io.IOException;

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

    protected CTFFolder(CTFObject parent, JSONObject data, String id, String parentId) {
        super(parent, id);
        this.id = id;
        this.projectId = data.get("projectId").toString();
        this.parentFolderId = parentId;
        this.path = data.get("path").toString();
        this.title = data.get("title").toString();
        this.description = data.get("description")==null? null : data.get("description").toString();
    }

    public String getId() {
        return id;
    }

    /**
     * Gets the project that this belongs to.
     */
    public CTFProject getProject() throws IOException {
        return app.getProjectById(projectId);
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

    public String getParentFolderId() {
        return parentFolderId;
    }
}
