package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.cemain.Group2SoapRow;
import com.collabnet.ce.soap50.webservices.cemain.GroupSoapDO;

import java.rmi.RemoteException;

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

    CTFGroup(CollabNetApp app, GroupSoapDO data) {
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

    /**
     * Adds the user to the this group.
     */
    public void addMember(CTFUser u) throws RemoteException {
        app.icns.addGroupMember(app.getSessionId(),getId(),u.getUserName());
    }
}
