package hudson.plugins.collabnet;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.scm.ChangeLogSet.Entry;

import java.util.Iterator;

/**
 * @author Kohsuke Kawaguchi
 */
public class CNChangeLogAnnotatorTest extends CNHudsonTestCase {
    public void test1() throws Exception {
        setGlobalConnectionFactory();

        FakeChangeLogSCM fake = new FakeChangeLogSCM();
        fake.commit("a").says("[artf12345] fixed a bug");
        fake.commit("b").says("xartf12345 shouldn't match");
        FreeStyleProject p = createFreeStyleProject();
        p.setScm(fake);
        FreeStyleBuild b = buildAndAssertSuccess(p);

        Iterator<? extends Entry> itr = b.getChangeSet().iterator();
        Entry e = itr.next();
        System.out.println(e.getMsgAnnotated());
        assertTrue(e.getMsgAnnotated(), e.getMsgAnnotated().contains("sf/go/artf12345"));

        e = itr.next();
        System.out.println(e.getMsgAnnotated());
        assertFalse(e.getMsgAnnotated().contains("sf/go/artf12345"));
    }
}
