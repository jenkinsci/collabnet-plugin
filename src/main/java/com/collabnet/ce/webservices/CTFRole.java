package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.cemain.UserSoapList;
import com.collabnet.ce.soap50.webservices.cemain.UserSoapRow;
import com.collabnet.ce.soap50.webservices.rbac.RoleSoapDO;
import com.collabnet.ce.soap50.webservices.rbac.RoleSoapRow;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A role in CTF belongs to a project.
 *
 * @author Kohsuke Kawaguchi
 */
public class CTFRole extends CTFObject implements ObjectWithTitle {
    private final String title, description;

    public CTFRole(CTFProject parent, RoleSoapDO data) {
        super(parent, data.getId());
        this.title = data.getTitle();
        this.description = data.getDescription();
    }

    public CTFRole(CTFProject parent, RoleSoapRow data) {
        super(parent, data.getId());
        this.title = data.getTitle();
        this.description = data.getDescription();
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Gets the users who has this role (in the project that the role belongs to.)
     */
    public CTFList<CTFUser> getMembers() throws RemoteException {
        CTFList<CTFUser> r = new CTFList<CTFUser>();
        for (UserSoapRow row : app.getRbacAppSoap().getRoleMemberList(app.getSessionId(),getId()).getDataRows()) {
            r.add(new CTFUser(app,row));
        }
        return r;
    }

    /**
     * Grants this role to the given user.
     */
    public void grant(String username) throws RemoteException {
        app.getRbacAppSoap().addUser(app.getSessionId(), getId(), username);
    }

    public void grant(CTFUser u) throws RemoteException {
        grant(u.getUserName());
    }
}
