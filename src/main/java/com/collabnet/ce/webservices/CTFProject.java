package com.collabnet.ce.webservices;

import hudson.plugins.collabnet.util.Helper;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A project in TeamForge.
 *
 * @author Kohsuke Kawaguchi
 */
public class CTFProject extends CTFObject implements ObjectWithTitle {
    private final String title;

    static Logger logger = Logger.getLogger(CTFProject.class.getName());

    Helper helper = new Helper();

    public String getTitle() {
        return title;
    }

    CTFProject(CollabNetApp app, JSONObject data) {
        super(app, data.get("id").toString());
        this.title = data.get("title").toString();
    }


    /**
     * @param title
     *      Package title.
     * @param description
     *      Package description.
     * @param isPublished
     *      Whether the package should be published
     */
    public CTFPackage createPackage(String title, String description, boolean isPublished) throws IOException {
        String end_point =  app.getServerUrl() + CTFConstants.FRS_URL + getId() + "/packages";
        JSONObject requestPayload = new JSONObject();
        requestPayload.put("title", title);
        requestPayload.put("description", description);
        requestPayload.put("published", String.valueOf(isPublished));
        Response response = helper.request(end_point, app.getSessionId(), requestPayload.toString(), HttpMethod.POST, null);
        String result = response.readEntity(String.class);
        int statusCode = response.getStatus();
        if (statusCode == 200) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                return new CTFPackage(this, data);
            } catch (ParseException e) {
                logger.log(Level.WARNING,"Unable to parse the json content in createPackage()  - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING,"Error creating a package " + statusCode  + ", Error Msg - " + result);
        }
        return null;
    }

    public CTFList<CTFPackage> getPackages() throws IOException {
        CTFList<CTFPackage> r = new CTFList<CTFPackage>();
        String end_point = app.getServerUrl() + CTFConstants.FRS_URL + getId() + "/packages";
        Response response = helper.request(end_point, app.getSessionId(), null, HttpMethod.GET, null);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status == 200) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                if (data != null & data.containsKey("items")) {
                    JSONArray dataArray = (JSONArray)data.get("items");
                    Iterator it = dataArray.iterator();
                    while (it.hasNext()) {
                        JSONObject jsonObject = (JSONObject) it.next();
                        r.add(new CTFPackage(this, jsonObject));
                    }
                }
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in getPackages() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING,"Error getting the packages - " + status + ", Error Msg - " + result);
        }
        return r;
    }

    public CTFList<CTFTracker> getTrackers() throws IOException {
        CTFList<CTFTracker> r = new CTFList<CTFTracker>();
        String end_point = app.getServerUrl() + CTFConstants.TRACKER_PRJ_URL + getId() + "/trackers";
        Response response = helper.request(end_point, app.getSessionId(), null, HttpMethod.GET, null);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status == 200) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                if (data != null & data.containsKey("items")) {
                    JSONArray dataArray = (JSONArray)data.get("items");
                    Iterator it = dataArray.iterator();
                    while (it.hasNext()) {
                        JSONObject jsonObject = (JSONObject) it.next();
                        r.add(new CTFTracker(this,jsonObject));
                    }
                }
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in getTrackers() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING,"Error getting the trackers of a project - " + status + ", Error Msg - " + result);
        }
        return r;
    }

    public CTFTracker createTracker(String name, String title, String description) throws IOException {
        String end_point =  app.getServerUrl() + CTFConstants.TRACKER_URL + getId() + "/trackers";
        JSONObject requestPayload = new JSONObject();
        requestPayload.put("name", name);
        requestPayload.put("title", title);
        requestPayload.put("description", description);
        Response response = helper.request(end_point, app.getSessionId(), requestPayload.toString(), HttpMethod.POST, null);
        String result = response.readEntity(String.class);
        int statusCode = response.getStatus();
        if (statusCode == 201) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                return new CTFTracker(this, data);
            } catch (ParseException e) {
                logger.log(Level.WARNING,"Unable to parse the json content in createTracker()  - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING,"Error creating a tracker " + statusCode + ", Error Msg - " + result);
        }
        return null;
    }

    public CTFList<CTFScmRepository> getScmRepositories() throws IOException {
        CTFList<CTFScmRepository> r = new CTFList<CTFScmRepository>();
        String end_point = app.getServerUrl() + CTFConstants.SCM_URL+ getId() + "/repositories";
        Response response = helper.request(end_point, app.getSessionId(), null, HttpMethod.GET, null);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status == 200) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                if (data != null & data.containsKey("items")) {
                    JSONArray dataArray = (JSONArray)data.get("items");
                    Iterator it = dataArray.iterator();
                    while (it.hasNext()) {
                        JSONObject jsonObject = (JSONObject) it.next();
                        r.add(new CTFScmRepository(this,jsonObject));
                    }
                }
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in getScmRepositories() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING,"Error getting the scm repositories of a project - " + status + ", Error Msg - " + result);
        }
        return r;
    }

    public List<CTFUser> getMembers() throws IOException {
        List<CTFUser> r = new ArrayList<CTFUser>();
        String end_point = app.getServerUrl() + CTFConstants.FOUNDATION_PRJ_URL + getId() + "/members";
        Map<String, String> queryParam = new HashMap<>();
        queryParam.put("offset", "0");
        queryParam.put("count", "-1");
        queryParam.put("sortby", "fullName");
        Response response = helper.request(end_point, app.getSessionId(), null, HttpMethod.GET, queryParam);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status == 200) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                if (data != null & data.containsKey("items")) {
                    JSONArray dataArray = (JSONArray)data.get("items");
                    Iterator it = dataArray.iterator();
                    while (it.hasNext()) {
                        JSONObject jsonObject = (JSONObject) it.next();
                        r.add(new CTFUser(app, jsonObject));
                    }
                }
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in getMembers() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING,"Error getting the members of a project - " + status + ", Error Msg - " + result);
        }
        return r;
    }

    /**
     * Gets the administrators of this project.
     */
    public List<CTFUser> getAdmins() throws IOException {
        List<CTFUser> r = new ArrayList<CTFUser>();
        String end_point = app.getServerUrl() + CTFConstants.FOUNDATION_PRJ_URL + getId() + "/members";
        Response response = helper.request(end_point, app.getSessionId(), null, HttpMethod.GET, null);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status == 200) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                if (data != null & data.containsKey("items")) {
                    JSONArray dataArray = (JSONArray)data.get("items");
                    Iterator it = dataArray.iterator();
                    while (it.hasNext()) {
                        JSONObject jsonObject = (JSONObject) it.next();
                        r.add(new CTFUser(app, jsonObject));
                    }
                }
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in getAdmins() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING,"Error getting the administrator of a project - " + status + ", Error Msg - " + result);
        }
        return r;
    }

    public void addMember(String userName) throws IOException {
        String end_point =  app.getServerUrl() + CTFConstants.FOUNDATION_PRJ_URL + getId() + "/" + userName;
        Response response = helper.request(end_point, app.getSessionId(), null, HttpMethod.PUT, null);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status == 200) {
            JSONObject memberData = null;
            try {
                memberData = (JSONObject) new JSONParser().parse(result);
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in addMember() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING, "Error while adding a member to the project - " + status + ", Error Msg - " + result);
        }
    }

    public void addMember(CTFUser u) throws IOException {
        addMember(u.getUserName());
    }

    public boolean hasMember(String username) throws IOException {
        for (CTFUser u : getMembers()) {
            if (u.getUserName().equals(username))
                return true;
        }
        return false;
    }

    /**
     * Roles in this project.
     */
    public CTFList<CTFRole> getRoles() throws IOException {
        CTFList<CTFRole> r = new CTFList<CTFRole>();
        String end_point = app.getServerUrl() + CTFConstants.FOUNDATION_PRJ_URL + getId() + "/roles";
        Response response = helper.request(end_point, app.getSessionId(), null, HttpMethod.GET, null);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status == 200) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                if (data != null & data.containsKey("items")) {
                    JSONArray dataArray = (JSONArray)data.get("items");
                    Iterator it = dataArray.iterator();
                    while (it.hasNext()) {
                        JSONObject jsonObject = (JSONObject) it.next();
                        r.add(new CTFRole(this, jsonObject));
                    }
                }
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in getRoles() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING,"Error getting the roles of a project - " + status + ", Error Msg - " + result);
        }
        return r;
    }

    public CTFRole createRole(String title, String description) throws IOException {
        String end_point =  app.getServerUrl() + CTFConstants.FOUNDATION_PRJ_URL + getId() + "/roles";
        JSONObject requestPayload = new JSONObject();
        requestPayload.put("title", title);
        requestPayload.put("description", description);
        requestPayload.put("preventInheritance", String.valueOf(false));
        requestPayload.put("requestable", String.valueOf(false));
        requestPayload.put("autoGrant", String.valueOf(false));
        Response response = helper.request(end_point, app.getSessionId(), requestPayload.toString(), HttpMethod.POST, null);
        String result = response.readEntity(String.class);
        int statusCode = response.getStatus();
        if (statusCode == 201) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                return new CTFRole(this, data);
            } catch (ParseException e) {
                logger.log(Level.WARNING,"Unable to parse the json content in createRole()  - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING,"Error creating a role " + statusCode  + ", Error Msg - " + result);
        }
        return null;
    }

    public CTFList<CTFRole> getUserRoles(CTFUser u) throws IOException {
        return getUserRoles(u.getUserName());
    }

    /**
     * Gets all the roles that the given user has in this project.
     */
    public CTFList<CTFRole> getUserRoles(String username) throws IOException {
        CTFList<CTFRole> r = new CTFList<CTFRole>();
        String end_point =  app.getServerUrl() + CTFConstants.FOUNDATION_PRJ_URL + getId() + "/roles/by-users" ;
        Response response = helper.request(end_point, app.getSessionId(), null, HttpMethod.GET, null);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status == 200) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                if (data != null & data.containsKey("items")) {
                    JSONArray dataArray = (JSONArray)data.get("items");
                    Iterator it = dataArray.iterator();
                    while (it.hasNext()) {
                        JSONObject jsonObject = (JSONObject) it.next();
                        r.add(new CTFRole(this, jsonObject));
                    }
                }
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in getUserRoles() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING,"Error getting the roles of an user - " + status + ", Error Msg - " + result);
        }
        return r;
    }

    public CTFDocumentFolder getRootFolder() throws IOException {
        String end_point = app.getServerUrl() + CTFConstants.DOCUMENT_FOLDERS_URL + getId();
        Map<String, String> queryParam = new HashMap<>();
        queryParam.put("recursive", "false");
        queryParam.put("basic", "true");
        Response response = helper.request(end_point, app.getSessionId(), null, HttpMethod.GET, queryParam);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status == 200) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                return new CTFDocumentFolder(this, data);
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in getRootFolder() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING,"getRootFolder for projectId " + title + " failed to find any folders " + status + ", Error Msg - " + result);
        }
        return null;
    }

    /**
     * Gets to the folder from a path string like "foo/bar/zot", if necessary by creating intermediate directories.
     */
    public CTFDocumentFolder getOrCreateDocumentFolder(String documentPath) throws IOException {
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
     public String verifyPath(String documentPath) throws IOException {
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