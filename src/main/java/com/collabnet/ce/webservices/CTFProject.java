package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.cemain.ProjectMemberSoapRow;
import com.collabnet.ce.soap50.webservices.cemain.ProjectSoapDO;
import com.collabnet.ce.soap50.webservices.cemain.ProjectSoapRow;
import com.collabnet.ce.soap50.webservices.docman.DocumentFolderSoapList;
import com.collabnet.ce.soap50.webservices.docman.DocumentFolderSoapRow;
import com.collabnet.ce.soap50.webservices.frs.PackageSoapRow;
import com.collabnet.ce.soap50.webservices.scm.RepositorySoapRow;
import com.collabnet.ce.soap50.webservices.tracker.TrackerSoapRow;

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

    public CTFTracker createTracker(String name, String title, String description) throws RemoteException {
        return new CTFTracker(this,
            app.getTrackerSoap().createTracker(app.getSessionId(), getId(), name, title, description));
    }

    public CTFScmRepository getScmRepositoryByTitle(String title) throws RemoteException {
        return findByTitle(getScmRepositories(),title);
    }

    public List<CTFScmRepository> getScmRepositories() throws RemoteException {
        List<CTFScmRepository> r = new ArrayList<CTFScmRepository>();
        for (RepositorySoapRow row : app.getScmAppSoap().getRepositoryList(app.getSessionId(), getId()).getDataRows()) {
            r.add(new CTFScmRepository(this,row));
        }
        return r;
    }

    public List<CTFUser> getMembers() throws RemoteException {
        List<CTFUser> r = new ArrayList<CTFUser>();
        for (ProjectMemberSoapRow row : app.icns.getProjectMemberList(app.getSessionId(),getId()).getDataRows())
            r.add(new CTFUser(row));
        return r;
    }

    public void addMember(String userName) throws RemoteException {
        app.icns.addProjectMember(app.getSessionId(),getId(),userName);
    }

    public void addMember(CTFUser u) throws RemoteException {
        addMember(u.getUserName());
    }

    public boolean hasMember(String username) throws RemoteException {
        for (CTFUser u : getMembers()) {
            if (u.getUserName().equals(username))
                return true;
        }
        return false;
    }

    public CTFDocumentFolder getRootFolder() throws RemoteException {
        DocumentFolderSoapList dfsList = app.getDocumentAppSoap().getDocumentFolderList(app.getSessionId(), getId(),false);
        switch (dfsList.getDataRows().length) {
        case 0:
            throw new CollabNetApp.
                CollabNetAppException("getRootFolder for projectId " +
                                      title +
                                      " failed to find any folders");
        case 1:
            return new CTFDocumentFolder(this,dfsList.getDataRows()[0]);

        default:
            StringBuilder rowNames = new StringBuilder();
            for (DocumentFolderSoapRow row: dfsList.getDataRows()) {
                rowNames.append(row.getTitle() + ", ");
            }
            throw new CollabNetApp.
                CollabNetAppException("getRootFolder returned unexpected " +
                                      "number of folders: " +
                                      rowNames.toString());
        }
    }

    /**
     * Gets to the folder from a path string like "foo/bar/zot", if necessary by creating intermediate directories.
     */
    public CTFDocumentFolder getOrCreateDocumentFolder(String documentPath) throws RemoteException {
        String[] folderNames = documentPath.split("/");
        int i = 0;
        // find the root folder since the first document path may or may not
        // match this.
        CTFDocumentFolder cur = getRootFolder();
        if (cur.getTitle().equals(folderNames[i])) {
            i++;
        }
        for (; i < folderNames.length; i++) {
            CTFDocumentFolder next = cur.getFolderByTitle(folderNames[i]);
            if (next==null) break;
            cur = next;
        }

        // create any missing folders
        for (; i < folderNames.length; i++) {
            cur = cur.createFolder(folderNames[i], folderNames[i]);
        }
        return cur;
    }

    /**
     * Verify a document folder path.  If at any point the folder
     * is missing, return the name of the first missing folder.
     *
     * @param documentPath string with folders separated by '/'.
     * @return the first missing folder, or null if all are found.
     * @throws RemoteException
     */
     public String verifyPath(String documentPath) throws RemoteException {
        String[] folderNames = documentPath.split("/");
        int i = 0;
        CTFDocumentFolder cur = getRootFolder();
        if (cur.getTitle().equals(folderNames[i])) {
            i++;
        }
        for (; i < folderNames.length; i++) {
            CTFDocumentFolder next = cur.getFolderByTitle(folderNames[i]);
            if (next == null) {
                return folderNames[i];
            } else {
                cur = next;
            }
        }
        return null;
     }
}