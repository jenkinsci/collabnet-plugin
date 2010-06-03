package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.cemain.ProjectMemberSoapList;
import com.collabnet.ce.soap50.webservices.cemain.ProjectMemberSoapRow;
import com.collabnet.ce.soap50.webservices.cemain.ProjectSoapDO;
import com.collabnet.ce.soap50.webservices.cemain.ProjectSoapRow;
import com.collabnet.ce.soap50.webservices.frs.PackageSoapRow;
import com.collabnet.ce.soap50.webservices.tracker.TrackerSoapRow;
import hudson.plugins.collabnet.util.CommonUtil;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * A project in TeamForge.
 *
 * @author Kohsuke Kawaguchi
 */
public class CTFProject extends CTFObject implements ObjectWithTitle {
    private final String title;

    CTFProject(CollabNetApp app, ProjectSoapDO data) {
        super(app,data.getId());
        this.title = data.getTitle();
    }

    CTFProject(CollabNetApp app, ProjectSoapRow data) {
        super(app,data.getId());
        this.title = data.getTitle();
    }

    public String getTitle() {
        return title;
    }
    
    /**
     * @param title
     *      Package title.
     * @param description
     *      Package description.
     * @param isPublished
     *      Whether the package should be published
     */
    public CTFPackage createPackage(String title, String description, boolean isPublished) throws RemoteException {
        return new CTFPackage(this, app.ifrs.createPackage(app.getSessionId(), getId(), title, description, isPublished));
    }

    /**
     * Finds a package by its title, or return null if not found.
     */
    public CTFPackage getPackageByTitle(String title) throws RemoteException {
        return findByTitle(getPackages(),title);
    }

    public List<CTFPackage> getPackages() throws RemoteException {
        List<CTFPackage> r = new ArrayList<CTFPackage>();
        for (PackageSoapRow row : app.ifrs.getPackageList(app.getSessionId(), getId()).getDataRows()) {
            r.add(new CTFPackage(this,row));
        }
        return r;
    }

    public List<CTFTracker> getTrackers() throws RemoteException {
        List<CTFTracker> r = new ArrayList<CTFTracker>();
        for (TrackerSoapRow row : app.getTrackerSoap().getTrackerList(app.getSessionId(), getId()).getDataRows()) {
            r.add(new CTFTracker(this,row));
        }
        return r;
    }

    /**
     * Finds a tracker by its title, or return null if not found.
     */
    public CTFTracker getTrackerByTitle(String title) throws RemoteException {
        return findByTitle(getTrackers(),title);
    }

    private <T extends ObjectWithTitle> T findByTitle(List<T> list, String title) {
        for (T p : list)
            if (p.getTitle().equals(title))
                return p;
        return null;
    }

    public List<CTFUser> getMembers() throws RemoteException {
        List<CTFUser> r = new ArrayList<CTFUser>();
        for (ProjectMemberSoapRow row : app.icns.getProjectMemberList(app.getSessionId(),getId()).getDataRows())
            r.add(new CTFUser(row));
        return r;
    }

    public boolean hasMember(String username) throws RemoteException {
        for (CTFUser u : getMembers()) {
            if (u.getUserName().equals(username))
                return true;
        }
        return false;
    }
}