package hudson.plugins.collabnet.auth;

import hudson.security.Permission;

import java.util.Collection;
import java.util.ArrayList;

/**
 * This class stores information about each CollabNet Role (name, description,
 * associated hudson permissions, etc.)
 */
public class CollabNetRole {
    private String name;
    private String description;
    private Collection<Permission> permissions;

    public CollabNetRole(String name, String description, 
                         Collection<Permission> permissions) {
        this.name = name;
        this.description = description;
        if (permissions != null) {
            this.permissions = new ArrayList<Permission>(permissions);
        } else {
            this.permissions = new ArrayList<Permission>();
        }
    }

    public CollabNetRole(String name, String description) {
        this.name = name;
        this.description = description;
        this.permissions = new ArrayList<Permission>();
    }

    public CollabNetRole(String name) {
        this.name = name;
        this.permissions = new ArrayList<Permission>();
    }

    /**
     * @return the name of the role.
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the description of the role.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * @return the Hudson Permissions granted by this role.
     */
    public Collection<Permission> getPermissions() {
        return this.permissions;
    }

    /**
     * @param permission to add.
     */
    public void addPermission(Permission permission) {
        this.permissions.add(permission);
    }

    /**
     * @return true if the role has this permission.
     */
    public boolean hasPermission(Permission permission) {
        if (this.permissions.contains(permission)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Any two CollabNetRoles with the same name are equal.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (obj.getClass() != this.getClass())) {
            return false;
        }
        CollabNetRole other = (CollabNetRole) obj;
        return (other.getName().equals(this.getName()));
    }

    /**
     * Override hashcode so it remains consistent with equals.
     */
    public int hashCode() {
        if (this.name == null) {
            return 0;
        } else {
            return this.name.hashCode();
        }
    }

    /**
     * Override for prettier logging.
     */
    public String toString() {
        String str = "CollabNetRole: {name: " + this.getName() + 
            ", description: " + this.getDescription() + ", permissions: " 
            + this.getPermissions();
        return str;
    }
}
