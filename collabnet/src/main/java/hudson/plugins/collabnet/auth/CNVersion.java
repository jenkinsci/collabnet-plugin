package hudson.plugins.collabnet.auth;

/**
 * Class to represent and compare CollabNet version numbers.
 */
public class CNVersion implements Comparable<CNVersion> {
    private int major = 0;
    private int minor = 0;
    private int servicePack = 0;
    private int hotfix = 0;
    
    public CNVersion(String versionString) {
        String[] parts = versionString.split("\\.");
        if (parts.length > 0) {
            this.major = Integer.parseInt(parts[0]);
        }
        if (parts.length > 1) {
            this.minor = Integer.parseInt(parts[1]);
        }
        if (parts.length > 2) {
            this.servicePack = Integer.parseInt(parts[2]);
        }
        if (parts.length > 3) {
            this.hotfix = Integer.parseInt(parts[3]);
        }
    }
    
    /**
     * @return negative int, zero, or positive int if this
     *         is less than, equal to, or greather than the otherVersion.
     *         By "less than", we mean an earlier version.
     *         "greater than" would be a later version.
     * @throws NullPointerException if otherVersion is null.
     */
    public int compareTo(CNVersion otherVersion) {
        final int LESS_THAN = -1;
        final int EQUAL = 0;
        final int GREATER_THAN = 1;
        
        int[] thisArray = {this.major, this.minor, this.servicePack, 
                           this.hotfix};
        int[] otherArray = {otherVersion.major, otherVersion.minor, 
                            otherVersion.servicePack, otherVersion.hotfix};
        for (int i = 0; i < thisArray.length; i++) {
            if (thisArray[i] < otherArray[i]) {
                return LESS_THAN;
            } else if (thisArray[i] > otherArray[i]) {
                return GREATER_THAN;
            } 
        }
        return EQUAL; 
    }
    
    /**
     * Override the toString method to display the parts of the version
     * as a String.
     */
    @Override
    public String toString() {
        return this.major + "." + this.minor + "." + this.servicePack + "." 
            + this.hotfix;
    }
    
    /**
     * Override the equals method to ensure that any CNVersion with
     * the same version numbers is equal.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        
        if (!(obj instanceof CNVersion)) {
            return false;
        }
        
        CNVersion other = (CNVersion) obj;
        if (this.major == other.major &&
            this.minor == other.minor &&
            this.servicePack == other.servicePack &&
            this.hotfix == other.hotfix) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Override hashCode since we're are overriding equals.
     */
    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}
