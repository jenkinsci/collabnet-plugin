package com.collabnet.ce.webservices;

import com.collabnet.ce.soap60.webservices.ClientSoapStub;
import com.collabnet.ce.soap60.webservices.ClientSoapStubFactory;
import com.collabnet.ce.soap60.webservices.cemain.ICollabNetSoap;

import hudson.RelativePath;
import hudson.plugins.collabnet.CollabNetPlugin;
import hudson.plugins.collabnet.CtfSoapHttpSender;
import hudson.plugins.collabnet.share.TeamForgeShare;
import hudson.plugins.collabnet.util.CNHudsonUtil;
import hudson.plugins.collabnet.util.CommonUtil;
import hudson.plugins.collabnet.util.Helper;
import hudson.util.Secret;

import org.apache.axis.EngineConfiguration;
import org.apache.axis.SimpleTargetedChain;
import org.apache.axis.configuration.SimpleProvider;

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
    protected final ICollabNetSoap icns;
    private transient Integer soapTimeout;

    static Logger logger = Logger.getLogger(CollabNetApp.class.getName());

    Helper helper = new Helper();

    static {
        EngineConfiguration engCfg = getEngineConfiguration();
        if (engCfg != null) {
            ClientSoapStubFactory.setConfig(engCfg);
        }
    }

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
        this.icns = null;
    }

    private <T> T createProxy(Class<T> type, String wsdlLoc) {
        int to = -1;
        if (soapTimeout == null) {
            soapTimeout = getSoapTimeout();
        }
        to = soapTimeout.intValue();
        return createProxy(type, getServerUrl(), wsdlLoc, to);
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
     * Creates ClientSoapStub for the requested type
     * @param type TeamForge soap application type
     * @param serverUrl TeamForge URL
     * @param wsdlLoc wsdl location
     * @param timeout soap timeout in milliseconds
     * @return
     */
    private static <T> T createProxy(Class<T> type, String serverUrl, String wsdlLoc, int timeout) {
        String soapUrl = serverUrl + CollabNetApp.SOAP_SERVICE + wsdlLoc + "?wsdl";
        ClientSoapStub s = (timeout <= 0 ?
                ClientSoapStubFactory.getSoapStub(type, soapUrl) :
                    ClientSoapStubFactory.getSoapStub(type, soapUrl, timeout));
        return type.cast(s);
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
     * @return sessionId
     * @throws RemoteException
     */
    public void loginWithToken(String token) 
        throws RemoteException {
        this.sessionId = icns.loginWithToken(this.username, token);
    }
    
    /**
     * Logoff for this user and invalidate the sessionId.
     *
     * @throws RemoteException
     */
    public void logoff() throws RemoteException {
        this.checkValidSessionId();
        this.sessionId = null;
    }

    /**
     * Uploads a file. The returned file object can be then used as an input
     * to methods like {@link CTFRelease#addFile(String, String, CTFFile)}.
     */
    public CTFFile upload(File src) throws IOException {
        String end_point =  url + CTFConstants.FILE_STORAGE_URL;
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
            Response responsec = builder
                    .post(Entity.entity(multiPart, MediaType.MULTIPART_FORM_DATA));
            if (responsec.getStatus() == 200) {
                String respnse = responsec.readEntity(String.class);
                JSONObject data = (JSONObject) new JSONParser().parse(respnse);
                return new CTFFile(this, data.get("guid").toString());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error uploading a file" + e.getLocalizedMessage(), e);
        }
        return null;
    }
    
    /**
     * Can the user can be found on the CollabNet server?
     *
     * @param username to check. 
     * @return true, if the user is found, false otherwise.
     * @throws RemoteException
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
     * @throws RemoteException 
     */
    public CTFList<CTFGroup> getGroups() throws IOException {
        this.checkValidSessionId();
        CTFList<CTFGroup> r = new CTFList<CTFGroup>();
        String end_point =  url + CTFConstants.FOUNDATION_URL + "groups?sortby=id&offset=0&count=25";
        Response response = helper.request(end_point, sessionId, null, HttpMethod.GET, null);
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
                        r.add(new CTFGroup(this, jsonObject));
                    }
                }
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in getGroups() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING, "Error getting the group lists - " + status  + ", Error Msg - " + result);
        }
        return r;
    }

    public CTFGroup getGroupByTitle(String fullName) throws IOException {
        return getGroups().byTitle(fullName);
    }

    public CTFGroup createGroup(String fullName, String description) throws IOException {
        String end_point =  url + CTFConstants.FOUNDATION_URL + "groups";
        JSONObject requestPayload = new JSONObject();
        requestPayload.put("fullname", fullName);
        requestPayload.put("description", description);
        Response response = helper.request(end_point, sessionId, requestPayload.toString(), HttpMethod.POST, null);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status == 200) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                return new CTFGroup(this, data);
            } catch (ParseException e) {
                logger.log(Level.WARNING,"Unable to parse the json content in createGroup()" + e.getLocalizedMessage());
            }
        } else {
            logger.log(Level.WARNING,"Error creating an user group - " + status  + ", Error Msg - " + result);
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
        if (status == 200) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                projectId = data.get("id").toString();
            } catch (ParseException e) {
                logger.log(Level.WARNING,"Unable to parse the json content in createProject()" + e.getLocalizedMessage());
            }
        } else {
            logger.log(Level.WARNING,"Error creating a project - " + status  + ", Error Msg - " + result);
        }
        return projectId;
    }

    /**
     * Return a collection of users that are active members of the group.
     *
     * @param groupId
     * @return active users (collection of usernames).
     * @throws RemoteException
     */
    public Collection<String> getGroupUsers(String groupId) 
        throws IOException {
        this.checkValidSessionId();
        Collection<String> users = new ArrayList<String>();
        String end_point =  url + CTFConstants.FOUNDATION_URL + "groups/" + groupId + "/members";
        Response response = helper.request(end_point, sessionId, null, HttpMethod.GET, null);
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
                        users.add(jsonObject.get("username").toString());
                    }
                }
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in getGroupUsers() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING, "Error getting the active members of the group - " + status + ", Error Msg - " + result);
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

    public CTFProject getProjectById(String projectId) throws IOException {
        CTFProject ctfProject = null;
        String end_point =  url + CTFConstants.FOUNDATION_URL + "projects/" + projectId;
        Response response = helper.request(end_point, sessionId, null, HttpMethod.GET, null);
        String result = response.readEntity(String.class);
        int status = response.getStatus();
        if (status == 200) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                ctfProject = new CTFProject(this, data);
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in getProjectById() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING, "Error getting the project details - " + status  + ", Error Msg - " + result);
        }
        return ctfProject;
    }

    public List<CTFProject> getProjects() throws IOException {
        List<CTFProject> r = new ArrayList<CTFProject>();
        String end_point =  url + CTFConstants.FOUNDATION_URL + "projects";
        Map<String, String> queryParam = new HashMap<>();
        queryParam.put("fetchHierarchyPath", "false");
        queryParam.put("count", "-1");
        Response response = helper.request(end_point, sessionId, null, HttpMethod.GET, queryParam);
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
                        r.add(new CTFProject(this, jsonObject));
                    }
                }
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Unable to parse the json content in getProjects() - " + e.getLocalizedMessage(), e);
            }
        } else {
            logger.log(Level.WARNING, "Error getting the projects - " + status + ", Error Msg - " + result);
        }
        return r;
    }

    public CTFProject getProjectByTitle(String title) throws IOException {
        for (CTFProject p : getProjects())
            if (p.getTitle().equals(title))
                return p;
        return null;
    }

    /**
     * Returns the current user that's logged in.
     */
    public CTFUser getMyself() throws IOException {
        return getUser(username);
    }

    public CTFUser getMyselfData() throws IOException {
        return new CTFUser(this, helper.getUserData(this.url, this.sessionId, username));
    }

    /**
     * Retrieves the user, or null if no such user exists.
     */
    public CTFUser getUser(String username) throws IOException {
        try {
            return new CTFUser(this, helper.getUserData(this.url, getSessionId(),username));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * @param locale
     *      Locale of the new user (currently supported locales are "en" for English, "ja" for Japanese).
     * @param timeZone
     *      User's time zone. The ID for a TimeZone, either an abbreviation such as "PST", a full name such as "America/Los_Angeles", or a custom ID such as "GMT-8:00".
     */
    public CTFUser createUser(String username, String email, String fullName, String locale, String timeZone, boolean isSuperUser, boolean isRestrictedUser, String password) throws IOException {
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
        if (status == 200) {
            JSONObject data = null;
            try {
                data = (JSONObject) new JSONParser().parse(result);
                ctfUser = new CTFUser(this, data);
            } catch (ParseException e) {
                logger.log(Level.WARNING,"Unable to parse the json content in createUser() " + e.getLocalizedMessage());
            }
        } else {
            logger.log(Level.WARNING,"Error creating an user " + status  + ", Error Msg - " + result);
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

    public static EngineConfiguration getEngineConfiguration() {
        SimpleProvider config = null;
        if (CollabNetApp.areSslErrorsIgnored()) {
            config = new SimpleProvider();
            config.deployTransport("https", new SimpleTargetedChain(new CtfSoapHttpSender())); //$NON-NLS-1$
            config.deployTransport("http", new SimpleTargetedChain(new CtfSoapHttpSender())); //$NON-NLS-1$
        }
        return config;
    }

    public static boolean areSslErrorsIgnored() {
        return Boolean.getBoolean(CollabNetPlugin.class.getName() + ".skipSslValidation");
    }

    public static int getSoapTimeout() {
        return Integer.getInteger(
                CollabNetPlugin.class.getName() + ".soapTimeout", -1).intValue();
    }
}
