package hudson.plugins.collabnet.tracker;

import hudson.plugins.collabnet.CNHudsonTestCase;

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
                "aaa", "bbb", "ccc", "ddd", Priority.P3, true, true, true, "eee"),FIELDS);
        // note that because filePatterns is minimum 1, new FilePattern[0] test would fail

        roundtripAndAssertIntegrity(new CNTracker(
                null,"abc","def","ghi","jkl",Priority.P5,false,false,false,"mno"),FIELDS);
    }

    /**
     * Makes sure that help link exists on all three options.
     */
    public void testHelpLink() throws Exception {
        assertHelpExists(CNTracker.class,FIELDS+",-connectionFactory");
    }

    private static final String FIELDS = "connectionFactory,project,tracker,title,assignUser,priority,attachLog,alwaysUpdate,closeOnSuccess,release";
}
