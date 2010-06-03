package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.cemain.ItemSoapDO;
import com.collabnet.ce.soap50.webservices.frs.FrsFileSoapDO;
import com.collabnet.ce.soap50.webservices.frs.FrsFileSoapRow;

import java.rmi.RemoteException;

/**
 * @author Kohsuke Kawaguchi
 */
public class CTFReleaseFile extends CTFItem {
    private final String description, mimeType, filename;
    private final long size;

    protected CTFReleaseFile(CTFObject parent, FrsFileSoapDO data) {
        super(parent,data);
        this.description = data.getDescription();
        this.mimeType = data.getMimeType();
        this.filename = data.getFilename();
        this.size = data.getSize();
    }

    protected CTFReleaseFile(CTFObject parent, FrsFileSoapRow data) {
        super(parent, toItemSoapDO(data));
        this.description = data.getDescription();
        this.mimeType = data.getMimeType();
        this.filename = data.getFilename();
        this.size = data.getFileSize();
    }

    private static ItemSoapDO toItemSoapDO(FrsFileSoapRow data) {
        ItemSoapDO r = new ItemSoapDO();
        r.setTitle(data.getTitle());
        r.setId(data.getId());
        // not sure how the rest of the parameters match up
        return r;
    }

    public String getDescription() {
        return description;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFilename() {
        return filename;
    }

    public long getSize() {
        return size;
    }

    public String getURL() {
        return app.getServerUrl() + "/sf/frs/do/downloadFile/" + getPath();
    }

    public void delete() throws RemoteException {
        app.getFrsAppSoap().deleteFrsFile(app.getSessionId(),getId());
    }
}
