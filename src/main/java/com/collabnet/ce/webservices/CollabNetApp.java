package com.collabnet.ce.webservices;

import com.collabnet.ce.soap60.webservices.ClientSoapStub;
import com.collabnet.ce.soap60.webservices.ClientSoapStubFactory;
import com.collabnet.ce.soap60.webservices.cemain.ICollabNetSoap;

import hudson.RelativePath;
import hudson.plugins.collabnet.CollabNetPlugin;
import hudson.plugins.collabnet.CtfSoapHttpSender;
import hudson.plugins.collabnet.share.TeamForgeShare;
import hudson.plugins.collabnet.util.CNFormFieldValidator;
import hudson.plugins.collabnet.util.CNHudsonUtil;
import hudson.plugins.collabnet.util.CommonUtil;
import hudson.plugins.collabnet.util.Helper;
import hudson.util.Secret;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.SimpleTargetedChain;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;

import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.kohsuke.stapler.QueryParameter;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
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

    private static final String FOUNDATION_URL = "/ctfrest/foundation/v1/";
    private static final String FILE_STORAGE_URL = "/ctfrest/filestorage/v1/files";
    static Logger logger = Logger.getLogger(CollabNetApp.class.getName());

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
        sessionId = Helper.getToken(new URL(this.url), this.username, password);
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
        String end_point =  url + FILE_STORAGE_URL;
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = CNFormFieldValidator.getHttpClient();
            Client clientc = ClientBuilder.newBuilder().
                    register(MultiPartFeature.class).build();
            WebTarget server = clientc.target(end_point);
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
                JSONObject data = JSONObject.fromObject(respnse);
                return new CTFFile(this, data.get("guid").toString());
            }
        } catch (Exception e) {
            logger.log(Level.INFO,"Error uploading a file" + e.getLocalizedMessage(), e);
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
        String end_point =  url + FOUNDATION_URL + "groups?sortby=id&offset=0&count=25";
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpGet get = new HttpGet(end_point);
            get.setHeader("Accept", "application/json");
            get.setHeader("Authorization", "Bearer " + sessionId);
            response = httpClient.execute(get);
            JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
            JSONArray dataArray = JSONArray.fromObject(data.get("items"));
            Iterator it = dataArray.iterator();
            while (it.hasNext()) {
                JSONObject jsonObject = (JSONObject) it.next();
                r.add(new CTFGroup(this, jsonObject));
            }
        } catch (Exception e) {
            logger.log(Level.INFO,"Error getting the group lists - " + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        }
        return r;
    }

    public CTFGroup getGroupByTitle(String fullName) throws IOException {
        return getGroups().byTitle(fullName);
    }

    public CTFGroup createGroup(String fullName, String description) throws IOException {
        String end_point =  url + FOUNDATION_URL + "groups";
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = CNFormFieldValidator.getHttpClient();
            HttpPost httpPost = new HttpPost(end_point);
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("fullname", fullName));
            params.add(new BasicNameValuePair("description", description));
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " +this.sessionId);
            response = client.execute(httpPost);
            if(response.getStatusLine().getStatusCode() == 200) {
                JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
                return new CTFGroup(this, data);
            }
        } catch (IOException e) {
            String errMsg = e.getMessage();
            logger.log(Level.INFO, errMsg);
        } catch (Exception e) {
            logger.log(Level.INFO,"Error creating an user group" + e.getLocalizedMessage(), e);
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
        String end_point =  url + FOUNDATION_URL + "projects";
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        String projectId = null;
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
            httpPost.setHeader("Authorization", "Bearer " +this.sessionId);
            response = client.execute(httpPost);
            if(response.getStatusLine().getStatusCode() == 200) {
                JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
                projectId = data.get("id").toString();
            }
        } catch (IOException e) {
            String errMsg = e.getMessage();
            logger.log(Level.INFO, errMsg);
        } catch (Exception e) {
            logger.log(Level.INFO,"Error creating an user group" + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if(client != null) {
                client.close();
            }
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
        String end_point =  url + FOUNDATION_URL + "groups/" + groupId + "/members";
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpGet get = new HttpGet(end_point);
            get.setHeader("Accept", "application/json");
            get.setHeader("Authorization", "Bearer " + sessionId);
            response = httpClient.execute(get);
            JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
            JSONArray dataArray = JSONArray.fromObject(data.get("items"));
            Iterator it = dataArray.iterator();
            while (it.hasNext()) {
                JSONObject jsonObject = (JSONObject) it.next();
                users.add(jsonObject.get("username").toString());
            }
        } catch (Exception e) {
            logger.log(Level.INFO,"TeamForge Associations - " + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
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
        String end_point =  url + FOUNDATION_URL + "projects/" + projectId;
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpGet get = new HttpGet(end_point);
            get.setHeader("Accept", "application/json");
            get.setHeader("Authorization", "Bearer " + sessionId);
            response = httpClient.execute(get);
            JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
            ctfProject = new CTFProject(this, data);
        } catch (Exception e) {
            logger.log(Level.INFO,"Error getting the members of a group" + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
        }
        return ctfProject;
    }

    public List<CTFProject> getProjects() throws IOException {
        List<CTFProject> r = new ArrayList<CTFProject>();
        String end_point =  url + FOUNDATION_URL + "projects";
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = CNFormFieldValidator.getHttpClient();
            HttpGet get = new HttpGet(end_point);
            get.setHeader("Accept", "application/json");
            get.setHeader("Content-type", "application/json");
            get.setHeader("Authorization", "Bearer " + sessionId);
            URI uri = new URIBuilder(get.getURI()).addParameter(
                    "fetchHierarchyPath", "false").build();
            get.setURI(uri);
            response = httpClient.execute(get);
            JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
            JSONArray dataArray = JSONArray.fromObject(data.get("items"));
            Iterator it = dataArray.iterator();
            while (it.hasNext()) {
                JSONObject jsonObject = (JSONObject) it.next();
               r.add(new CTFProject(this, jsonObject));
            }
        } catch (Exception e) {
            logger.log(Level.INFO,"Error getting the project details - " + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if (httpClient != null) {
                httpClient.close();
            }
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
        return new CTFUser(this, Helper.getUserData(this.url, this.sessionId, username));
    }

    /**
     * Retrieves the user, or null if no such user exists.
     */
    public CTFUser getUser(String username) throws IOException {
        try {
            return new CTFUser(this, Helper.getUserData(this.url, getSessionId(),username));
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
        String end_point =  url + FOUNDATION_URL + "users";
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = CNFormFieldValidator.getHttpClient();
            HttpPost httpPost = new HttpPost(end_point);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " +this.sessionId);
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("username", username));
            params.add(new BasicNameValuePair("email", email));
            params.add(new BasicNameValuePair("fullname", fullName));
            params.add(new BasicNameValuePair("locale", locale));
            params.add(new BasicNameValuePair("timeZone", timeZone));
            params.add(new BasicNameValuePair("organization", organization));
            params.add(new BasicNameValuePair("licenseTypes", licenseType));
            params.add(new BasicNameValuePair("superUser", Boolean.toString(isSuperUser)));
            params.add(new BasicNameValuePair("restrictedUser",  Boolean.toString(isRestrictedUser)));
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            response = client.execute(httpPost);
            if(response.getStatusLine().getStatusCode() == 200) {
                JSONObject data = JSONObject.fromObject(EntityUtils.toString(response.getEntity()));
                ctfUser = new CTFUser(this, data);
            }
        }catch (IOException e) {
            String errMsg = e.getMessage();
            logger.log(Level.INFO, errMsg);
        } catch (Exception e) {
            logger.log(Level.INFO,"Error creating an user " + e.getLocalizedMessage(), e);
        } finally {
            if(response != null) {
                response.close();
            }
            if(client != null) {
                client.close();
            }
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
