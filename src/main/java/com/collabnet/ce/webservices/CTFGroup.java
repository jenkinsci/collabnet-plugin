package com.collabnet.ce.webservices;

import com.collabnet.ce.soap60.webservices.cemain.UserGroupSoapRow;
import com.collabnet.ce.soap60.webservices.cemain.UserGroupSoapDO;

import java.rmi.RemoteException;

/**
 * @author Kohsuke Kawaguchi
 */
public class CTFGroup extends CTFObject implements ObjectWithTitle {
    private final String fullName, description;

    CTFGroup(CollabNetApp app, UserGroupSoapRow data) {
        super(app,data.getId());
        this.fullName = data.getFullName();
        this.description = data.getDescription();
    }

    CTFGroup(CollabNetApp app, UserGroupSoapDO data) {
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
        app.icns.addUserGroupMember(app.getSessionId(),getId(),u.getUserName());
    }
}
