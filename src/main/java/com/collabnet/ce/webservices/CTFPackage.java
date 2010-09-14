package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.frs.PackageSoapDO;
import com.collabnet.ce.soap50.webservices.frs.PackageSoapRow;
import com.collabnet.ce.soap50.webservices.frs.ReleaseSoapRow;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class CTFPackage extends CTFFolder {
    CTFPackage(CTFObject parent, PackageSoapDO data) {
        super(parent,data);
    }

    CTFPackage(CTFObject parent, PackageSoapRow data) {
        super(parent, data);
    }

    /**
     * Deletes this package.
     */
    public void delete() throws RemoteException {
        app.getFrsAppSoap().deletePackage(app.getSessionId(),getId());
    }

    public CTFRelease createRelease(String title, String description, String status, String maturity) throws RemoteException {
        return new CTFRelease(this,app.getFrsAppSoap().createRelease(app.getSessionId(),getId(),title,description,status,maturity));
    }

    /**
     * Finds a release by its title, or return null if not found.
     */
    public CTFRelease getReleaseByTitle(String title) throws RemoteException {
        for (CTFRelease p : getReleases())
            if (p.getTitle().equals(title))
                return p;
        return null;
    }

    public CTFList<CTFRelease> getReleases() throws RemoteException {
        CTFList<CTFRelease> r = new CTFList<CTFRelease>();
        for (ReleaseSoapRow row : app.getFrsAppSoap().getReleaseList(app.getSessionId(), getId()).getDataRows()) {
            r.add(new CTFRelease(this,row));
        }
        return r;
    }
}
