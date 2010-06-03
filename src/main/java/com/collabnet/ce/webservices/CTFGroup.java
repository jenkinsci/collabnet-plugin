package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.cemain.Group2SoapRow;

/**
 * @author Kohsuke Kawaguchi
 */
public class CTFGroup extends CTFObject implements ObjectWithTitle {
    private final String fullName, description;
    CTFGroup(CollabNetApp app, Group2SoapRow data) {
        super(app,data.getId());
        this.fullName = data.getFullName();
        this.description = data.getDescription();
    }

    public String getFullName() {
        return fullName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Alias for {@link #getFullName()}.
     */
    @Override
    public String getTitle() {
        return getFullName();
    }
}
