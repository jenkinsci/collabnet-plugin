package hudson.plugins.collabnet.documentuploader;

import hudson.model.FreeStyleProject;
import hudson.plugins.collabnet.CNHudsonTestCase;
import hudson.plugins.collabnet.ConnectionFactory;
import hudson.plugins.collabnet.share.TeamForgeShare;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * Test document upload facility.
 *
 * Unlike {@link DocUploadTest}, this version doesn't have any setUp/tearDown.
 */
public class CNDocumentUploaderTest extends CNHudsonTestCase {
    public void testConfigRoundtrip() throws Exception {
        setGlobalConnectionFactory();

        roundtripAndAssertIntegrity(new CNDocumentUploader(
                createConnectionFactory(),
                "project", "path", "description", new FilePattern[]{new FilePattern("ddd")}, true),FIELDS);
        // note that because filePatterns is minimum 1, new FilePattern[0] test would fail

        roundtripAndAssertIntegrity(new CNDocumentUploader(
                null, "project", "path", "description",
                new FilePattern[]{new FilePattern("xxx"),new FilePattern("yyy")}, false),FIELDS);
    }

    /**
     * Makes sure that help link exists on all three options.
     */
    public void testHelpLink() throws Exception {
        assertHelpExists(CNDocumentUploader.class,
                // FIELDS-connectionFactory
                "project,uploadPath,description,filePatterns,includeBuildLog");
    }

    private static final String FIELDS = "connectionFactory,project,uploadPath,description,filePatterns,includeBuildLog";
}
