package hudson.plugins.collabnet.orchestrate;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import jenkins.model.TransientActionFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;

/**
 * Created by sureshk on 07/01/16.
 */
@Extension
public class TraceabilityActionFactory extends TransientActionFactory<AbstractBuild> {

    @Override
    public Class<AbstractBuild> type(){
        return AbstractBuild.class;
    }

    @Nonnull
    @Override
    public Collection<? extends Action> createFor(@Nonnull AbstractBuild abstractBuild) {
        final TraceabilityAction newAction = new TraceabilityAction(abstractBuild);
        return Collections.singleton(newAction);
    }
}
