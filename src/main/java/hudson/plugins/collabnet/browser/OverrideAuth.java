package hudson.plugins.collabnet.browser;

import hudson.plugins.collabnet.util.CNHudsonUtil;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Class to represent the override_auth data.  
 * Needed for the DataBoundConstructor to work properly.
 */
public class OverrideAuth {
    public String collabneturl;
    public String username;
    public Secret password;
    
    @DataBoundConstructor
    public OverrideAuth(String collabneturl, String username, 
                        String password) {
        this.collabneturl = CNHudsonUtil.sanitizeCollabNetUrl(collabneturl);
        this.username = username;
        this.password = Secret.fromString(password);
    }
}
