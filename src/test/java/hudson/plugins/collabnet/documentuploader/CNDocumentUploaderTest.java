package hudson.plugins.collabnet.documentuploader;

import hudson.model.FreeStyleProject;
import hudson.plugins.collabnet.ConnectionFactory;
import hudson.plugins.collabnet.share.TeamForgeShare;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * Test document upload facility.
 *
 * Unlike {@link DocUploadTest}, this version doesn't have any setUp/tearDown.
 */
public class CNDocumentUploaderTest extends HudsonTestCase {
    public void testConfigRoundtrip() throws Exception {
        // setting a global value would enable job configuration to choose the override or delegate to the default.
        TeamForgeShare.getTeamForgeShareDescriptor().setConnectionFactory(
            new ConnectionFactory("http://www.google.com/", "abc", "def")
        );

        roundtrip(new CNDocumentUploader(
                new ConnectionFactory("http://www.google.com/", "abc", "def"),
                "project", "path", "description", new FilePattern[]{new FilePattern("ddd")}, true));
        // note that because filePatterns is minimum 1, new FilePattern[0] test would fail

        roundtrip(new CNDocumentUploader(
                null, "project", "path", "description",
                new FilePattern[]{new FilePattern("xxx"),new FilePattern("yyy")}, false));
    }

    private void roundtrip(CNDocumentUploader before) throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getPublishersList().add(before);
        submit(createWebClient().getPage(p,"configure").getFormByName("config"));
        CNDocumentUploader after = p.getPublishersList().get(CNDocumentUploader.class);
        assertEqualBeans(before,after,FIELDS);
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
