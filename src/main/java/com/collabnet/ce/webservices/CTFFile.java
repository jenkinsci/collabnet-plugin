package com.collabnet.ce.webservices;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * A file in the file storage.
 *
 * <p>
 * In CTF, file objects are tied to a specific SOAP session.
 *
 * @author Kohsuke Kawaguchi
 * @see CollabNetApp#upload(File) 
 */
public class CTFFile extends CTFObject {
    public CTFFile(CollabNetApp app, String id) {
        super(app, id);
    }

    /**
     * Retrieves the file.
     */
    public InputStream download() throws IOException {
        return app.getFileStorageAppSoap().downloadFile(app.getSessionId(),getId()).getInputStream();
    }
}
