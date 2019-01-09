package jenkins.plugins.collabnet.security;

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;

import hudson.security.GroupDetails;

public class CnGroupDetails extends GroupDetails {

    private final String name;
    private Set<String> members;
    
    public CnGroupDetails(@Nonnull String name, @Nonnull Set<String> members) {
        this.name = name;
        this.members = members;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Set<String> getMembers() {
        return Collections.unmodifiableSet(this.members);
    }

    
}
