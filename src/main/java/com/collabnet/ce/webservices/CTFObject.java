package com.collabnet.ce.webservices;

import java.util.List;

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

    /**
     * Convenience method for a subtype to find an item in a collection by its title.
     */
    protected <T extends ObjectWithTitle> T findByTitle(List<T> list, String title) {
        for (T p : list)
            if (p.getTitle().equals(title))
                return p;
        return null;
    }
}
