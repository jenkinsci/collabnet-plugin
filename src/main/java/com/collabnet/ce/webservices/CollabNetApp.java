package com.collabnet.ce.webservices;

import hudson.RelativePath;
import hudson.plugins.collabnet.CollabNetPlugin;
import hudson.plugins.collabnet.CtfSoapHttpSender;
import hudson.plugins.collabnet.share.TeamForgeShare;
import hudson.plugins.collabnet.util.CNHudsonUtil;
import hudson.plugins.collabnet.util.CommonUtil;
import hudson.plugins.collabnet.util.Helper;
import hudson.util.Secret;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.kohsuke.stapler.QueryParameter;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/***
 * This class represents the connection to the CollabNet webservice.
 * Since it contains login/logout data, other webservices will
 * require an instance of it.
 * This is written based on the 5.0 version of the soap services.
 */
public class CollabNetApp {
    public static String SOAP_SERVICE = "/ce-soap60/services/";
    private String sessionId;
    private String username;
    private String url;

    static Logger logger = Logger.getLogger(CollabNetApp.class.getName());

    Helper helper = new Helper();

    /**
     * Creates a new session to the server at the given url.
     *
     * @param url of the CollabNet server.
     * @param username to login as.
     * @param password to login with.
     * @throws RemoteException if we fail to login with the username/password
     */
    public CollabNetApp(String url, String username, String password)
        throws IOException {
        this(url, username);
        this.sessionId = this.login(password);
    }

    /**
     * Creates a new session to the server without actually authenticating, relying only on values passed in.
     *
     * @param url of the CollabNet server.
     * @param username to login as.
     * @param password to login with.
     * @param sessionId the session id
     */
    public CollabNetApp(String url, String username, String password, String sessionId) {
        this(url, username);
        this.sessionId = sessionId;
    }

    /**
     * Creates a new CollabNetApp without a session.
     *
     * @param url of the CollabNet server.
     * @param username to login as.
     */
    public CollabNetApp(String url, String username) {
        this(url);
        this.username = username;
    }

    /**
     * Creates a new collabnet app
     * @param url url of the CollabNet server
     */
    public CollabNetApp(String url) {
        this.url = url;
    }
    /**
     * Returns the user name that this connection is set up with.
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * @return the session id.
     */
    public String getSessionId() {
        return this.sessionId;
    }
    
    /**
     * @return the url of the CollabNet server.
     */
    public String getServerUrl() {
        return this.url;
    }
    
    /**
     * Login is only done in the constructor.  If you need to
     * re-login, you should get a new CollabNetApp object.
     *
     * @param password used to login with.
     * @return a new sessionId.
     * @throws RemoteException
     */
    private String login(String password) throws IOException {
        sessionId = helper.getToken(new URL(this.url), this.username, password);
        return sessionId;
    }

  /**
   * Return N bytes from provided buffer
   * @param buffer to be read
   * @param n no.of bytes to be read from buffer
   * @return resulting buffer
   */
    private byte[] getFirstNBytesOfBuffer(byte[] buffer, int n) {
      if (buffer.length == n) {
        return buffer;
      } else {
        return Arrays.copyOfRange(buffer, 0, n);
      }
    }

  /**
     * Login with a token.
     *
     * @param token one-time token
     * @throws IOException if any problems occurs reading/writing file
     * @throws MalformedURLException if TeamForge URL is invalid
     * @throws RemoteException If unexpected system error occurs
     */
    public void loginWithToken(String token)
            throws IOException, MalformedURLException, RemoteException {
        this.sessionId = helper.getSessionId(new URL(this.url), token);
    }
    
    /**
     * Logoff for this user and invalidate the sessionId.
     *
     * @throws RemoteException If unexpected system error occurs
     */
    public void logoff() throws RemoteException {
        this.checkValidSessionId();
        this.sessionId = null;
    }

    /**
     * Uploads a file. The returned file object can be then used as an input
     * to methods like {@link CTFRelease#addFile(String, String, CTFFile)}.
     *
     * @param src The file to upload
     * @return CTFFile object
     * @throws IOException if any problems occurs reading/writing file
     */
    public CTFFile upload(File src) throws IOException {
        String end_point =  url + CTFConstants.FILE_STORAGE_URL;
        int status = 0;
        String result =null;
        try {
            Client client = ClientBuilder.newBuilder().
                    register(MultiPartFeature.class).build();
            WebTarget server = client.target(end_point);
            Invocation.Builder builder = server.request(MediaType.APPLICATION_JSON_TYPE);
            builder.header("Content-Type", "multipart/form-data");
            builder.header("accept", "application/json");
            builder.header("Authorization" , "Bearer " + this.sessionId);
            MultiPart multiPart = new MultiPart();
            FileDataBodyPart pdfBodyPart = new FileDataBodyPart("file", src,
                    MediaType.APPLICATION_OCTET_STREAM_TYPE);
            multiPart.bodyPart(pdfBodyPart);
            Response response = builder
                    .post(Entity.entity(multiPart, MediaType.MULTIPART_FORM_DATA));
            status = response.getStatus();
            result = response.readEntity(String.class);
            if (status < 300) {
                JSONObject data = (JSONObject) new JSONParser().parse(result);
                return new CTFFile(this, data.get("guid").toString());
            } else {
                logger.log(Level.WARNING, "Error uploading a file, response code - " + status);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error uploading a file" + e.getLocalizedMessage(), e);
            throw new IOException("Error uploading a file - " + status + ", Error Msg - " + helper.getErrorMessage(result));
        }
        return null;
    }
    
    /**
     * Can the user can be found on the CollabNet server?
     *
     * @param username to check. 
     * @return true, if the user is found, false otherwise.
     * @throws IOException if any problems occurs reading/writing file
     */
    public boolean isUsernameValid(String username) throws IOException {
        this.checkValidSessionId();
        return getUser(username)!=null;
    }
    
    /**
     * Get the list of all Groups on the system.
     * Can only be called by SuperUsers.
     *
     * @return a Map of all group name/ids.
     * @throws IOException if any problems occurs reading/writing file
     */
    public CTFList<CTFGroup> getGroups() throws IOException {
        this.checkValidSessionId();
        CTFList<CTFGroup> r = new CTFList<CTFGroup>();
        String end_point =  url + CTFConstants.FOUNDATION_URL + "groups?sortby=id&offset=0&count=25";
        Response response = helper.request(end_point, sessionId, null, HttpMethod.GET, null);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status < 300) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                if (data != null & data.containsKey("items")) {
                    JSONArray dataArray = (JSONArray)data.get("items");
                    Iterator it = dataArray.iterator();
                    while (it.hasNext()) {
                        JSONObject jsonObject = (JSONObject) it.next();
                        if (jsonObject != null) {
                            r.add(new CTFGroup(this, jsonObject));
                        }
                    }
                }
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in getGroups() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING, "Error getting the group lists - " + status  + ", Error Msg - " + result);
            throw new IOException("Error getting the group lists - " + status + ", Error Msg - " + helper.getErrorMessage(result));
        }
        return r;
    }

    /**
     * Get the Group for given fullName.
     *
     * @param fullName the fullName of group 
     * @return a CTFGroup object
     * @throws IOException if any problems occurs reading/writing file
     */
    public CTFGroup getGroupByTitle(String fullName) throws IOException {
        return getGroups().byTitle(fullName);
    }

    /**
     * Creates Group.
     *
     * @param fullName the fullName of group 
     * @param description the description of group 
     * @return Group object.
     * @throws IOException if any problems occurs reading/writing file
     */
    public CTFGroup createGroup(String fullName, String description) throws IOException {
        String end_point =  url + CTFConstants.FOUNDATION_URL + "groups";
        JSONObject requestPayload = new JSONObject();
        requestPayload.put("fullname", fullName);
        requestPayload.put("description", description);
        Response response = helper.request(end_point, sessionId, requestPayload.toString(), HttpMethod.POST, null);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status < 300) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                return new CTFGroup(this, data);
            } catch (ParseException e) {
                logger.log(Level.WARNING,"Unable to parse the json content in createGroup()" + e.getLocalizedMessage());
            }
        } else {
            logger.log(Level.WARNING,"Error creating an user group - " + status  + ", Error Msg - " + result);
            throw new IOException("Error creating an user group - " + status + ", Error Msg - " + helper.getErrorMessage(result));
        }
        return null;
    }

    /**
     * Creates a new project and obtains its ID.
     *
     * @param name
     *      ID of the project. Used as a token in URL. Can be null, in which case
     *      inferred from the title parameter.
     * @param title
     *      Human readable title of the project that can include whitespace and so on.
     * @param description
     *      Longer human readable description of the project.
     * @throws IOException if any problems occurs reading/writing file
     * @return project id
     */
    public String createProject(String name, String title, String description) throws IOException {
        String projectId = null;
        String end_point =  url + CTFConstants.FOUNDATION_URL + "projects";
        JSONObject requestPayload = new JSONObject();
        requestPayload.put("name", name);
        requestPayload.put("title", title);
        requestPayload.put("description", description);
        Response response = helper.request(end_point, sessionId, requestPayload.toString(), HttpMethod.POST, null);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status < 300) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                projectId = data.get("id").toString();
            } catch (ParseException e) {
                logger.log(Level.WARNING,"Unable to parse the json content in createProject()" + e.getLocalizedMessage());
            }
        } else {
            logger.log(Level.WARNING,"Error creating a project - " + status  + ", Error Msg - " + result);
            throw new IOException("Error creating a project - " + status + ", Error Msg - " + helper.getErrorMessage(result));
        }
        return projectId;
    }

    /**
     * Return a collection of users that are active members of the group.
     *
     * @param groupId groupId
     * @return active users (collection of usernames).
     * @throws IOException if any problems occurs reading/writing file
     */
    public Collection<String> getGroupUsers(String groupId) 
        throws IOException {
        this.checkValidSessionId();
        Collection<String> users = new ArrayList<String>();
        String end_point =  url + CTFConstants.FOUNDATION_URL + "groups/" + groupId + "/members";
        Response response = helper.request(end_point, sessionId, null, HttpMethod.GET, null);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status < 300) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                if (data != null & data.containsKey("items")) {
                    JSONArray dataArray = (JSONArray)data.get("items");
                    Iterator it = dataArray.iterator();
                    while (it.hasNext()) {
                        JSONObject jsonObject = (JSONObject) it.next();
                        if (jsonObject != null) {
                            users.add(jsonObject.get("username").toString());
                        }
                    }
                }
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in getGroupUsers() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING, "Error getting the active members of the group - " + status + ", Error Msg - " + result);
            throw new IOException("Error getting the active members of the group - " + status + ", Error Msg - " + helper.getErrorMessage(result));
        }
        return users;
    }
    
    /**
     * Throws a CollabNetAppException if there is no current sessionId.
     */
    public void checkValidSessionId() {
        if (this.sessionId == null) {
            throw new CollabNetApp.CollabNetAppException("Not currently in " +
                                                         "a valid session.");
        }
    }

    /**
     * Returns project object for given projectId 
     *
     * @param projectId the teamforge project id or path
     * @return project object
     * @throws IOException if any problems occurs reading project data
     */
    public CTFProject getProjectById(String projectId) throws IOException {
        CTFProject ctfProject = null;
        String end_point =  url + CTFConstants.FOUNDATION_URL + "projects/" + projectId;
        Response response = helper.request(end_point, sessionId, null, HttpMethod.GET, null);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status < 300) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                ctfProject = new CTFProject(this, data);
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in getProjectById() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING, "Error getting the project details - " + status  + ", Error Msg - " + result);
            throw new IOException("Error getting the project details - " + status + ", Error Msg - " + helper.getErrorMessage(result));
        }
        return ctfProject;
    }

    /**
     * Return a collection of projects
     *
     * @return list of projects
     * @throws IOException if any problems occurs reading projects data
     */
    public List<CTFProject> getProjects() throws IOException {
        List<CTFProject> r = new ArrayList<CTFProject>();
        String end_point =  url + CTFConstants.FOUNDATION_URL + "projects";
        Map<String, String> queryParam = new HashMap<>();
        queryParam.put("fetchHierarchyPath", "false");
        queryParam.put("count", "-1");
        Response response = helper.request(end_point, sessionId, null, HttpMethod.GET, queryParam);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status < 300) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                if (data != null & data.containsKey("items")) {
                    JSONArray dataArray = (JSONArray)data.get("items");
                    Iterator it = dataArray.iterator();
                    while (it.hasNext()) {
                        JSONObject jsonObject = (JSONObject) it.next();
                        if (jsonObject != null) {
                            r.add(new CTFProject(this, jsonObject));
                        }
                    }
                }
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in getProjects() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING, "Error getting the projects - " + status + ", Error Msg - " + result);
            throw new IOException("Error getting the projects - " + status + ", Error Msg - " + helper.getErrorMessage(result));
        }
        return r;
    }

    /**
     * Returns project data for given project title
     *
     * @param title project title
     * @return project data
     * @throws IOException if any problems occurs reading project data
     */
    public CTFProject getProjectByTitle(String title) throws IOException {
        for (CTFProject p : getProjects())
            if (p != null) {
                if (p.getTitle().equals(title))
                    return p;
            }
        return null;
    }

    /**
     * Returns the current user that's logged in.
     *
     * @return current user data
     * @throws IOException if any problems occurs reading user data
     */
    public CTFUser getMyself() throws IOException {
        return getUser(username);
    }

    /**
     * Returns the current user that's logged in.
     *
     * @return current user data
     * @throws IOException if any problems occurs reading user data
     */
    public CTFUser getMyselfData() throws IOException {
        return new CTFUser(this, helper.getUserData(this.url, this.sessionId, username));
    }

    /**
     * Retrieves the user with the specified username, or null if no such user exists.
     *
     * @param username the username for which the user data to be retrived
     * @return user data for specified username
     * @throws IOException if any problems occurs reading user data
     */
    public CTFUser getUser(String username) throws IOException {
        try {
            return new CTFUser(this, helper.getUserData(this.url, getSessionId(),username));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Creates new teamforge user
     * 
     * @param username the name of the user to be created
     * @param email the mail id of the user
     * @param fullName the fullname of the user
     * @param locale
     *      Locale of the new user (currently supported locales are "en" for English, "ja" for Japanese).
     * @param timeZone
     *      User's time zone. The ID for a TimeZone, either an abbreviation such as "PST",
     *      a full name such as "America/Los_Angeles", or a custom ID such as "GMT-8:00".
     * @param isSuperUser boolean flag
     * @param isRestrictedUser boolean flag
     * @param password the password of the user
     * @return user data 
     * @throws IOException if any problems occurs reading/writing user data
     */
    public CTFUser createUser(String username, String email, String fullName, String locale, String timeZone,
                                   boolean isSuperUser, boolean isRestrictedUser, String password) throws IOException {
    	String organization = null;
    	String licenseType = "ALM";
        CTFUser ctfUser = null;
        String end_point =  url + CTFConstants.FOUNDATION_URL + "users";
        JSONObject requestPayload = new JSONObject();
        requestPayload.put("username", username);
        requestPayload.put("email", email);
        requestPayload.put("fullname", fullName);
        requestPayload.put("locale", locale);
        requestPayload.put("timeZone", timeZone);
        requestPayload.put("organization", organization);
        requestPayload.put("licenseTypes", licenseType);
        requestPayload.put("superUser", Boolean.toString(isSuperUser));
        requestPayload.put("restrictedUser",  Boolean.toString(isRestrictedUser));
        Response response = helper.request(end_point, sessionId, requestPayload.toString(), HttpMethod.POST, null);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status < 300) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                ctfUser = new CTFUser(this, data);
            } catch (ParseException e) {
                logger.log(Level.WARNING,"Unable to parse the json content in createUser() " + e.getLocalizedMessage());
            }
        } else {
            logger.log(Level.WARNING,"Error creating an user " + status  + ", Error Msg - " + result);
            throw new IOException("Error creating an user - " + status + ", Error Msg - " + helper.getErrorMessage(result));
        }
        return  ctfUser;
    }

    /**
     * Exception class to throw when something unexpected goes wrong.
     */
    public static class CollabNetAppException extends RuntimeException{
        public CollabNetAppException(String msg) {
            super(msg);
        }
    }

    /**
     * A databinding method from Stapler.
     * @param connectionFactory relates to the checkbox to override the global authentication parameters
     */
    public static CollabNetApp fromStapler(@QueryParameter boolean connectionFactory, 
            @QueryParameter @RelativePath("connectionFactory") String url,
            @QueryParameter @RelativePath("connectionFactory") String username, 
            @QueryParameter @RelativePath("connectionFactory") String password) {
        
        // form may contain password either entered by user or the encrypted value
        password = Secret.fromString(password).getPlainText();
        
        TeamForgeShare.TeamForgeShareDescriptor descriptor =
            TeamForgeShare.getTeamForgeShareDescriptor();
        if (descriptor != null && descriptor.useGlobal() && !connectionFactory) {
            url = descriptor.getCollabNetUrl();
            username = descriptor.getUsername();
            password = descriptor.getPassword();
        }

        if (CommonUtil.unset(url) || CommonUtil.unset(username) || CommonUtil.unset(password)) {
            return null;
        }
        return CNHudsonUtil.getCollabNetApp(url, username, password);
    }

    public static boolean areSslErrorsIgnored() {
        return Boolean.getBoolean(CollabNetPlugin.class.getName() + ".skipSslValidation");
    }
}
