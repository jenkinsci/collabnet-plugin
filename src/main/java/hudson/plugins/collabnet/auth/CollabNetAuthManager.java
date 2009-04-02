package hudson.plugins.collabnet.auth;

import java.rmi.RemoteException;

import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationManager;
import org.acegisecurity.BadCredentialsException;

import com.collabnet.ce.webservices.CollabNetApp;

public class CollabNetAuthManager implements AuthenticationManager {
    private String collabNetUrl;

    public CollabNetAuthManager(String collabNetUrl) {
        this.collabNetUrl = collabNetUrl;
    }

    public String getCollabNetUrl() {
        return this.collabNetUrl;
    }

    /**
     * @param authentication request object
     * @return fully authenticated object, including credentials
     */
    public Authentication authenticate(Authentication authentication) 
        throws BadCredentialsException {
        String username = authentication.getName();
        String password = (String) authentication.getCredentials();
        CollabNetApp cna = null;
        try {
            cna = new CollabNetApp(this.getCollabNetUrl(), username, password);
        } catch (RemoteException re) {
            throw new BadCredentialsException("Failed to log into " + 
                                              this.getCollabNetUrl() + ": " + 
                                              re.getMessage());
        }
        return new CNAuthentication(authentication.getName(), cna);
    }
}
