package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.ClientSoapStubFactory;

import com.collabnet.ce.soap50.webservices.filestorage.ISimpleFileStorageAppSoap;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.rmi.RemoteException;

/**
 * Class to hold the file-related methods.
 * Wraps a collabNetApp.
 */
public class SimpleFileStorageApp {
    private static int BYTE_SIZE = 1048576; // 1 MB
    private CollabNetApp collabNetApp;
    private ISimpleFileStorageAppSoap ifsa;
    
    /**
     * Construct a new SimpleFileStorageApp.
     *
     * @param collabNetApp a valid (logged-in) collabNetApp.
     */
    public SimpleFileStorageApp(CollabNetApp collabNetApp) {
        this.collabNetApp = collabNetApp;
        this.ifsa = this.getISimpleFileStorageAppSoap();
    }
    
    /**
     * @return a Client Soap stub for the SimpleFileStorageApp.
     */
    private ISimpleFileStorageAppSoap getISimpleFileStorageAppSoap() {
        String soapURL = this.getUrl() + CollabNetApp.SOAP_SERVICE +
            "SimpleFileStorageApp?wsdl";
        return (ISimpleFileStorageAppSoap) ClientSoapStubFactory.
            getSoapStub(ISimpleFileStorageAppSoap.class, soapURL);
    }
    
    /**
     * Upload a file to the server.
     *
     * @param file to upload.
     * @return the fileId associatd with the uploaded file.
     * @throws RemoteException.
     */
    public String uploadFile(File file) throws RemoteException {
        String id = null;    
        byte[] bytes = new byte[SimpleFileStorageApp.BYTE_SIZE];
        byte[] smallerBytes = null;
        int bytesRead = 0;
        FileInputStream istream = null;
        try {
            istream = new FileInputStream(file);
        } catch (FileNotFoundException fnfe) {
            throw new CollabNetApp.
                CollabNetAppException("uploadBuildLog failed due to " +
                                      "FileNotFoundException: " + 
                                      fnfe.getMessage());
        }
        try {
            id = this.ifsa.startFileUpload(this.getSessionId());
            while ((bytesRead = istream.read(bytes)) > 0) {
                /* ifsa.write doesn't manage the byte array size as
                 * separate data, so it will write the entire array,
                 * no matter how big.  So we check if the real data is
                 * smaller, and in that case, copy to a smaller array.
                 */
                if (bytesRead == SimpleFileStorageApp.BYTE_SIZE) {
                    this.ifsa.write(this.getSessionId(), id, bytes);
                } else {
                    smallerBytes = new byte[bytesRead];
                    System.arraycopy(bytes, 0, smallerBytes, 0, bytesRead);
                    this.ifsa.write(this.getSessionId(), id, smallerBytes);
                }
            }
            this.ifsa.endFileUpload(this.getSessionId(), id);
            istream.close();
        } catch (IOException ioe) { 
            throw new CollabNetApp.
                CollabNetAppException("uploadBuildLog failed due to " +
                                      "IOException: " + ioe.getMessage());
        }
        return id;
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
