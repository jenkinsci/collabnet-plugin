package hudson.plugins.collabnet.tracker;

import hudson.plugins.collabnet.CNHudsonTestCase;
import hudson.plugins.collabnet.ConnectionFactory;
import hudson.plugins.collabnet.filerelease.CNFileRelease;
import hudson.plugins.collabnet.share.TeamForgeShare;

/**
 * Tests tracker integration.
 *
 * Unlike {@link TrackerTest}, this version doesn't have any setUp/tearDown.
 */
public class CNTrackerTest extends CNHudsonTestCase {
    public void testConfigRoundtrip() throws Exception {
        setGlobalConnectionFactory();

        roundtripAndAssertIntegrity(new CNTracker(
                createConnectionFactory(),
                "aaa", "bbb", "ccc", "ddd", "p", true, true, true, "eee"),FIELDS);
        // note that because filePatterns is minimum 1, new FilePattern[0] test would fail

        roundtripAndAssertIntegrity(new CNTracker(
                null,
                "abc","def","ghi","jkl","P",false,false,false,"mno"),FIELDS);
    }

    /**
     * Makes sure that help link exists on all three options.
     */
    public void testHelpLink() throws Exception {
        assertHelpExists(CNFileRelease.class,
                // FIELDS-connectionFactory
                "project,pkg,release,filePatterns,overwrite");
    }

    private static final String FIELDS = "connectionFactory,project,pkg,release,filePatterns,overwrite";
}
