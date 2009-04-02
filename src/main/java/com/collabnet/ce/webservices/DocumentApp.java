package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.ClientSoapStubFactory;

import com.collabnet.ce.soap50.webservices.docman.IDocumentAppSoap;
import com.collabnet.ce.soap50.webservices.docman.DocumentFolderSoapDO;
import com.collabnet.ce.soap50.webservices.docman.DocumentFolderSoapList;
import com.collabnet.ce.soap50.webservices.docman.DocumentFolderSoapRow;
import com.collabnet.ce.soap50.webservices.docman.DocumentSoapDO;
import com.collabnet.ce.soap50.webservices.docman.DocumentSoapList;
import com.collabnet.ce.soap50.webservices.docman.DocumentSoapRow;

import com.collabnet.ce.soap50.types.SoapFilter;

import java.rmi.RemoteException;
import java.util.Date;

/**
 * Class to hold the document-related methods.
 * Wraps a collabNetApp.
 */
public class DocumentApp {
    private CollabNetApp collabNetApp;
    private IDocumentAppSoap da;

    /**
     * Construct a new DocumentApp.
     *
     * @param collabNetApp a valid (logged-in) collabNetApp.
     */
    public DocumentApp(CollabNetApp collabNetApp) {
        this.collabNetApp = collabNetApp;
        this.da = this.getDocumentAppSoap();
    }

    /**
     * @return a Client Soap stub for the DocumentApp.
     */
    private IDocumentAppSoap getDocumentAppSoap() {
        String soapURL = this.getUrl() + CollabNetApp.SOAP_SERVICE +
            "DocumentApp?wsdl";
        return (IDocumentAppSoap) ClientSoapStubFactory.
            getSoapStub(IDocumentAppSoap.class, soapURL);
    }

    /**
     * Find or create a document folder path.
     *
     * @param projectId to get the root folder for the project.
     * @param documentPath string with folders separated by '/'.  The path
     *                     may or may not include the root folder.
     * @return folder id for the found/created folder.
     * @throws RemoteException
     */
    public String findOrCreatePath(String projectId, String documentPath) 
        throws RemoteException {
        this.checkValidSessionId();
        String[] folderNames = documentPath.split("/");
        int i = 0;
        // find the root folder since the first document path may or may not
        // match this.
        DocumentFolderSoapRow rootRow = this.getRootFolder(projectId);
        if (rootRow.getTitle().equals(folderNames[i])) {
            i++;
        }
        String currentFolderId = rootRow.getId();
        String subFolderId = null;
        for (; i < folderNames.length; i++) {
            DocumentFolderSoapList dfsList = this.da.
                getDocumentFolderList(this.getSessionId(), currentFolderId, 
                                      false);
            String folderList = "";
            for (DocumentFolderSoapRow row: dfsList.getDataRows()) {
                folderList += row.getTitle() + ", ";
            }
            for (DocumentFolderSoapRow row: dfsList.getDataRows()) {
                if (row.getTitle().equals(folderNames[i])) {
                    subFolderId = row.getId();
                    break;
                }
            }
            if (subFolderId == null) {
                break;
            } else {
                currentFolderId = subFolderId;
                subFolderId = null;
                }
        }
        // create any missing folders
        for (; i < folderNames.length; i++) {
            DocumentFolderSoapDO dfsd = this.da.
                createDocumentFolder(this.getSessionId(), currentFolderId, 
                                     folderNames[i], "Hudson Document " +
                                     "creation of " + folderNames[i]);
            currentFolderId = dfsd.getId();
        }
        return currentFolderId;
    }

    /**
     * @param projectId
     * @return the folder row for the topmost folder.
     * @throws RemoteException
     */
    private DocumentFolderSoapRow getRootFolder(String projectId) 
        throws RemoteException {
        DocumentFolderSoapList dfsList = this.
            da.getDocumentFolderList(this.getSessionId(), projectId, 
                                     false);
        if (dfsList.getDataRows().length < 1) {
            throw new CollabNetApp.
                CollabNetAppException("getRootFolder for projectId " + 
                                      projectId + 
                                      " failed to find any folders");
        }
        else if (dfsList.getDataRows().length > 1) {
            StringBuffer rowNames = new StringBuffer("");
            for (DocumentFolderSoapRow row: dfsList.getDataRows()) {
                rowNames.append(row.getTitle() + ", ");
            }
            throw new CollabNetApp.
                CollabNetAppException("getRootFolder returned unexpected " +
                                      "number of folders: " + 
                                      rowNames.toString());
        } else {
            return dfsList.getDataRows()[0];
        }
    }

    /**
     * Verify a document folder path.  If at any point the folder
     * is missing, return the name of the first missing folder.
     *
     * @param projectId to get the root folder for the project.
     * @param documentPath string with folders separated by '/'.
     * @return the first missing folder, or null if all are found.
     * @throws RemoteException
     */
     public String verifyPath(String projectId, String documentPath) 
        throws RemoteException {
        this.checkValidSessionId();
        String[] folderNames = documentPath.split("/");
        int i = 0;
        DocumentFolderSoapRow rootRow = this.getRootFolder(projectId);
        if (rootRow.getTitle().equals(folderNames[i])) {
            i++;
        }
        String currentFolderId = rootRow.getId();
        String subFolderId = null;
        for (; i < folderNames.length; i++) {
            DocumentFolderSoapList dfsList = this.da.
                getDocumentFolderList(this.getSessionId(), currentFolderId, 
                                      false);
            for (DocumentFolderSoapRow row: dfsList.getDataRows()) {
                if (row.getTitle().equals(folderNames[i])) {
                    subFolderId = row.getId();
                    break;
                }
            }
            if (subFolderId == null) {
                return folderNames[i];
            } else {
                currentFolderId = subFolderId;
                subFolderId = null;
            }
        }
        return null;
     }

    /**
     * Find the url path for the folder.
     *
     * @param folderId
     * @return path to the folder.
     * @throws RemoteException
     */
    public String getFolderPath(String folderId) throws RemoteException {
        this.checkValidSessionId();
        DocumentFolderSoapDO folderSoap = this.da.
            getDocumentFolderData(this.getSessionId(), folderId);
        return folderSoap.getPath();
    }

    /**
     * Find the latest version of the document in the folder which has a 
     * matching title.
     *
     * @param folderId 
     * @param title
     * @return id for the document, if found.  Otherwise, null.
     */
    public String findDocumentId(String folderId, String title) 
        throws RemoteException {
        this.checkValidSessionId();
        // someday we'll do this with filters, but for now, they're not
        // supported :P
        //SoapFilter[] filters = new SoapFilter[1];
        //filters[0] = new SoapFilter("title", title);
        
        DocumentSoapList dsList = this.da.getDocumentList(this.getSessionId(), 
                                                          folderId, null);
        if (dsList.getDataRows().length < 1) {
            return null;
        }
        Date latest = null;
        String lastId = null;
        for (DocumentSoapRow row: dsList.getDataRows()) {
            if (row.getTitle().equals(title)) {
                if (latest == null) {
                    latest = row.getDateVersionCreated();
                    lastId = row.getId();
                    continue;
                }
                if (row.getDateVersionCreated().after(latest)) {
                    latest = row.getDateVersionCreated();
                    lastId = row.getId();
                }
            }
        }
        return lastId;
    }

    /**
     * Update document with new version.
     */ 
    public void updateDoc(String docId, String fileId) throws RemoteException {
        this.checkValidSessionId();
        DocumentSoapDO docData = this.da.getDocumentData(this.getSessionId(),
                                                         docId, 0);
        this.da.setDocumentData(this.getSessionId(), docData, fileId);
    }
    
    /**
     * Create a new document.
     *
     * @param parentId
     * @param title 
     * @param description
     * @param versionComment 
     * @param status
     * @param createLocked 
     * @param fileName
     * @param mimeType 
     * @param fileId
     * @param associationId 
     * @param associationDesc
     * @return newly created Document Soap DO.
     * @throws RemoteException
     */
    public DocumentSoapDO createDocument(String parentId, String title, 
                                         String description, 
                                         String versionComment, 
                                         String status, boolean createLocked, 
                                         String fileName, String mimeType, 
                                         String fileId, String associationId, 
                                         String associationDesc) 
        throws RemoteException{
        return this.da.createDocument(this.getSessionId(), parentId, title, 
                               description, versionComment, status, 
                               createLocked, fileName, mimeType, fileId, 
                               associationId, associationDesc);        
    } 

    /*************************************
     * Below are wrapping functions, to make the code neater and in case,
     * something changes.
     **************************************/

    private void checkValidSessionId() {
        this.collabNetApp.checkValidSessionId();
    }
    
    private String getSessionId() {
        return this.collabNetApp.getSessionId();
    }
    
    private String getUrl() {
        return this.collabNetApp.getServerUrl();
    }
}
