package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.cemain.ProjectMemberSoapRow;
import com.collabnet.ce.soap50.webservices.cemain.ProjectSoapDO;
import com.collabnet.ce.soap50.webservices.cemain.ProjectSoapRow;
import com.collabnet.ce.soap50.webservices.cemain.UserSoapRow;
import com.collabnet.ce.soap50.webservices.docman.DocumentFolderSoapList;
import com.collabnet.ce.soap50.webservices.docman.DocumentFolderSoapRow;
import com.collabnet.ce.soap50.webservices.frs.PackageSoapRow;
import com.collabnet.ce.soap50.webservices.rbac.RoleSoapList;
import com.collabnet.ce.soap50.webservices.rbac.RoleSoapRow;
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
        return new CTFPackage(this, app.getFrsAppSoap().createPackage(app.getSessionId(), getId(), title, description, isPublished));
    }

    public CTFList<CTFPackage> getPackages() throws RemoteException {
        CTFList<CTFPackage> r = new CTFList<CTFPackage>();
        for (PackageSoapRow row : app.getFrsAppSoap().getPackageList(app.getSessionId(), getId()).getDataRows()) {
            r.add(new CTFPackage(this,row));
        }
        return r;
    }

    public CTFList<CTFTracker> getTrackers() throws RemoteException {
        CTFList<CTFTracker> r = new CTFList<CTFTracker>();
        for (TrackerSoapRow row : app.getTrackerSoap().getTrackerList(app.getSessionId(), getId()).getDataRows()) {
            r.add(new CTFTracker(this,row));
        }
        return r;
    }

    public CTFTracker createTracker(String name, String title, String description) throws RemoteException {
        return new CTFTracker(this,
            app.getTrackerSoap().createTracker(app.getSessionId(), getId(), name, title, description));
    }

    public CTFList<CTFScmRepository> getScmRepositories() throws RemoteException {
        CTFList<CTFScmRepository> r = new CTFList<CTFScmRepository>();
        for (RepositorySoapRow row : app.getScmAppSoap().getRepositoryList(app.getSessionId(), getId()).getDataRows()) {
            r.add(new CTFScmRepository(this,row));
        }
        return r;
    }

    public List<CTFUser> getMembers() throws RemoteException {
        List<CTFUser> r = new ArrayList<CTFUser>();
        for (ProjectMemberSoapRow row : app.icns.getProjectMemberList(app.getSessionId(),getId()).getDataRows())
            r.add(new CTFUser(app,row));
        return r;
    }

    /**
     * Gets the administrators of this project.
     */
    public List<CTFUser> getAdmins() throws RemoteException {
        List<CTFUser> r = new ArrayList<CTFUser>();
        for (UserSoapRow row : app.icns.listProjectAdmins(app.getSessionId(),getId()).getDataRows())
            r.add(new CTFUser(app,row));
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

    /**
     * Roles in this project.
     */
    public CTFList<CTFRole> getRoles() throws RemoteException {
        return toRoleList(app.getRbacAppSoap().getRoleList(app.getSessionId(), getId()));
    }

    public CTFRole createRole(String title, String description) throws RemoteException {
        return new CTFRole(this,app.getRbacAppSoap().createRole(app.getSessionId(),getId(),title,description));
    }

    public CTFList<CTFRole> getUserRoles(CTFUser u) throws RemoteException {
        return getUserRoles(u.getUserName());
    }

    /**
     * Gets all the roles that the given user has in this project.
     */
    public CTFList<CTFRole> getUserRoles(String username) throws RemoteException {
        return toRoleList(app.getRbacAppSoap().getUserRoleList(app.getSessionId(),getId(),username));
    }

    private CTFList<CTFRole> toRoleList(RoleSoapList roles) {
        CTFList<CTFRole> r = new CTFList<CTFRole>();
        for (RoleSoapRow row : roles.getDataRows()) {
            r.add(new CTFRole(this,row));
        }
        return r;
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
        documentPath = normalizePath(documentPath);
        String[] folderNames = documentPath.split("/");
        int i = 0;
        // find the root folder since the first document path may or may not
        // match this.
        CTFDocumentFolder cur = getRootFolder();
        if (cur.getTitle().equals(folderNames[i])) {
            i++;
        }
        for (; i < folderNames.length; i++) {
            CTFDocumentFolder next = cur.getFolders().byTitle(folderNames[i]);
            if (next==null) break;
            cur = next;
        }

        // create any missing folders
        for (; i < folderNames.length; i++) {
            cur = cur.createFolder(folderNames[i], folderNames[i]);
        }
        return cur;
    }

    private String normalizePath(String documentPath) {
        if (documentPath.startsWith("/")) {
            documentPath = documentPath.substring(1);
        }
        if (documentPath.endsWith("/")) {
            documentPath = documentPath.substring(0, documentPath.length() - 1);
        }
        return documentPath;
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
        documentPath = normalizePath(documentPath);
        String[] folderNames = documentPath.split("/");
        int i = 0;
        CTFDocumentFolder cur = getRootFolder();
        if (cur.getTitle().equals(folderNames[i])) {
            i++;
        }
        for (; i < folderNames.length; i++) {
            CTFDocumentFolder next = cur.getFolders().byTitle(folderNames[i]);
            if (next == null) {
                return folderNames[i];
            } else {
                cur = next;
            }
        }
        return null;
     }
}