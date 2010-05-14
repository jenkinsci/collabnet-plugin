package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.ClientSoapStubFactory;

import com.collabnet.ce.soap50.webservices.filestorage.IFileStorageAppSoap;

import java.io.File;
import java.rmi.RemoteException;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

/**
 * Class to hold the file-related methods.
 * Wraps a CollabNetApp.
 */
public class FileStorageApp extends AbstractSoapApp {
    private IFileStorageAppSoap ifsa;

    /**
     * Construct a new FileStorageApp.
     *
     * @param collabNetApp a valid (logged-in) collabNetApp.
     */
    public FileStorageApp(CollabNetApp collabNetApp) {
        super(collabNetApp);
        this.ifsa = this.getIFileStorageAppSoap();
    }

    /**
     * @return a Client Soap stub for the SimpleFileStorageApp.
     */
    private IFileStorageAppSoap getIFileStorageAppSoap() {
        String soapURL = this.getServerUrl() + CollabNetApp.SOAP_SERVICE +
            "FileStorageApp?wsdl";
        return (IFileStorageAppSoap) ClientSoapStubFactory.
            getSoapStub(IFileStorageAppSoap.class, soapURL);
    }

    /**
     * Upload a file to the server.
     *
     * @param file to upload.
     * @return the fileId associated with the uploaded file.
     * @throws RemoteException if something fails
     */
    public String uploadFile(File file) throws RemoteException {
        FileDataSource fds = new FileDataSource(file);
        DataHandler dh = new DataHandler(fds);
        String fileId = this.ifsa.uploadFile(this.getSessionId(), dh);
        return fileId;
    }
}
