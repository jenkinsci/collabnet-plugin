package hudson.plugins.collabnet.auth;

import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.security.ACL;
import hudson.security.AuthorizationStrategy;
import hudson.security.SparseACL;

import java.util.List;

public class MockCNAuthorizationStrategy extends CNAuthorizationStrategy {

    public MockCNAuthorizationStrategy(List<String> readUsers, List<String> readGroups,
            List<String> adminUsers, List<String> adminGroups, int permCacheTimeoutMin) {
        super(readUsers, readGroups, adminUsers, adminGroups, permCacheTimeoutMin);
    }

    public MockCNAuthorizationStrategy(String readUsers, String readGroups, String adminUsers,
            String adminGroups, int permCacheTimeoutMin) {
        super(readUsers, readGroups, adminUsers, adminGroups, permCacheTimeoutMin);
    }

    /**
     * Allow resubmission of the system config without logging in first.
     * @return
     */
    @Override
    public ACL getRootACL() {
        SparseACL acl = new SparseACL(null);
        acl.add(ACL.ANONYMOUS, Hudson.ADMINISTER, true);
        return acl;
    }

    @Override
    public Descriptor<AuthorizationStrategy> getDescriptor() {
        return Hudson.getInstance().getDescriptorOrDie(CNAuthorizationStrategy.class);
    }

}
