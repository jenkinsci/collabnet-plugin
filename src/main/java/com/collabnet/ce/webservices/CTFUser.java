package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.cemain.GroupSoapList;
import com.collabnet.ce.soap50.webservices.cemain.GroupSoapRow;
import com.collabnet.ce.soap50.webservices.cemain.ProjectMemberSoapRow;
import com.collabnet.ce.soap50.webservices.cemain.UserSoapDO;
import com.collabnet.ce.soap50.webservices.cemain.UserSoapRow;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Kohsuke Kawaguchi
 */
public class CTFUser extends CTFObject implements ObjectWithTitle {
    private String userName;
    private String fullName;
    private String email;

    /**
     * Detailed data is obtained lazily.
     */
    private volatile UserSoapDO data;

    CTFUser(CollabNetApp app, ProjectMemberSoapRow data) {
        super(app,data.getUserName());
        this.userName = data.getUserName();
        this.fullName = data.getFullName();
        this.email = data.getEmail();
    }

    CTFUser(CollabNetApp app, UserSoapDO data) {
        super(app,data.getUsername());
        this.userName = data.getUsername();
        this.fullName = data.getFullName();
        this.email = data.getEmail();
        this.data = data;
    }

    CTFUser(CollabNetApp app, UserSoapRow data) {
        super(app,data.getUserName());
        this.userName = data.getUserName();
        this.fullName = data.getFullName();
        this.email = data.getEmail();
    }

    private UserSoapDO data() throws RemoteException {
        if (data==null)
            data = app.icns.getUserData(app.getSessionId(),getId());
        return data;
    }

    public String getUserName() {
        return userName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    /**
     * Alias for {@link #getUserName()}.
     */
    @Override
    public String getTitle() {
        return userName;
    }

    public String getLocale() throws RemoteException {
        return data().getLocale();
    }

    public String getTimeZone() throws RemoteException {
        return data().getTimeZone();
    }

    public boolean isSuperUser() throws RemoteException {
        return data().getSuperUser();
    }

    public boolean isRestrictedUser() throws RemoteException {
        return data().getRestrictedUser();
    }

    public String getStatus() throws RemoteException {
        return data().getStatus();
    }

    /**
     * Gets the group full names that this user belongs to.
     *
     * This will only work
     * if logged in as the user in question, or if the logged in user has
     * superuser permissions.
     *
     * <p>
     * Because of the incompleteness in the API, this method cannot readily return
     * {@link CTFGroup}s.
     */
    public Set<String> getGroupNames() throws RemoteException {
        Set<String> groups = new HashSet<String>();
        GroupSoapList gList = app.icns.getUserGroupList(app.getSessionId(),userName);
        for (GroupSoapRow row: gList.getDataRows()) {
            groups.add(row.getFullName());
        }
        return groups;
    }

    public CTFList<CTFGroup> getGroups() throws RemoteException {
        CTFList<CTFGroup> groups = new CTFList<CTFGroup>();
        GroupSoapList gList = app.icns.getUserGroupList(app.getSessionId(),userName);
        for (GroupSoapRow row: gList.getDataRows()) {
            groups.add(app.getGroupByTitle(row.getFullName()));
        }
        return groups;
    }

    /**
     * Adds the user to the this group.
     */
    public void addTo(CTFGroup g) throws RemoteException {
        g.addMember(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CTFUser that = (CTFUser) o;
        return userName.equals(that.userName);
    }

    @Override
    public int hashCode() {
        return userName.hashCode();
    }
}
