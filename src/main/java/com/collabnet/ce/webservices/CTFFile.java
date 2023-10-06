package com.collabnet.ce.webservices;

import java.io.File;

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
}
