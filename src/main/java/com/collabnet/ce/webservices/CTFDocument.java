package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.cemain.ItemSoapDO;
import com.collabnet.ce.soap50.webservices.docman.DocumentSoapDO;
import com.collabnet.ce.soap50.webservices.docman.DocumentSoapRow;
import com.collabnet.ce.soap50.webservices.docman.IDocumentAppSoap;

import java.rmi.RemoteException;

/**
 * @author Kohsuke Kawaguchi
 */
public class CTFDocument extends CTFItem {
    private final String description;

    public CTFDocument(CTFObject parent, DocumentSoapRow data) {
        super(parent, toItemSoapDO(data));
        this.description = data.getDescription();
    }

    public CTFDocument(CTFObject parent, DocumentSoapDO data) {
        super(parent, data);
        this.description = data.getDescription();
    }

    private static ItemSoapDO toItemSoapDO(DocumentSoapRow data) {
        ItemSoapDO r = new ItemSoapDO();
        r.setTitle(data.getTitle());
        r.setId(data.getId());
        // not sure how the rest of the parameters match up
        return r;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Updates this document by a new file.
     */
    public void update(CTFFile file) throws RemoteException {
        IDocumentAppSoap soap = app.getDocumentAppSoap();
        DocumentSoapDO docData = soap.getDocumentData(app.getSessionId(), getId(), 0);
        docData.setCurrentVersion(docData.getLatestVersion()+1); // make the new version active
        soap.setDocumentData(app.getSessionId(), docData, file.getId());
    }

    /**
     * Get the document's URL.
     *
     * @return an absolute URL to the document.
     */
    public String getURL() {
        return app.getServerUrl() + "/sf/go/" + getId();
    }
}
