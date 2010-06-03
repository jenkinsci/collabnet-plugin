package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.scm.RepositorySoapDO;
import com.collabnet.ce.soap50.webservices.scm.RepositorySoapRow;

import java.rmi.RemoteException;

/**
 * A SCM repository.
 *
 * @author Kohsuke Kawaguchi
 */
public class CTFScmRepository extends CTFFolder {
    /**
     * Lazily fetched.
     */
    private volatile RepositorySoapDO data;

    public CTFScmRepository(CTFProject parent, RepositorySoapDO data) {
        super(parent, data);
    }

    public CTFScmRepository(CTFProject parent, RepositorySoapRow data) {
        super(parent, data);
    }

    private RepositorySoapDO data() throws RemoteException {
        if (data==null)
            data = app.getScmAppSoap().getRepositoryData(app.getSessionId(),getId());
        return data;
    }
    public String getSystemId() throws RemoteException {
        return data().getSystemId();
    }

    public String getSystemTitle() throws RemoteException {
        return data().getSystemTitle();
    }

    public String getRepositoryDirectory() throws RemoteException {
        return data().getRepositoryDirectory();
    }

    public String getScmViewerUrl() throws RemoteException {
        return data().getScmViewerUrl();
    }

    public String getScmAdapterName() throws RemoteException {
        return data().getScmAdapterName();
    }

    public boolean getIdRequiredOnCommit() throws RemoteException {
        return data().getIdRequiredOnCommit();
    }

    public boolean getIsOnManagedScmServer() throws RemoteException {
        return data().getIsOnManagedScmServer();
    }
}
