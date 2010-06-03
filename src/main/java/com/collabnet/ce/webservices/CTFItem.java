package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.cemain.ItemSoapDO;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class CTFItem extends CTFObject implements ObjectWithTitle {
    private final String path, title, folder;

    protected CTFItem(CTFObject parent, ItemSoapDO data) {
        super(parent,data.getId());
        this.path = data.getPath();
        this.title = data.getTitle();
        this.folder = data.getFolderId();
    }

    public String getPath() {
        return path;
    }

    public String getTitle() {
        return title;
    }
}
