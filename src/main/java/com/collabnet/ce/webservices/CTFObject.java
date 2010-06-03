package com.collabnet.ce.webservices;

/**
 * @author Kohsuke Kawaguchi
 */
abstract class CTFObject {
    protected final CollabNetApp app;

    protected final String id;

    protected CTFObject(CollabNetApp app, String id) {
        this.app = app;
        this.id = id;
    }

    protected CTFObject(CTFObject parent, String id) {
        this.app = parent.app;
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
