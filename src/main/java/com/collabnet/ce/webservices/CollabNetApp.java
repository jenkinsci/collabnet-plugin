package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.ClientSoapStubFactory;

import com.collabnet.ce.soap50.webservices.cemain.ICollabNetSoap;
import com.collabnet.ce.soap50.webservices.cemain.GroupSoapList;
import com.collabnet.ce.soap50.webservices.cemain.GroupSoapRow;
import com.collabnet.ce.soap50.webservices.cemain.Group2SoapList;
import com.collabnet.ce.soap50.webservices.cemain.Group2SoapRow;
import com.collabnet.ce.soap50.webservices.cemain.ProjectMemberSoapList;
import com.collabnet.ce.soap50.webservices.cemain.ProjectMemberSoapRow;
import com.collabnet.ce.soap50.webservices.cemain.ProjectSoapDO;
import com.collabnet.ce.soap50.webservices.cemain.ProjectSoapList;
import com.collabnet.ce.soap50.webservices.cemain.ProjectSoapRow;
import com.collabnet.ce.soap50.webservices.cemain.UserSoapList;
import com.collabnet.ce.soap50.webservices.cemain.UserSoapRow;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import hudson.plugins.collabnet.share.TeamForgeShare;
import hudson.plugins.collabnet.util.CNHudsonUtil;
import hudson.plugins.collabnet.util.CommonUtil;
import org.apache.log4j.Logger;
import org.kohsuke.stapler.QueryParameter;

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
    private ICollabNetSoap icns;

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
        String soapURL = this.url + CollabNetApp.SOAP_SERVICE +
            "CollabNet?wsdl";
        return (ICollabNetSoap) ClientSoapStubFactory.
            getSoapStub(ICollabNetSoap.class, soapURL);
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
     * Find the project that matches the given name, and return it's id.
     * 
     * @param projectName
     * @return id for this project (if a match is found), null otherwise.
     * @throws RemoteException
     */
    public String getProjectId(String projectName) throws RemoteException {
        this.checkValidSessionId();
        ProjectSoapList pslist = this.icns.getProjectList(sessionId);
        ProjectSoapRow[] rows = pslist.getDataRows();
        for (ProjectSoapRow row : rows) {
            logger.debug(row.getId() + " " + row.getTitle());
            if (row.getTitle().equals(projectName)) {
                return row.getId();
            }
        }
        return null;
    }

    /**
     * Find the project that matches the given name, and return it's id.
     *
     * @param projectId
     * @return name for this project (if a match is found), null otherwise.
     */
    public String getProjectName(String projectId) {
        this.checkValidSessionId();
        try {
            ProjectSoapDO projectDO = icns.getProjectData(sessionId, projectId);
            return projectDO.getTitle();
        } catch (RemoteException e) {
            return null;
        }
    }

    /**
     * Return the list of project names.
     *
     * @return a Collection of project names.
     * @throws RemoteException
     */
    public Collection<String> getProjects() throws RemoteException {
        this.checkValidSessionId();
        ProjectSoapList pslist = this.icns.getProjectList(sessionId);
        Collection<String> names = new ArrayList<String>();
        for (ProjectSoapRow row: pslist.getDataRows()) {
            names.add(row.getTitle());
        }
        return names;
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
        UserSoapList usList = this.icns.findUsers(this.sessionId, username);
        for (UserSoapRow row: usList.getDataRows()) {
            if (row.getUserName().equals(username)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Is the user a member of the project?
     *
     * @param username to check.
     * @param projectId
     * @return true, if the user is a member of the project, false otherwise.
     * @throws RemoteException
     */
    public boolean isUserMemberOfProject(String username, String projectId) 
        throws RemoteException {
        this.checkValidSessionId();
        ProjectMemberSoapList pmList = this.icns.
            getProjectMemberList(this.sessionId, projectId);
        for (ProjectMemberSoapRow row: pmList.getDataRows()) {
            if (row.getUserName().equals(username)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Is the user a member of the group?
     *
     * @param username to check.
     * @param group
     * @return true, if the user is a member of the group, false otherwise.
     * @throws RemoteException
     */
    public boolean isUserMemberOfGroup (String username, String group) 
        throws RemoteException {
        this.checkValidSessionId();
        GroupSoapList gList = this.icns.getUserGroupList(this.sessionId, 
                                                         username);
        for (GroupSoapRow row: gList.getDataRows()) {
            if (row.getFullName().equals(group)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a list of the groups the user belongs to.  This will only work
     * if logged in as the user in question, or if the logged in user has
     * superuser permissions.
     *
     * @param username
     * @return collection of group names.
     * @throws RemoteException
     */
    public Collection<String> getUserGroups(String username) 
        throws RemoteException {
        this.checkValidSessionId();
        Collection<String> groups = new ArrayList<String>();
        GroupSoapList gList = this.icns.getUserGroupList(this.sessionId, 
                                                         username);
        for (GroupSoapRow row: gList.getDataRows()) {
            groups.add(row.getFullName());
        }
        return groups;
    }

    /**
     * Is the user a project admin?
     * @param username to check.
     * @param projectId
     * @return true if the user is an admin of this project, false otherwise.
     * @throws RemoteException
     */
    public boolean isUserProjectAdmin(String username, String projectId) 
        throws RemoteException {
        this.checkValidSessionId();
        UserSoapList usList = this.icns.listProjectAdmins(this.sessionId, 
                                                          projectId);
        for (UserSoapRow row: usList.getDataRows()) {
            if (row.getUserName().equals(username)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param username
     * @return true if the user is a super user.  False if not, or if the user
     *         is not found.
     * @throws RemoteException
     */
    public boolean isUserSuper(String username) throws RemoteException {
        this.checkValidSessionId();
        UserSoapList usList = this.icns.findUsers(this.sessionId, username);
        for (UserSoapRow row: usList.getDataRows()) {
            if (row.getUserName().equals(username)) {
                return row.getSuperUser();
            }
        }
        return false;
    }

    /**
     * Get the usernames of all users.
     *
     * @param projectId the project id
     * @return collection of usernames
     * @throws RemoteException
     */
    public Collection<String> getUsers(String projectId) 
        throws RemoteException {
        this.checkValidSessionId();
        Collection<String> users = new ArrayList<String>();
        ProjectMemberSoapList pmList = this.icns.
            getProjectMemberList(this.sessionId, projectId);
        for (ProjectMemberSoapRow row: pmList.getDataRows()) {
            users.add(row.getUserName());
        }
        return users;
    }

    /**
     * Get the usernames of all project admins.
     *
     * @param projectId the project id
     * @return collection of admin usernames
     * @throws RemoteException
     */
    public Collection<String> getAdmins(String projectId) 
        throws RemoteException {
        this.checkValidSessionId();
        Collection<String> admins = new ArrayList<String>();
        UserSoapList usList = this.icns.listProjectAdmins(this.sessionId, 
                                                          projectId);
        for (UserSoapRow row: usList.getDataRows()) {
            admins.add(row.getUserName());
        }
        return admins;
    }

    /**
     * Get the list of all Groups on the system.
     * Can only be called by SuperUsers.
     *
     * @return a Map of all group name/ids.
     * @throws RemoteException 
     */
    public Map<String, String> getGroups() throws RemoteException {
        this.checkValidSessionId();
        Map<String, String> nameId = new HashMap<String, String>();
        Group2SoapList gsList = this.icns.getGroupList2(this.sessionId, null);
        for (Group2SoapRow row: gsList.getDataRows()) {
            nameId.put(row.getFullName(), row.getId());
        }
        return nameId;
    } 

    /**
     * Given a list of group names, return a list of all unique users that 
     * are members of any of the groups.  If the group name is not found,
     * no error is thrown.
     * Only works for SuperUsers.
     *
     * @param groupNames collection of group names.
     * @return a collection of user names.
     * @throws RemoteException
     */
    public Collection<String> getUsersInGroups(Collection<String> groupNames) 
        throws RemoteException {
        this.checkValidSessionId();
        Map<String, String> groupNameIds = this.getGroups();
        Collection<String> users = new HashSet<String>();
        for (String groupName: groupNames) {
            String id = groupNameIds.get(groupName);
            if (id != null) {
                users.addAll(this.getGroupUsers(id));
            }
        }
        return users;
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
