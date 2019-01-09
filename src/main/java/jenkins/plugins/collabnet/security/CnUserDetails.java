package jenkins.plugins.collabnet.security;

import javax.annotation.Nonnull;

import org.acegisecurity.Authentication;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.userdetails.User;
import org.acegisecurity.userdetails.UserDetails;

import hudson.plugins.collabnet.auth.CNAuthentication;

public class CnUserDetails extends User implements UserDetails {

    private static final long serialVersionUID = 1L;

    private boolean hasGrantedAuthorities;

    private final CNAuthentication authentication;
    
    public CnUserDetails(String username, GrantedAuthority[] authorities)
            throws IllegalArgumentException {
        super(username, "", true, true, true, true, authorities);
        this.hasGrantedAuthorities = true;
        this.authentication = null;
    }

    public CnUserDetails(@Nonnull CNAuthentication authentication)
            throws IllegalArgumentException {
        super(authentication.getPrincipal(), "", true, true, true, true, authentication.getAuthorities());
        this.hasGrantedAuthorities = false;
        this.authentication = authentication;
    }

    @Override
    public GrantedAuthority[] getAuthorities() {
        if (!hasGrantedAuthorities) {
            setAuthorities(this.authentication.getAuthorities());
        }
        return super.getAuthorities();
    }

    public Authentication getAuthentication() {
        return this.authentication;
    }
}
