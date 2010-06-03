package hudson.plugins.collabnet.filerelease;

import hudson.plugins.collabnet.CNHudsonTestCase;
import hudson.plugins.collabnet.documentuploader.FilePattern;

/**
 * Test document upload facility.
 *
 * Unlike {@link FileReleaseTest}, this version doesn't have any setUp/tearDown.
 */
public class CNFileReleaseTest extends CNHudsonTestCase {
    public void testConfigRoundtrip() throws Exception {
        setGlobalConnectionFactory();

        roundtripAndAssertIntegrity(new CNFileRelease(
                createConnectionFactory(),
                "aaa", "bbb", "ccc", true, new FilePattern[]{new FilePattern("ddd")}),FIELDS);
        // note that because filePatterns is minimum 1, new FilePattern[0] test would fail

        roundtripAndAssertIntegrity(new CNFileRelease(
                null,
                "xxx", "yyy", "zzz", false, new FilePattern[]{new FilePattern("111"),new FilePattern("222")}),FIELDS);
    }

    /**
     * Makes sure that help link exists on all three options.
     */
    public void testHelpLink() throws Exception {
        assertHelpExists(CNFileRelease.class,FIELDS+",-connectionFactory");
    }

    private static final String FIELDS = "connectionFactory,project,pkg,release,filePatterns,overwrite";
}
