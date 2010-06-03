package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.frs.FrsFileSoapDO;
import com.collabnet.ce.soap50.webservices.frs.FrsFileSoapRow;
import com.collabnet.ce.soap50.webservices.frs.ReleaseSoapDO;
import com.collabnet.ce.soap50.webservices.frs.ReleaseSoapRow;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class CTFRelease extends CTFFolder {
    CTFRelease(CTFPackage parent, ReleaseSoapDO data) {
        super(parent,data);
    }

    CTFRelease(CTFPackage parent, ReleaseSoapRow data) {
        super(parent,data);
    }

    /**
     * The HTTP URL of this release on the server.
     */
    public String getUrl() {
        return app.getServerUrl()+"/sf/frs/do/viewRelease/"+getPath();
    }

    public void delete() throws RemoteException {
        app.getFrsAppSoap().deleteRelease(app.getSessionId(),getId());
    }

    public CTFReleaseFile getFileByTitle(String title) throws RemoteException {
        for (CTFReleaseFile f : getFiles())
            if (f.getTitle().equals(title))
                return f;
        return null;
    }

    public List<CTFReleaseFile> getFiles() throws RemoteException {
        List<CTFReleaseFile> r = new ArrayList<CTFReleaseFile>();
        for (FrsFileSoapRow row : app.getFrsAppSoap().getFrsFileList(app.getSessionId(),getId()).getDataRows()) {
            r.add(new CTFReleaseFile(this,row));
        }
        return r;
    }

    public CTFReleaseFile addFile(String fileName, String mimeType, CTFFile file)
        throws RemoteException {
        return new CTFReleaseFile(this,app.getFrsAppSoap().createFrsFile(app.getSessionId(), getId(), fileName, mimeType, file.getId()));
    }

}
