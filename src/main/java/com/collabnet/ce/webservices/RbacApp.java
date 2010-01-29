package com.collabnet.ce.webservices;

import com.collabnet.ce.soap50.webservices.ClientSoapStubFactory;

import com.collabnet.ce.soap50.webservices.cemain.UserSoapList;
import com.collabnet.ce.soap50.webservices.rbac.IRbacAppSoap;
import com.collabnet.ce.soap50.webservices.rbac.RoleSoapList;
import com.collabnet.ce.soap50.webservices.rbac.RoleSoapRow;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to hold the role-related methods.
 * Wraps a collabNetApp.
 */
public class RbacApp extends AbstractSoapApp {
    private IRbacAppSoap iras;

    /**
     * Constructs a new RbacApp.
     * 
     * @param collabNetApp a valid (logged-in) instance.
     */
    public RbacApp(CollabNetApp collabNetApp) {
        super(collabNetApp);
        this.iras = this.getIRbacAppSoap();
    }

    /**
     * @return an instance of the Client Soap stub for RbacApp.wsdl.
     */
    private IRbacAppSoap getIRbacAppSoap() {
        String soapURL = this.getServerUrl() + CollabNetApp.SOAP_SERVICE +
            "RbacApp?wsdl";
        return (IRbacAppSoap) ClientSoapStubFactory.
            getSoapStub(IRbacAppSoap.class, soapURL);
    }

    /**
     * @param projectId the project id
     * @return map (name->id) of roles associated with the given project
     * @throws RemoteException
     */
    public Map<String, String> getRoles(String projectId)
        throws RemoteException {
        this.checkValidSessionId();
        RoleSoapList rsList = this.iras.getRoleList(this.getSessionId(), 
                                                    projectId);
        Map<String, String> roleNameToIdMap = new HashMap<String, String>();
        for (RoleSoapRow row: rsList.getDataRows()) {
            roleNameToIdMap.put(row.getTitle(), row.getId());
        }
        return roleNameToIdMap;
    }

    /**
     * Add a set of roles with descriptions to the project.  Only adds
     * roles if they are not already present.
     *
     * @param projectId
     * @param roles to add (only added if missing).
     * @param descriptions of roles to add (the nth description refers to 
     *                     the nth role)
     * @return true if some roles were added, false if none were.
     * @throws RemoteException
     */
    public boolean addRoles(String projectId, String[] roles, 
                            String[] descriptions) throws RemoteException {
        this.checkValidSessionId();
        boolean rolesAdded = false;
        if (roles.length != descriptions.length) {
            throw new IllegalArgumentException("RbacApp: addRoles: the " +
                                               "number of role names and " +
                                               "descriptions  are not equal.");
        }
        Map<String, String> existingRoles = this.getRoles(projectId);
        for (int i = 0; i < roles.length; i++) {
            if (!existingRoles.containsKey(roles[i])) {
                this.addRole(projectId, roles[i], descriptions[i]);
                rolesAdded = true;
            }
        }
        return rolesAdded;
    }

    /**
     * Add a single role to the project.  Does not pre-check for existence.
     *
     * @param projectId
     * @param role
     * @param description
     * @throws RemoteException
     */
    public void addRole(String projectId, String role, String description) 
        throws RemoteException {
        this.checkValidSessionId();
        this.iras.createRole(this.getSessionId(), projectId, role, 
                             description);
    }

    /**
     * Grant the given role to the user.
     * 
     * @param projectId 
     * @param roleName
     * @param username
     * @throws RemoteException
     */
    public void grantRole(String projectId, String roleName, String username) 
        throws RemoteException {
        this.checkValidSessionId();
        String roleId = this.findRoleId(projectId, roleName);
        this.iras.addUser(this.getSessionId(), roleId, username);
    }

    /**
     * Grant the given role to the user.
     *
     * @param roleId
     * @param username
     * @throws RemoteException
     */
    public void grantRole(String roleId, String username)
        throws RemoteException {
        this.checkValidSessionId();
        this.iras.addUser(this.getSessionId(), roleId, username);
    }

    /**
     * Find the roleId for the roleName.
     *
     * @param projectId
     * @param roleName
     * @return roleId
     * @throws RemoteException
     */
    private String findRoleId(String projectId, String roleName) 
        throws RemoteException {
        this.checkValidSessionId();
        RoleSoapList rsList = this.iras.getRoleList(this.getSessionId(), 
                                                    projectId);
        for (RoleSoapRow row: rsList.getDataRows()) {
            if (row.getTitle().equals(roleName)) {
                return row.getId();
            }
        }
        return null;
    }

    /**
     * Get a list of role the user has in the given project.
     *
     * @param projectId
     * @param username
     * @throws RemoteException
     */
    public Collection<String> getUserRoles(String projectId, String username) 
        throws RemoteException {
        this.checkValidSessionId();
        Collection<String> roles = new ArrayList<String>();
        RoleSoapList rsList = this.iras.getUserRoleList(this.getSessionId(), 
                                                         projectId, username);
        for (int i = 0; i < rsList.getDataRows().length; i++) {
            roles.add(rsList.getDataRows()[i].getTitle());
        }
        return roles;
    }

    /**
     * Get a list of users that are members of a given role
     *
     * @param roleId the id of the role to get
     * @throws RemoteException
     */
    public Collection<String> getRoleMembers(String roleId) throws RemoteException {
        this.checkValidSessionId();
        Collection<String> roles = new ArrayList<String>();
        UserSoapList userList = this.iras.getRoleMemberList(this.getSessionId(), roleId);
        for (int i = 0; i < userList.getDataRows().length; i++) {
            roles.add(userList.getDataRows()[i].getUserName());
        }
        return roles;
    }
}
