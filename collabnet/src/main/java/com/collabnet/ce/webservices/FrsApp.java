package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.ClientSoapStubFactory;

import com.collabnet.ce.soap50.webservices.frs.IFrsAppSoap;
import com.collabnet.ce.soap50.webservices.frs.FrsFileSoapDO;
import com.collabnet.ce.soap50.webservices.frs.FrsFileSoapList;
import com.collabnet.ce.soap50.webservices.frs.FrsFileSoapRow;
import com.collabnet.ce.soap50.webservices.frs.PackageSoapList;
import com.collabnet.ce.soap50.webservices.frs.PackageSoapRow;
import com.collabnet.ce.soap50.webservices.frs.ReleaseSoapDO;
import com.collabnet.ce.soap50.webservices.frs.ReleaseSoapList;
import com.collabnet.ce.soap50.webservices.frs.ReleaseSoapRow;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;

/**
 * Class to hold the package and release-related methods.
 * Wraps a collabNetApp.
 */
public class FrsApp {
    private CollabNetApp collabNetApp;
    private IFrsAppSoap ifa;
    
    /**
     * Constructs a FileReleaseSystem webservice container, from
     * a CollabNetApp webservice container.
     *
     * @param collabNetApp logged in main CollabNet webservice.
     */
    public FrsApp(CollabNetApp collabNetApp) {
        this.collabNetApp = collabNetApp;
        this.ifa = this.getFrsAppSoap();
    }
    
    /**
     * Create a new interface to the underlying SOAP methods.
     *
     * @return an instance of the FileReleseSystem webservice client. 
     */
    private IFrsAppSoap getFrsAppSoap() {
        String soapURL = this.getUrl() + CollabNetApp.SOAP_SERVICE + 
            "FrsApp?wsdl";
        return (IFrsAppSoap) ClientSoapStubFactory.
            getSoapStub(IFrsAppSoap.class, soapURL);
    }

    /**
     * Find the packageId in a project with a given name.
     *
     * @param packageName 
     * @param projectId
     * @return the package id for a package with this name in the project, or
     *         null if the package cannot be found.
     * @throws RemoteException
     */
    public String findPackageId(String packageName, String projectId) 
        throws RemoteException {
        this.checkValidSessionId();
        if (packageName == null || packageName.equals("")) {
            return null;
        }
        PackageSoapList psList = this.ifa.getPackageList(this.getSessionId(), 
                                                         projectId);
        for (PackageSoapRow prow: psList.getDataRows()) {
            if (prow.getTitle().equals(packageName)) {
                return prow.getId();
            }
        }
        return null;
    }

    /**
     * Get the list of package names associated with a given projectId.
     *
     * @param projectId
     * @return collection of package names.
     * @throws RemoteException
     */
    public Collection<String> getPackages(String projectId) 
        throws RemoteException {
        this.checkValidSessionId();
        PackageSoapList psList = this.ifa.getPackageList(this.getSessionId(), 
                                                         projectId);
        Collection<String> names = new ArrayList<String>();
        for (PackageSoapRow prow: psList.getDataRows()) {
            names.add(prow.getTitle());
        }
        return names;
    }
    
    /**
     * Find the releaseId when we don't know the package.
     * We'll need to get all packages for the project, then get all releases
     * for each package and search for the release we're looking for.
     *
     * @param releaseName
     * @param projectId
     * @return the id for the first matching release, or null, if 
     *         the release cannot be found.
     * @throws RemoteException
     */
    public String findReleaseId (String releaseName, String projectId) 
        throws RemoteException {
        this.checkValidSessionId();
        if (releaseName == null || releaseName.equals("")) {
            return null;
        }
        PackageSoapList psList = this.ifa.getPackageList(this.getSessionId(), 
                                                         projectId);
        String matchingReleaseId = null;
        for (PackageSoapRow prow: psList.getDataRows()) {
            if ((matchingReleaseId = 
                 this.findReleaseIdByPackage(releaseName, prow.getId())) 
                != null) {
                break;
            }
        }
        return matchingReleaseId;
    }

    /**
     * Get the list of possible releases, in a given project.
     *
     * @param projectId
     * @return a collection of unique release names.
     */
    public Collection<String> getProjectReleases(String projectId) 
        throws RemoteException {
        this.checkValidSessionId();
        Collection<String> names = new TreeSet<String>();
        PackageSoapList psList = this.ifa.getPackageList(this.getSessionId(), 
                                                         projectId);
        for (PackageSoapRow prow: psList.getDataRows()) {
            names.addAll(this.getReleases(prow.getId()));
        }
        return names;
    }

    /**
     * Get the releaseId when we know the packageId.
     *
     * @param releaseName 
     * @param packageId
     * @return the id for the release from this package or null, if none is
     *         found.
     * @throws RemoteException
     */
    public String findReleaseIdByPackage(String releaseName, String packageId) 
        throws RemoteException {
        this.checkValidSessionId();
        if (releaseName == null || releaseName.equals("")) {
            return null;
        }
        ReleaseSoapList rsList = this.ifa.getReleaseList(this.getSessionId(), 
                                                          packageId);
        for (ReleaseSoapRow rrow: rsList.getDataRows()) {
            if (rrow.getTitle().equals(releaseName)) {
                return rrow.getId();
            }
        }
        return null;
    }

    /**
     * Get the list of release names, given a packageId.
     *
     * @param packageId
     * @return a list of release names.
     * @throws RemoteException
     */
    public Collection<String> getReleases(String packageId) 
        throws RemoteException {
        this.checkValidSessionId();
        ReleaseSoapList rsList = this.ifa.getReleaseList(this.getSessionId(), 
                                                         packageId);
        Collection<String> names = new ArrayList<String>();
        for (ReleaseSoapRow rrow: rsList.getDataRows()) {
            names.add(rrow.getTitle());
        }
        return names;
    }

    /**
     * Get the path to the release's page.
     *
     * @param releaseId
     * @return the path to the release.
     * @throws RemoteException
     */
    public String getReleasePath(String releaseId) throws RemoteException {
        this.checkValidSessionId();
        ReleaseSoapDO rsd = this.ifa.getReleaseData(this.getSessionId(), 
                                                    releaseId);
        return rsd.getPath();
    }

    /**
     * Find the File Release System file which matches the given name,
     * if any.  Otherwise, return null.
     *
     * @param name match with the file's title.
     * @param releaseId for this file.
     * @return the id for this file, or null if none is found.
     * @throws RemoteException
     */
    public String findFrsFile(String name, String releaseId) 
        throws RemoteException {
        this.checkValidSessionId();
        String fileId = null;
        FrsFileSoapList ffslist = this.ifa.getFrsFileList(this.getSessionId(), 
                                                          releaseId);
        for (FrsFileSoapRow row: ffslist.getDataRows()) {
            if (row.getTitle().equals(name)) {
                fileId = row.getId();
                break;
            }
        }
        return fileId;
    }

    /**
     * Delete existing Frs files matching the given name.
     *
     * @param fileId of the file to delete.
     * @throws RemoteException
     */
    public void deleteFrsFile(String fileId) 
        throws RemoteException {
        this.checkValidSessionId();
        this.ifa.deleteFrsFile(this.getSessionId(), fileId);
    }

    /**
     * Associate an uploaded file with a release.
     * 
     * @param releaseId 
     * @param fileName of file that was uploaded.
     * @param mimeType of the uploaded file.
     * @param fileId returned when file was uploaded.
     * @return newly SoapDO for newly created FRSFile.
     */
    public FrsFileSoapDO createFrsFile(String releaseId, String fileName, 
                                       String mimeType, String fileId) 
        throws RemoteException {
        this.checkValidSessionId();
        return this.ifa.createFrsFile(this.getSessionId(), releaseId, fileName,
                                      mimeType, fileId);
    }
    
    /*************************************
     * Below are wrapping functions, to make the code neater and in case,
     * something changes.
     **************************************/
    
    private void checkValidSessionId() {
        this.collabNetApp.checkValidSessionId();
    }
    
    private String getSessionId() {
        return this.collabNetApp.getSessionId();
    }
    
    private String getUrl() {
        return this.collabNetApp.getServerUrl();
    }
}
