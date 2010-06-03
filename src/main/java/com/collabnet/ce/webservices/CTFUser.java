package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.cemain.ProjectMemberSoapRow;
import com.collabnet.ce.soap50.webservices.cemain.UserSoapRow;

/**
 * @author Kohsuke Kawaguchi
 */
public class CTFUser implements ObjectWithTitle {
    private java.lang.String userName;
    private java.lang.String fullName;
    private java.lang.String email;

    CTFUser(ProjectMemberSoapRow data) {
        this.userName = data.getUserName();
        this.fullName = data.getFullName();
        this.email = data.getEmail();
    }

    public CTFUser(UserSoapRow data) {
        this.userName = data.getUserName();
        this.fullName = data.getFullName();
        this.email = data.getEmail();
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
