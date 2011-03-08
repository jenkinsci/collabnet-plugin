package hudson.plugins.collabnet;

import hudson.Extension;
import hudson.MarkupText;
import hudson.MarkupText.SubText;
import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogAnnotator;
import hudson.scm.ChangeLogSet.Entry;

import java.util.regex.Pattern;

import static hudson.plugins.collabnet.AbstractTeamForgeNotifier.getTeamForgeShareDescriptor;

/**
 * Looks for object IDs in the commit messages and turn them into hyperlinks.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class CNChangeLogAnnotator extends ChangeLogAnnotator {
    @Override
    public void annotate(AbstractBuild<?, ?> build, Entry change, MarkupText text) {
        String base = getCollabNetUrl(build);
        if (base==null) return;

        for (SubText t : text.findTokens(OBJECT_IDS))
            t.href(base+"/sf/go/"+t.getText());
    }

    /**
     * Obtains the TeamForge URL for the build.
     * Just in case a single Jenkins deployment serves multiple CTF instances,
     * this method first tries to obtain the URL from the job setting. Failing that,
     * it resorts to the global configuration.
     *
     * @return null
     *      If no configured CTF URL was found.
     */
    private String getCollabNetUrl(AbstractBuild<?,?> build) {
        AbstractTeamForgeNotifier n = build.getParent().getPublishersList().get(AbstractTeamForgeNotifier.class);
        if (n!=null)    return n.getCollabNetUrl();
        return getTeamForgeShareDescriptor().getCollabNetUrl();
    }

    private static final Pattern OBJECT_IDS = Pattern.compile("\\b(cmmt|doc|frs|news|post|report|task|forum|pkg|rel|docr|docf|topc|tracker|user|proj|reps|taskgrp|wiki|page|srch|plan|artf)[0-9]{4,}(#\\d+)?\\b");
}
