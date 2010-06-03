package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.cemain.ProjectSoapDO;
import com.collabnet.ce.soap50.webservices.cemain.ProjectSoapRow;
import com.collabnet.ce.soap50.webservices.frs.PackageSoapRow;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * A project in TeamForge.
 *
 * @author Kohsuke Kawaguchi
 */
public class CTFProject extends CTFObject {
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
        for (CTFPackage p : getPackages())
            if (p.getTitle().equals(title))
                return p;
        return null;
    }

    public List<CTFPackage> getPackages() throws RemoteException {
        List<CTFPackage> r = new ArrayList<CTFPackage>();
        for (PackageSoapRow row : app.ifrs.getPackageList(app.getSessionId(), getId()).getDataRows()) {
            r.add(new CTFPackage(this,row));
        }
        return r;
    }
}