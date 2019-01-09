package jenkins.plugins.collabnet.security;

import java.rmi.RemoteException;

import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationManager;
import org.acegisecurity.BadCredentialsException;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;

import com.collabnet.ce.webservices.CollabNetApp;

import hudson.plugins.collabnet.auth.CNAuthentication;

public class CnAuthenticationManager implements AuthenticationManager {

    private String collabNetUrl;

    public CnAuthenticationManager(String collabNetUrl) {
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
        if (authentication instanceof CNAuthentication) {
            return authentication;
        }
        else if (authentication instanceof UsernamePasswordAuthenticationToken) {
            String username = authentication.getName();
            String password = (String) authentication.getCredentials();
            try {
                CollabNetApp cna = new CollabNetApp(this.getCollabNetUrl(), username, password);
                return new CNAuthentication(authentication.getName(), cna);
            } catch (RemoteException re) {
                throw new BadCredentialsException("Failed to log into " + 
                        getCollabNetUrl() + ": " + re.getMessage());
            }
        }
        throw new BadCredentialsException(
                "Unexpected authentication type: " + authentication);
    }
}
