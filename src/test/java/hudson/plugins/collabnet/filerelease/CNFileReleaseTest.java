package hudson.plugins.collabnet.filerelease;

import hudson.model.FreeStyleProject;
import hudson.plugins.collabnet.ConnectionFactory;
import hudson.plugins.collabnet.documentuploader.CNDocumentUploader;
import hudson.plugins.collabnet.documentuploader.DocUploadTest;
import hudson.plugins.collabnet.documentuploader.FilePattern;
import hudson.plugins.collabnet.share.TeamForgeShare;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * Test document upload facility.
 *
 * Unlike {@link FileReleaseTest}, this version doesn't have any setUp/tearDown.
 */
public class CNFileReleaseTest extends HudsonTestCase {
    public void testConfigRoundtrip() throws Exception {
        // setting a global value would enable job configuration to choose the override or delegate to the default.
        TeamForgeShare.getTeamForgeShareDescriptor().setConnectionFactory(
            new ConnectionFactory("http://www.google.com/", "abc", "def")
        );

        roundtrip(new CNFileRelease(
                new ConnectionFactory("http://www.google.com/", "abc", "def"),
                "aaa", "bbb", "ccc", true, new FilePattern[]{new FilePattern("ddd")}));
        // note that because filePatterns is minimum 1, new FilePattern[0] test would fail

        roundtrip(new CNFileRelease(
                null,
                "xxx", "yyy", "zzz", false, new FilePattern[]{new FilePattern("111"),new FilePattern("222")}));
    }

    private void roundtrip(CNFileRelease before) throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getPublishersList().add(before);
        // interactiveBreak();
        submit(createWebClient().getPage(p,"configure").getFormByName("config"));
        CNFileRelease after = p.getPublishersList().get(CNFileRelease.class);
        assertEqualBeans(before,after,FIELDS);
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
