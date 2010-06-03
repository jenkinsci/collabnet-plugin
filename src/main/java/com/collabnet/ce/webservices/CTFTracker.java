package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.types.SoapFieldValues;
import com.collabnet.ce.soap50.types.SoapFilter;
import com.collabnet.ce.soap50.webservices.tracker.ArtifactSoapRow;
import com.collabnet.ce.soap50.webservices.tracker.TrackerSoapDO;
import com.collabnet.ce.soap50.webservices.tracker.TrackerSoapRow;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class CTFTracker extends CTFFolder {
    private CTFProject project;

    CTFTracker(CTFProject parent, TrackerSoapRow data) {
        super(parent,data);
        this.project = parent;
    }

    CTFTracker(CTFProject parent, TrackerSoapDO data) {
        super(parent,data);
        this.project = parent;
    }

    public CTFProject getProject() {
        return project;
    }

    public List<CTFArtifact> getArtifactsByTitle(String title) throws RemoteException {
        SoapFilter[] filters = {new SoapFilter("title", title)};
        List<CTFArtifact> r = new ArrayList<CTFArtifact>();
        for (ArtifactSoapRow row : app.getTrackerSoap().getArtifactList(app.getSessionId(),getId(),filters).getDataRows()) {
            r.add(new CTFArtifact(this,row));
        }
        return r;
    }

    /**
     * Create a new tracker artifact with the given values.
     *
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
    public CTFArtifact createArtifact(   String title,
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
                                                   CTFFile file)
    throws RemoteException {
        return new CTFArtifact(this,app.getTrackerSoap().createArtifact(app.getSessionId(), getId(), title,
                                        description, group, category,  status,
                                        customer, priority, estimatedHours,
                                        assignTo, releaseId, flexFields,
                                        fileName, fileMimeType, file!=null?file.getId():null));
    }
}
