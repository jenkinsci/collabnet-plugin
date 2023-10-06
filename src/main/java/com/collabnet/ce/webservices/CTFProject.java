package com.collabnet.ce.webservices;

import hudson.plugins.collabnet.util.CNFormFieldValidator;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A project in TeamForge.
 *
 * @author Kohsuke Kawaguchi
 */
public class CTFProject extends CTFObject implements ObjectWithTitle {
    private final String title;

    private static final String DOCUMENT_URL = "/ctfrest/docman/v1/documentfolders/";
    private static final String FRS_URL = "/ctfrest/frs/v1/projects/";
    private static final String TRACKER_URL = "/ctfrest/tracker/v1/projects/";
    private static final String FOUNDATION_URL = "/ctfrest/foundation/v1/projects/";
    private static final String SCM_URL = "/ctfrest/scm/v1/projects/";

    static Logger logger = Logger.getLogger(CTFProject.class.getName());

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
        String end_point =  app.getServerUrl() + FRS_URL + getId() + "/packages";
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = CNFormFieldValidator.getHttpClient();
            HttpPost httpPost = new HttpPost(end_point);
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("title", title));
            params.add(new BasicNameValuePair("description", description));
            params.add(new BasicNameValuePair("published", String.valueOf(isPublished)));
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " +app.getSessionId());
            response = client.execute(httpPost);
            if(response.getStatusLine().getStatusCode() == 200) {
                JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
                return new CTFPackage(this, data);
            }
        } catch (IOException e) {
            String errMsg = e.getMessage();
            logger.log(Level.INFO, errMsg);
        } catch (Exception e) {
            logger.log(Level.INFO,"Error creating a package" + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if(client != null) {
                client.close();
            }
        }
        return null;
    }

    public CTFList<CTFPackage> getPackages() throws RemoteException {
        CTFList<CTFPackage> r = new CTFList<CTFPackage>();
        String end_point = app.getServerUrl() + FRS_URL + getId() + "/packages";
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpGet get = new HttpGet(end_point);
            get.setHeader("Accept", "application/json");
            get.setHeader("Authorization", "Bearer " + app.getSessionId());
            response = httpClient.execute(get);
            JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
            JSONArray dataArray = JSONArray.fromObject(data.get("items"));
            Iterator it = dataArray.iterator();
            while (it.hasNext()) {
                JSONObject jsonObject = (JSONObject) it.next();
                r.add(new CTFPackage(this, jsonObject));
            }
        } catch (Exception e) {
            logger.log(Level.INFO,"Error getting the package list" + e.getLocalizedMessage(), e);
        }
        return r;
    }

    public CTFList<CTFTracker> getTrackers() throws IOException {
        CTFList<CTFTracker> r = new CTFList<CTFTracker>();
        String end_point = app.getServerUrl() + TRACKER_URL + getId() + "/trackers";
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
             httpClient = CNFormFieldValidator.getHttpClient();
            HttpGet get = new HttpGet(end_point);
            get.setHeader("Accept", "application/json");
            get.setHeader("Authorization", "Bearer " + app.getSessionId());
            response = httpClient.execute(get);
            JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
            JSONArray dataArray = JSONArray.fromObject(data.get("items"));
            Iterator it = dataArray.iterator();
            while (it.hasNext()) {
                JSONObject jsonObject = (JSONObject) it.next();
                r.add(new CTFTracker(this,jsonObject));
            }
        } catch (Exception e) {
            logger.log(Level.INFO,"Error getting the tracker lists" + e.getLocalizedMessage(), e);
        }
        return r;
    }

    public CTFTracker createTracker(String name, String title, String description) throws IOException {
        String end_point =  app.getServerUrl() + TRACKER_URL + getId() + "/trackers";
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = CNFormFieldValidator.getHttpClient();
            HttpPost httpPost = new HttpPost(end_point);
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("name", name));
            params.add(new BasicNameValuePair("title", title));
            params.add(new BasicNameValuePair("description", description));
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " +app.getSessionId());
            response = client.execute(httpPost);
            if(response.getStatusLine().getStatusCode() == 200) {
                JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
                return new CTFTracker(this, data);
            }
        } catch (IOException e) {
            String errMsg = e.getMessage();
            logger.log(Level.INFO, errMsg);
        } catch (Exception e) {
            logger.log(Level.INFO,"Error creating a tracker" + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if(client != null) {
                client.close();
            }
        }
        return null;
    }

    public CTFList<CTFScmRepository> getScmRepositories() throws RemoteException {
        CTFList<CTFScmRepository> r = new CTFList<CTFScmRepository>();
        String end_point = app.getServerUrl() + SCM_URL+ getId() + "/repositories";
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpGet get = new HttpGet(end_point);
            get.setHeader("Accept", "application/json");
            get.setHeader("Authorization", "Bearer " + app.getSessionId());
            response = httpClient.execute(get);
            JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
            JSONArray dataArray = JSONArray.fromObject(data.get("items"));
            Iterator it = dataArray.iterator();
            while (it.hasNext()) {
                JSONObject jsonObject = (JSONObject) it.next();
                r.add(new CTFScmRepository(this,jsonObject));
            }
        } catch (Exception e) {
            logger.log(Level.INFO,"Error getting the source code repositories" + e.getLocalizedMessage(), e);
        }
        return r;
    }

    public List<CTFUser> getMembers() throws RemoteException {
        List<CTFUser> r = new ArrayList<CTFUser>();
        String end_point = app.getServerUrl() + FOUNDATION_URL + getId() + "/members";
        CloseableHttpClient httpClient;
        CloseableHttpResponse response;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpGet get = new HttpGet(end_point);
            get.setHeader("Accept", "application/json");
            get.setHeader("Authorization", "Bearer " + app.getSessionId());
            URI uri = new URIBuilder(get.getURI()).addParameter(
                    "offset", "0").addParameter("count", "-1").
                    addParameter("sortby", "fullName").build();
            get.setURI(uri);
            response = httpClient.execute(get);
            JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
            JSONArray dataArray = JSONArray.fromObject(data.get("items"));
            Iterator it = dataArray.iterator();
            while (it.hasNext()) {
                JSONObject jsonObject = (JSONObject) it.next();
                r.add(new CTFUser(app, jsonObject));
            }
        } catch (Exception e) {
            logger.log(Level.INFO,"Error getting the members" + e.getLocalizedMessage(), e);
        }
        return r;
    }

    /**
     * Gets the administrators of this project.
     */
    public List<CTFUser> getAdmins() throws RemoteException {
        List<CTFUser> r = new ArrayList<CTFUser>();
        String end_point = app.getServerUrl() + FOUNDATION_URL + getId() + "/members";
        CloseableHttpClient httpClient;
        CloseableHttpResponse response;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpGet get = new HttpGet(end_point);
            get.setHeader("Accept", "application/json");
            get.setHeader("Authorization", "Bearer " + app.getSessionId());
            response = httpClient.execute(get);
            JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
            JSONArray dataArray = JSONArray.fromObject(data.get("items"));
            Iterator it = dataArray.iterator();
            while (it.hasNext()) {
                JSONObject jsonObject = (JSONObject) it.next();
                r.add(new CTFUser(app, jsonObject));
            }
        } catch (Exception e) {
            logger.log(Level.INFO,"Error getting the administrator of a project" + e.getLocalizedMessage(), e);
        }
        return r;
    }

    public void addMember(String userName) throws IOException {
        String end_point =  app.getServerUrl() + FOUNDATION_URL + getId() + "/" + userName;
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpPut put = new HttpPut(end_point);
            put.setHeader("Accept", "application/json");
            put.setHeader("Authorization", "Bearer " + app.getSessionId());
            response = httpClient.execute(put);
        } catch (Exception e) {
            logger.log(Level.INFO,"Error while adding a member to the project - " + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        }
    }

    public void addMember(CTFUser u) throws IOException {
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
        CTFList<CTFRole> r = new CTFList<CTFRole>();
        String end_point = app.getServerUrl() + FOUNDATION_URL + getId() + "/roles";
        CloseableHttpClient httpClient;
        CloseableHttpResponse response;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpGet get = new HttpGet(end_point);
            get.setHeader("Accept", "application/json");
            get.setHeader("Authorization", "Bearer " + app.getSessionId());
            response = httpClient.execute(get);
            JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
            JSONArray dataArray = JSONArray.fromObject(data.get("items"));
            Iterator it = dataArray.iterator();
            while (it.hasNext()) {
                JSONObject jsonObject = (JSONObject) it.next();
                r.add(new CTFRole(this, jsonObject));
            }
        } catch (Exception e) {
            logger.log(Level.INFO,"Error getting the roles of the project" + e.getLocalizedMessage(), e);
        }
        return r;
    }

    public CTFRole createRole(String title, String description) throws IOException {
        String end_point =  app.getServerUrl() + FOUNDATION_URL + getId() + "/roles";
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = CNFormFieldValidator.getHttpClient();
            HttpPost httpPost = new HttpPost(end_point);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " +app.getSessionId());
            JSONObject roleObj = new JSONObject()
                    .element("title", title)
                    .element("description", description)
                    .element("preventInheritance", String.valueOf(false))
                    .element("requestable", String.valueOf(false))
                    .element("autoGrant", String.valueOf(false));
            StringEntity stringEntity = new StringEntity(roleObj.toString(), ContentType.APPLICATION_JSON);
            httpPost.setEntity(stringEntity);
            response = client.execute(httpPost);
            if(response.getStatusLine().getStatusCode() == 201) {
                JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
                return new CTFRole(this, data);
            }
        } catch (IOException e) {
            String errMsg = e.getMessage();
            logger.log(Level.INFO, errMsg);
        } catch (Exception e) {
            logger.log(Level.INFO,"Error creating a tracker" + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if(client != null) {
                client.close();
            }
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
        String end_point =  app.getServerUrl() + FOUNDATION_URL + getId() + "/roles/by-users" ;
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = CNFormFieldValidator.getHttpClient();
            HttpPost httpPost = new HttpPost(end_point);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " +app.getSessionId());
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("userNames", username));
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            response = client.execute(httpPost);
            if(response.getStatusLine().getStatusCode() == 200) {
                JSONArray dataArray = JSONArray.fromObject(response.getEntity());
                Iterator it = dataArray.iterator();
                while (it.hasNext()) {
                    JSONObject jsonObject = (JSONObject) it.next();
                    r.add(new CTFRole(this, jsonObject));
                }
            }
        } catch (IOException e) {
            String errMsg = e.getMessage();
            logger.log(Level.INFO, errMsg);
        } catch (Exception e) {
            logger.log(Level.INFO,"Error creating a tracker" + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if(client != null) {
                client.close();
            }
        }
        return null;
    }

    public CTFDocumentFolder getRootFolder() throws IOException {
        String end_point = app.getServerUrl() + DOCUMENT_URL + getId();
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpGet get = new HttpGet(end_point);
            get.setHeader("Accept", "application/json");
            get.setHeader("Authorization", "Bearer " + app.getSessionId());
            URI uri = new URIBuilder(get.getURI()).addParameter("recursive", "false").
                    addParameter("basic", "true").build();
            get.setURI(uri);
            response = httpClient.execute(get);
            JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
            if (response.getStatusLine().getStatusCode() == 200) {
                return new CTFDocumentFolder(this, data);
            } else {
                throw new CollabNetApp.
                        CollabNetAppException("getRootFolder for projectId " +
                        title +
                        " failed to find any folders");
            }
        } catch (IOException e) {
            throw new CollabNetApp.
                    CollabNetAppException("Error getting the root folder.");
        } catch (Exception e) {
            throw new CollabNetApp.
                    CollabNetAppException("Error getting the root folder.");
        } finally {
            if(response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        }
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