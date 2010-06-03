package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.docman.DocumentFolderSoapDO;
import com.collabnet.ce.soap50.webservices.docman.DocumentFolderSoapList;
import com.collabnet.ce.soap50.webservices.docman.DocumentFolderSoapRow;
import com.collabnet.ce.soap50.webservices.docman.DocumentSoapList;
import com.collabnet.ce.soap50.webservices.docman.DocumentSoapRow;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * A folder in the documents section.
 * @author Kohsuke Kawaguchi
 */
public class CTFDocumentFolder extends CTFFolder {
    CTFDocumentFolder(CTFObject parent, DocumentFolderSoapDO data) {
        super(parent, data);
    }

    CTFDocumentFolder(CTFObject parent, DocumentFolderSoapRow data) {
        super(parent, data);
    }

    public List<CTFDocumentFolder> getFolders() throws RemoteException {
        List<CTFDocumentFolder> r = new ArrayList<CTFDocumentFolder>();

        DocumentFolderSoapList dfsList = app.getDocumentAppSoap().getDocumentFolderList(app.getSessionId(),getId(),false);
        for (DocumentFolderSoapRow row : dfsList.getDataRows())
            r.add(new CTFDocumentFolder(this,row));
        return r;
    }

    public CTFDocumentFolder getFolderByTitle(String title) throws RemoteException {
        return findByTitle(getFolders(),title);
    }

    public CTFDocumentFolder createFolder(String title, String description) throws RemoteException {
        return new CTFDocumentFolder(this,app.getDocumentAppSoap().createDocumentFolder(
                app.getSessionId(), getId(), title, description));
    }

    public List<CTFDocument> getDocuments() throws RemoteException {
        DocumentSoapList list = app.getDocumentAppSoap().getDocumentList(app.getSessionId(), getId(), null);

        List<CTFDocument> r = new ArrayList<CTFDocument>();
        for (DocumentSoapRow row : list.getDataRows())
            r.add(new CTFDocument(this,row));
        return r;
    }

    public CTFDocument getDocumentByTitle(String title) throws RemoteException {
        return findByTitle(getDocuments(),title);
    }

    public CTFDocument createDocument(java.lang.String title,
                              java.lang.String description,
                              java.lang.String versionComment,
                              java.lang.String status,
                              boolean createLocked,
                              java.lang.String fileName,
                              java.lang.String mimeType,
                              CTFFile file,
                              java.lang.String associationId,
                              java.lang.String associationDesc) throws RemoteException {
        return new CTFDocument(this,
            app.getDocumentAppSoap().createDocument(app.getSessionId(), getId(), title, description,
                    versionComment, status, createLocked, fileName, mimeType, file.getId(), associationId, associationDesc));
    }

    public String getURL() {
        return app.getServerUrl() + "/sf/docman/do/listDocuments/" + getPath();
    }


}
