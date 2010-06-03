package hudson.plugins.collabnet.auth;

import com.collabnet.ce.webservices.CollabNetApp;
import hudson.model.Hudson;
import org.acegisecurity.Authentication;

/**
 * Provides access to the {@link CollabNetApp} associated with the current {@link Authentication}
 * that the calling thread carries.
 */
public class CNConnection {
    /**
     * Gets an instance with the current authentication, or null if the auth
     * is the wrong type.
     */
    public static CollabNetApp getInstance() {
        return CNConnection.getInstance(Hudson.getAuthentication());
    }

    /**
     * Wraps the private constructor.  Will return null if the Authentication
     * is the wrong type (i.e. not CNAuthentication).
     */
    public static CollabNetApp getInstance(Authentication a) {
        if (a instanceof CNAuthentication) {
            return ((CNAuthentication) a).getCredentials();
        } else {
            return null;
        }
    }
}
