package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.ClientSoapStubFactory;

import com.collabnet.ce.soap50.webservices.scm.IScmAppSoap;
import com.collabnet.ce.soap50.webservices.scm.Repository2SoapDO;
import com.collabnet.ce.soap50.webservices.scm.RepositorySoapList;
import com.collabnet.ce.soap50.webservices.scm.RepositorySoapRow;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Class to hold the scm-related methods.
 * Wraps a collabNetApp.
 */
public class ScmApp extends AbstractSoapApp {
    private IScmAppSoap isas;

    /**
     * Constructs a new ScmApp.
     * 
     * @param collabNetApp a valid (logged-in) instance.
     */
    public ScmApp(CollabNetApp collabNetApp) {
        super(collabNetApp);
        this.isas = this.getIScmAppSoap();
    }

    /**
     * @return an instance of the Client Soap stub for ScmApp.wsdl.
     */
    private IScmAppSoap getIScmAppSoap() {
        String soapURL = this.getServerUrl() + CollabNetApp.SOAP_SERVICE +
            "ScmApp?wsdl";
        return (IScmAppSoap) ClientSoapStubFactory.
            getSoapStub(IScmAppSoap.class, soapURL);
    }

    /**
     * @return the repoId for a given repo name and project.
     */
    public String getRepoId(String projectId, String repoName) 
        throws RemoteException {
        this.checkValidSessionId();
        RepositorySoapList repoList = this.isas.
            getRepositoryList(this.getSessionId(), projectId);
        for (RepositorySoapRow row: repoList.getDataRows()) {
            if (row.getTitle().equals(repoName)) {
                return row.getId();
            }
        }
        return null;
    }

    /**
     * @return the repository data for the given repoId.
     */
    public Repository2SoapDO getRepoData(String repoId) throws RemoteException {
        this.checkValidSessionId();
        return this.isas.getRepositoryData2(this.getSessionId(), repoId);
    }

    /**
     * @return a collection of repo names for a project.
     */
    public Collection<String> getRepos(String projectId) 
        throws RemoteException {
        this.checkValidSessionId();
        RepositorySoapList repoList = this.isas.
            getRepositoryList(this.getSessionId(), projectId);
        Collection<String> repoNames = new ArrayList<String>
            (repoList.getDataRows().length);
        for (RepositorySoapRow row: repoList.getDataRows()) {
            repoNames.add(row.getTitle());
        }
        return repoNames;
    }
}
