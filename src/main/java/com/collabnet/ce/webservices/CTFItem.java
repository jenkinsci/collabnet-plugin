package com.collabnet.ce.webservices;

import net.sf.json.JSONObject;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class CTFItem extends CTFObject implements ObjectWithTitle {
    private final String path;
    private final String title;
    private final String folder;

    protected CTFItem(CTFObject parent, JSONObject data) {
        super(parent,data.get("id").toString());
        this.path = data.get("folderPath") == null ? data.get("path").toString():data.get("folderPath").toString();
        this.title = data.get("title").toString();
        this.folder = data.get("folderId") == null ? null:data.get("folderId").toString();
    }

    public String getPath() {
        return path;
    }

    public String getTitle() {
        return title;
    }

    public String getFolder() {
        return folder;
    }
}
