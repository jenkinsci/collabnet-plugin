package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.fault.NoSuchObjectFault;
import com.collabnet.ce.soap50.webservices.ClientSoapStubFactory;
import com.collabnet.ce.soap50.webservices.cemain.Group2SoapList;
import com.collabnet.ce.soap50.webservices.cemain.Group2SoapRow;
import com.collabnet.ce.soap50.webservices.cemain.ICollabNetSoap;
import com.collabnet.ce.soap50.webservices.cemain.ProjectSoapRow;
import com.collabnet.ce.soap50.webservices.cemain.UserSoapList;
import com.collabnet.ce.soap50.webservices.cemain.UserSoapRow;
import com.collabnet.ce.soap50.webservices.docman.IDocumentAppSoap;
import com.collabnet.ce.soap50.webservices.filestorage.IFileStorageAppSoap;
import com.collabnet.ce.soap50.webservices.frs.IFrsAppSoap;
import com.collabnet.ce.soap50.webservices.rbac.IRbacAppSoap;
import com.collabnet.ce.soap50.webservices.scm.IScmAppSoap;
import com.collabnet.ce.soap50.webservices.tracker.ITrackerAppSoap;
import hudson.plugins.collabnet.share.TeamForgeShare;
import hudson.plugins.collabnet.util.CNHudsonUtil;
import hudson.plugins.collabnet.util.CommonUtil;
import org.apache.axis.AxisFault;
import org.apache.log4j.Logger;
import org.kohsuke.stapler.QueryParameter;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/***
 * This class represents the connection to the CollabNet webservice.
 * Since it contains login/logout data, other webservices will
 * require an instance of it.
 * This is written based on the 5.0 version of the soap services.
 */
public class CollabNetApp {
    private static Logger logger = Logger.getLogger(CollabNetApp.class);
    public static String SOAP_SERVICE = "/ce-soap50/services/";
    private String sessionId;
    private String username;
    private String url;
    protected final ICollabNetSoap icns;
    private volatile IFrsAppSoap ifrs;
    private volatile IFileStorageAppSoap ifsa;
    private volatile ITrackerAppSoap itas;
    private volatile IDocumentAppSoap idas;
    private volatile IScmAppSoap isas;
    private volatile IRbacAppSoap iras;

    /**
     * Creates a new session to the server at the given url.
     *
     * @param url of the CollabNet server.
     * @param username to login as.
     * @param password to login with.
     * @throws RemoteException if we fail to login with the username/password
     */
    public CollabNetApp(String url, String username, String password)
        throws RemoteException {
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
        this.icns = this.getICollabNetSoap();
    }

    private <T> T createProxy(Class<T> type, String wsdlLoc) {
        String soapURL = this.getServerUrl() + SOAP_SERVICE + wsdlLoc + "?wsdl";
        return type.cast(ClientSoapStubFactory. getSoapStub(type, soapURL));
    }

    protected ITrackerAppSoap getTrackerSoap() {
        if (itas==null)
            itas = createProxy(ITrackerAppSoap.class, "TrackerApp");
        return itas;
    }

    protected IDocumentAppSoap getDocumentAppSoap() {
        if (idas==null)
            idas = createProxy(IDocumentAppSoap.class, "DocumentApp");
        return idas;
    }

    protected IScmAppSoap getScmAppSoap() {
        if (isas==null)
            isas = createProxy(IScmAppSoap.class, "ScmApp");
        return isas;
    }

    protected IRbacAppSoap getRbacAppSoap() {
        if (iras==null)
            iras = createProxy(IRbacAppSoap.class, "RbacApp");
        return iras;
    }

    protected IFrsAppSoap getFrsAppSoap() {
        if (ifrs==null)
            ifrs = createProxy(IFrsAppSoap.class, "FrsApp");
        return ifrs;
    }

    protected IFileStorageAppSoap getFileStorageAppSoap() {
        if (ifsa==null)
            ifsa = createProxy(IFileStorageAppSoap.class, "FileStorageApp");
        return ifsa;
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
     * @return client soap stub for the main CollabNet.wsdl.
     */
    private ICollabNetSoap getICollabNetSoap() {
        return getICollabNetSoap(url);
    }

    /**
     * @return client soap stub for an arbitrary url.
     */
    private static ICollabNetSoap getICollabNetSoap(String url) {
        String soapURL = url + CollabNetApp.SOAP_SERVICE +
            "CollabNet?wsdl";
        return (ICollabNetSoap) ClientSoapStubFactory.
            getSoapStub(ICollabNetSoap.class, soapURL);
    } 
    
    /**
     * Login is only done in the constructor.  If you need to
     * re-login, you should get a new CollabNetApp object.
     *
     * @param password used to login with.
     * @return a new sessionId.
     * @throws RemoteException
     */
    private String login(String password) throws RemoteException {
        sessionId = icns.login(this.username, password);
        return sessionId;
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
        this.icns.logoff(this.username, this.sessionId);
        this.sessionId = null;
    }

    public CTFFile upload(DataHandler src) throws RemoteException {
        return new CTFFile(this,this.getFileStorageAppSoap().uploadFile(getSessionId(),src));
    }

    /**
     * Uploads a file. The returned file object can be then used as an input
     * to methods like {@link CTFRelease#addFile(String, String, CTFFile)}.
     */
    public CTFFile upload(File src) throws RemoteException {
        return upload(new DataHandler(new FileDataSource(src)));
    }

    /**
     * @param url of the CollabNet server.
     * @return the API version number string.  This string is in the format 
     *         ${Release major}.${Release minor}.${service pack}.${hot fix}
     * 
     * @throws RemoteException
     */
    public static String getApiVersion(String url) throws RemoteException {
        return getICollabNetSoap(url).getApiVersion();
    }

    /**
     * @return the api version number string for CTF.
     *
     * @throws RemoteException if the call fails for some unknown reason
     */
    public String getApiVersion() throws RemoteException {
        return this.icns.getApiVersion();
    }

    /**
     * @return the version number string for SourceForge itself.
     * 
     * @throws RemoteException
     */
    public String getVersion() throws RemoteException {
        this.checkValidSessionId();
        return this.icns.getVersion(this.sessionId);
    }
    
    /**
     * Can the user can be found on the CollabNet server?
     *
     * @param username to check. 
     * @return true, if the user is found, false otherwise.
     * @throws RemoteException
     */
    public boolean isUsernameValid(String username) throws RemoteException {
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
    public CTFList<CTFGroup> getGroups() throws RemoteException {
        this.checkValidSessionId();
        CTFList<CTFGroup> r = new CTFList<CTFGroup>();
        Group2SoapList gsList = this.icns.getGroupList2(this.sessionId, null);
        for (Group2SoapRow row: gsList.getDataRows()) {
            r.add(new CTFGroup(this,row));
        }
        return r;
    }

    public CTFGroup getGroupByTitle(String fullName) throws RemoteException {
        return getGroups().byTitle(fullName);
    }

    public CTFGroup createGroup(String fullName, String description) throws RemoteException {
        return new CTFGroup(this,icns.createGroup(getSessionId(),fullName,description));
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
    public String createProject(String name, String title, String description) throws RemoteException {
        return this.icns.createProject(this.sessionId,name,title,description).getId();
    }

    /**
     * Return a collection of users that are active members of the group.
     *
     * @param groupId
     * @return active users (collection of usernames).
     * @throws RemoteException
     */
    public Collection<String> getGroupUsers(String groupId) 
        throws RemoteException {
        this.checkValidSessionId();
        Collection<String> users = new ArrayList<String>();
        UserSoapList usList = this.icns.getActiveGroupMembers(this.sessionId, 
                                                              groupId);
        for (UserSoapRow row: usList.getDataRows()) {
            users.add(row.getUserName());
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

    public CTFProject getProjectById(String projectId) throws RemoteException {
        return new CTFProject(this,icns.getProjectData(sessionId,projectId));
    }

    public List<CTFProject> getProjects() throws RemoteException {
        List<CTFProject> r = new ArrayList<CTFProject>();
        for (ProjectSoapRow row : icns.getProjectList(getSessionId()).getDataRows()) {
            r.add(new CTFProject(this,row));
        }
        return r;
    }

    public CTFProject getProjectByTitle(String title) throws RemoteException {
        for (CTFProject p : getProjects())
            if (p.getTitle().equals(title))
                return p;
        return null;
    }

    /**
     * Returns the current user that's logged in.
     */
    public CTFUser getMyself() throws RemoteException {
        return getUser(username);
    }

    /**
     * Retrieves the user, or null if no such user exists.
     */
    public CTFUser getUser(String username) throws RemoteException {
        try {
            return new CTFUser(this,this.icns.getUserData(getSessionId(),username));
        } catch (NoSuchObjectFault e) {
            return null;
        } catch (AxisFault e) {
            // somehow Axis is failing to create a strongly typed binding.
            if (NoSuchObjectFault.FAULT_CODE.equals(e.getFaultCode()))
                return null;
            throw e;
        }
    }

    /**
     * @param locale
     *      Locale of the new user (currently supported locales are "en" for English, "ja" for Japanese).
     * @param timeZone
     *      User's time zone. The ID for a TimeZone, either an abbreviation such as "PST", a full name such as "America/Los_Angeles", or a custom ID such as "GMT-8:00".
     */
    public CTFUser createUser(String username, String email, String fullName, String locale, String timeZone, boolean isSuperUser, boolean isRestrictedUser, String password) throws RemoteException {
        return new CTFUser(this,this.icns.createUser(getSessionId(),username,email,fullName,locale,timeZone,isSuperUser,isRestrictedUser,password));
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
     */
    public static CollabNetApp fromStapler(@QueryParameter boolean overrideAuth, @QueryParameter String url,
                                           @QueryParameter String username, @QueryParameter String password) {
        TeamForgeShare.TeamForgeShareDescriptor descriptor =
            TeamForgeShare.getTeamForgeShareDescriptor();
        if (descriptor != null && descriptor.useGlobal() && !overrideAuth) {
            url = descriptor.getCollabNetUrl();
            username = descriptor.getUsername();
            password = descriptor.getPassword();
        }

        if (CommonUtil.unset(url) || CommonUtil.unset(username) || CommonUtil.unset(password)) {
            return null;
        }
        return CNHudsonUtil.getCollabNetApp(url, username, password);
    }
}
