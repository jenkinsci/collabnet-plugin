package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.ClientSoapStubFactory;

import com.collabnet.ce.soap50.webservices.tracker.ITrackerAppSoap;
import com.collabnet.ce.soap50.webservices.tracker.ArtifactSoapDO;
import com.collabnet.ce.soap50.webservices.tracker.ArtifactSoapList;
import com.collabnet.ce.soap50.webservices.tracker.ArtifactSoapRow;
import com.collabnet.ce.soap50.webservices.tracker.TrackerSoapList;
import com.collabnet.ce.soap50.webservices.tracker.TrackerSoapRow;

import com.collabnet.ce.soap50.types.SoapFieldValues;
import com.collabnet.ce.soap50.types.SoapFilter;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Date;
import java.util.TreeSet;


/**
 * Class to hold the tracker-related methods.
 * Wraps a collabNetApp.
 */
public class TrackerApp {
    private CollabNetApp collabNetApp;
    private ITrackerAppSoap itas;
    
    /**
     * Contructs a new TrackerApp.
     * 
     * @param collabNetApp a valid (logged-in) instance.
     */
    public TrackerApp(CollabNetApp collabNetApp) {
        this.collabNetApp = collabNetApp;
        this.itas = this.getITrackerAppSoap();
    }
    
    /**
     * @return an instance of the Client Soap stub for TrackerApp.wsdl.
     */
    private ITrackerAppSoap getITrackerAppSoap() {
        String soapURL = this.getUrl() + CollabNetApp.SOAP_SERVICE +
            "TrackerApp?wsdl";
        return (ITrackerAppSoap) ClientSoapStubFactory.
            getSoapStub(ITrackerAppSoap.class, soapURL);
    } 
    
    /**
     * Find the matching tracker id.
     *
     * @param projectId
     * @param trackerName
     * @return the tracker id for this name, in this project, if found.
     *         Otherwise, null.
     * @throws RemoteException
     */
    public String getTrackerId(String projectId, String trackerName) 
        throws RemoteException {
        this.checkValidSessionId();
        TrackerSoapList tslist = this.itas.getTrackerList(this.getSessionId(), 
                                                          projectId);
        for (TrackerSoapRow row: tslist.getDataRows()) {
            if (row.getTitle().equals(trackerName)) {
                return row.getId();
            }
        }
        return null;
    }

    /**
     * Return a list of unique tracker titles for a given project.
     *
     * @param projectId
     * @return Collection of unique tracker names.
     */
    public Collection<String> getTrackers(String projectId) 
        throws RemoteException {
        this.checkValidSessionId();
        Collection<String> names = new TreeSet<String>();
        TrackerSoapList tslist = this.itas.getTrackerList(this.getSessionId(), 
                                                          projectId);
        for (TrackerSoapRow row: tslist.getDataRows()) {
            names.add(row.getTitle());
        }
        return names;
    }
    
    /**
     * Find the most recently submitted TrackerArtifact matching the given
     * title.
     *
     * @param trackerId
     * @param title of the tracker to find.
     * @return artifact for the last artifact with that title.
     * @throws RemoteException
     */
    public ArtifactSoapDO findLastTrackerArtifact(String trackerId, 
                                                  String title) 
        throws RemoteException {
        this.checkValidSessionId();
        SoapFilter[] filters = new SoapFilter[1];
        filters[0] = new SoapFilter("title", title);
        ArtifactSoapList aslist = this.itas.
            getArtifactList(this.getSessionId(), trackerId, filters);
        ArtifactSoapRow latestRow = this.getLastSubmitted(aslist.
                                                          getDataRows());
        if (latestRow != null) {
            return this.getArtifactDO(latestRow.getId());
        } else {
            return null;
        }
    }
    
    /**
     * Given an artifactId (which you can get from an ArtifactSoapRow)
     * return the appropriate ArtifactSoapDO.
     *
     * @param artifactId 
     * @return artifact data object with the given id.
     * @throws RemoteException
     */
    public ArtifactSoapDO getArtifactDO(String artifactId) 
        throws RemoteException {
        this.checkValidSessionId();
        return this.itas.getArtifactData(this.getSessionId(), artifactId);
    }
    
    /**
     * Return the row with the latest submitted date
     *
     * @param rows of artifacts.
     * @return the artifact with the last submitted date.
     */
    private ArtifactSoapRow getLastSubmitted(ArtifactSoapRow[] rows) {
        if (rows.length < 1) {
            return null;
        }
        Date latest_date = rows[0].getSubmittedDate();
        int latest_index = 0;
        for (int index = 1; index < rows.length; index++) {
            Date row_date = rows[index].getSubmittedDate();
            if (row_date.after(latest_date)) {
                latest_date = row_date;
                latest_index = index;
            }
        }
        return rows[latest_index];
    }
    
    /**
     * Create a new tracker artifact with the given values.
     *
     * @param trackerId
     * @param title for the new tracker
     * @param description of the tracker
     * @param group of the tracker
     * @param category of the tracker
     * @param status of the tracker (open, closed, etc).
     * @param customer this artifact affects.
     * @param priority of the artifact.
     * @param estimatedHours to fix the issue.
     * @param assignTo user to assign this issue to.
     * @param releaseId of the release this issue is associated with.
     * @param flexFields user-defined fields.
     * @param fileName of the attachment.
     * @param fileMimeType of the attachment.
     * @param fileId of the attachment (returned when attachment was uploaded).
     * @return the newly created ArtifactSoapDO.
     * @throws RemoteException
     */
    public ArtifactSoapDO createNewTrackerArtifact(String trackerId,
                                                   String title,
                                                   String description,
                                                   String group,
                                                   String category,
                                                   String status,
                                                   String customer,
                                                   int priority,
                                                   int estimatedHours,
                                                   String assignTo,
                                                   String releaseId,
                                                   SoapFieldValues flexFields,
                                                   String fileName,
                                                   String fileMimeType,
                                                   String fileId) 
    throws RemoteException {
        this.checkValidSessionId();
        return this.itas.createArtifact(this.getSessionId(), trackerId, title,
                                        description, group, category,  status, 
                                        customer, priority, estimatedHours, 
                                        assignTo, releaseId, flexFields,
                                        fileName, fileMimeType, fileId);
    }
    
    /**
     * Update an existing artifact.
     *
     * @param artifact to update
     * @param comment of this update.
     * @param fileName of associated attachment.
     * @param fileMimeType of associated attachment.
     * @param fileId of associated attachment (returned when attachment 
     *                                         was uploaded)
     * @throws RemoteExcpetion
     */
    public void setArtifactData(ArtifactSoapDO artifact, String comment, 
                                String fileName, String fileMimeType, 
                                String fileId) throws RemoteException {
        this.checkValidSessionId();
        this.itas.setArtifactData(this.getSessionId(), artifact, comment, 
                                  fileName, fileMimeType, fileId);
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
