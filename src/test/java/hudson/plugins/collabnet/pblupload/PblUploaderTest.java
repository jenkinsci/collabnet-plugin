package hudson.plugins.collabnet.pblupload;

import hudson.plugins.collabnet.CNHudsonTestCase;
import hudson.plugins.collabnet.documentuploader.FilePattern;
import hudson.util.Secret;

public class PblUploaderTest extends CNHudsonTestCase {
    public void testConfigRoundtrip() throws Exception {
        if (!verifyOnline())    return;

        setGlobalConnectionFactory();

        roundtripAndAssertIntegrity(new PblUploader(
                "aaa","bbb",Secret.fromString("ccc"),"ddd",true,new FilePattern[]{new FilePattern("eee")},"fff",true,true,"ggg","hhh","iii"),FIELDS);
        // note that because filePatterns is minimum 1, new FilePattern[0] test would fail

        roundtripAndAssertIntegrity(new PblUploader(
                "aaa","bbb",Secret.fromString("ccc"),"ddd",false,new FilePattern[]{new FilePattern("eee"),new FilePattern("eee2")},
                "fff",false,false,"ggg","hhh","iii"),FIELDS);
    }

    /**
     * Makes sure that help link exists on all three options.
     */
    public void testHelpLink() throws Exception {
        assertHelpExists(PblUploader.class,FIELDS+",-preserveLocal");
    }

    private static final String FIELDS = "hostUrl,user,key,project,pubOrPriv,filePatterns,path,preserveLocal,force,comment,description,removePrefixRegex";
}
