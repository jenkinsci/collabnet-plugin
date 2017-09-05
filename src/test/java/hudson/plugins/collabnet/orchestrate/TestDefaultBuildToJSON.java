/*
 * Copyright 2013 CollabNet, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hudson.plugins.collabnet.orchestrate;


import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.User;
import hudson.plugins.collabnet.orchestrate.BuildToJSON;
import hudson.plugins.collabnet.orchestrate.DefaultBuildToJSON;
import hudson.plugins.collabnet.util.MockUtils;
import hudson.scm.ChangeLogSet;
import hudson.tasks.test.AbstractTestResultAction;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.easymock.EasyMock;
import org.easymock.IMockBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertFalse;
import static org.easymock.EasyMock.expect;

/**
 * Unit tests for the DefaultBuildToOrchestrateAPI
 */
public class TestDefaultBuildToJSON {
    /** The association key to test with. */
    private String associationKey;

    /** The server URL to connect to. */
    private URI serverUri;

    /** Helper for mocks. */
    private MockUtils mocks;

    private AbstractBuild<?, ?> build;

    /** Mock builder for mocking out individual functions on the converter **/
    private IMockBuilder<DefaultBuildToJSON> converterBuilder;

    private JSONObject repositoryInfo;

    /** Converter to test **/
    private BuildToJSON converter;

    /** Sets up the tests. */
    @Before
    public void setUp() throws URISyntaxException {
        serverUri = new URI("http://orchestrate.test");
        associationKey = "q20394SAfasrd";
        mocks = new MockUtils();
        build = mocks.createMock("build", AbstractBuild.class);
        repositoryInfo = new JSONObject()
                .accumulate("repository_type", "foo")
                .accumulate("repository_url", "bar://omg.wtf/bbq");


        converterBuilder = EasyMock.createMockBuilder(DefaultBuildToJSON.class);
        converter = new DefaultBuildToJSON();
    }

    /**
     * Do nothing subclass of ChangeLogSet.Entry for testing parts of the code.
     */
    class FakeChangeLogEntry extends ChangeLogSet.Entry {
        String revisionNumber;

        public FakeChangeLogEntry(String revisionNumber) {
            this.revisionNumber = revisionNumber;
        }

        public String getCommitId() {
            return revisionNumber;
        }

        public String getMsg() {
            return null;
        }

        public User getAuthor() {
            return null;
        }

        public Collection<String> getAffectedPaths() {
            return null;
        }

		public String getUser() {
			// TODO Auto-generated method stub
			return null;
		}
    }

    /**
     * Stand-in for a real ChangeLog
     */
    class FakeChangeLogSet extends ChangeLogSet<FakeChangeLogEntry> {
        FakeChangeLogEntry[] entries;

        public FakeChangeLogSet(AbstractBuild build, FakeChangeLogEntry... entries) {
        	super((Run<?,?>)build,null);
            this.entries = entries;
        }

        /**
         * Returns true if there's no change.
         */
        @Override
        public boolean isEmptySet() {
            return (entries == null) || entries.length == 0;
        }

        /**
         * Returns an iterator over a set of elements of type T.
         *
         * @return an Iterator.
         */
        @Override
        public Iterator<FakeChangeLogEntry> iterator() {
            return Arrays.asList(entries).iterator();
        }

		public Collection<FakeChangeLogEntry> getLogs() {
			// TODO Auto-generated method stub
			return null;
		}
    }

    @Test
    public void successIsSuccessful() {
        // Setup
        expect(build.getResult()).andReturn(Result.SUCCESS);

        mocks.replayAll();

        // execute
        JSONObject status = converter.getStatus(build);

        //verify

        mocks.verifyAll();

        assertEquals("SUCCESS", status.getString("type"));
        assertEquals("Successful", status.getString("name"));
    }

    @Test
    public void unstableIsUnstable() {
        // Setup
        expect(build.getResult()).andReturn(Result.UNSTABLE);

        mocks.replayAll();

        // execute
        JSONObject status = converter.getStatus(build);

        //verify

        mocks.verifyAll();

        assertEquals("UNSTABLE", status.getString("type"));
        assertEquals("Unstable", status.getString("name"));
    }

    @Test
    public void notBuiltIsFailure() {
        // Setup
        expect(build.getResult()).andReturn(Result.NOT_BUILT);

        mocks.replayAll();

        // execute
        JSONObject status = converter.getStatus(build);

        //verify

        mocks.verifyAll();

        assertEquals("FAILURE", status.getString("type"));
        assertEquals("Failed", status.getString("name"));
    }

    @Test
    public void failureIsFailure() {
        // Setup
        expect(build.getResult()).andReturn(Result.FAILURE);

        mocks.replayAll();

        // execute
        JSONObject status = converter.getStatus(build);

        //verify

        mocks.verifyAll();

        assertEquals("FAILURE", status.getString("type"));
        assertEquals("Failed", status.getString("name"));
    }

    @Test
    public void abortedIsAborted() {
        // Setup
        expect(build.getResult()).andReturn(Result.ABORTED);

        mocks.replayAll();

        // execute
        JSONObject answer = converter.getStatus(build);

        //verify

        mocks.verifyAll();

        assertEquals("ABORTED", answer.getString("type"));
        assertEquals("Aborted", answer.getString("name"));
    }

    @Test
    public void noTestsReturnsNull() {
        // Setup
        expect(build.getAction(AbstractTestResultAction.class)).andReturn(null);

        mocks.replayAll();

        // Execute
        JSONObject answer = converter.getTestResults(build);

        // Verify
        mocks.verifyAll();
        assertNull("No answer expected", answer);
    }

    @Test
    public void resultsMathIsCorrect() throws URISyntaxException {
        // Setup
        AbstractTestResultAction results = mocks.createMock("results", AbstractTestResultAction.class);
        converter = mocks.createMock("converter", converterBuilder.addMockedMethod("getBuildURI", Run.class));

        expect(converter.getBuildURI(build)).andReturn(serverUri.resolve("/blah/"));
        expect(build.getAction(AbstractTestResultAction.class)).andReturn(results);

        int failCount = 5;
        int skipCount = 0;
        int passCount = 12;
        int total = failCount + skipCount + passCount;

        expect(results.getUrlName()).andReturn("theResults");
        expect(results.getFailCount()).andReturn(failCount).atLeastOnce();
        expect(results.getSkipCount()).andReturn(skipCount).atLeastOnce();
        expect(results.getTotalCount()).andReturn(total);

        mocks.replayAll();

        // Execute
        JSONObject answer = converter.getTestResults(build);

        // Verify
        mocks.verifyAll();
        assertEquals(passCount, answer.getInt("passed_count"));
    }

    @Test
    public void testURLIsCorrect() throws URISyntaxException {
        // Setup
        AbstractTestResultAction results = mocks.createMock("results", AbstractTestResultAction.class);
        converter = mocks.createMock("converter", converterBuilder.addMockedMethod("getBuildURI", Run.class));

        expect(converter.getBuildURI(build)).andReturn(serverUri.resolve("/blah/"));

        expect(build.getAction(AbstractTestResultAction.class)).andReturn(results);

        expect(results.getUrlName()).andReturn("theResults");
        expect(results.getFailCount()).andReturn(0).atLeastOnce();
        expect(results.getSkipCount()).andReturn(0).atLeastOnce();
        expect(results.getTotalCount()).andReturn(0);

        mocks.replayAll();

        URI expectedURI = serverUri.resolve("/blah/theResults");

        // Execute
        JSONObject answer = converter.getTestResults(build);

        // Verify
        mocks.verifyAll();
        assertEquals(expectedURI.toString(), answer.getString("url"));
    }

    /**
     * Forced builds have no revisions.  Return empty set
     */
    @Test
    public void noRevisionsReturnsEmptyArray() throws IOException {
        //Setup
        converter = mocks.createMock("converter",
                converterBuilder.addMockedMethod("getRepositoryInfo", Run.class));
        expect(converter.getRepositoryInfo(build)).andReturn(repositoryInfo);

        ChangeLogSet changeLogSet = new FakeChangeLogSet(build);
        expect(build.getChangeSet()).andReturn(changeLogSet).atLeastOnce();
        expect(build.getPreviousBuild()).andReturn(null);

        mocks.replayAll();

        // Execute

        JSONArray actual = converter.getRevisions(build);

        // Verify
        mocks.verifyAll();
        assert(actual.isEmpty());
    }

    /**
     * Check the data returned from a single revision
     */
    @Test
    public void revisionReturnsCorrectData() throws IOException {
        //Setup
        converter = mocks.createMock("converter",
                converterBuilder.addMockedMethod("getRepositoryInfo", Run.class));
        expect(converter.getRepositoryInfo(build)).andReturn(repositoryInfo);

        ChangeLogSet changeLogSet = new FakeChangeLogSet(build, new FakeChangeLogEntry("12345"));
        expect(build.getChangeSet()).andReturn(changeLogSet).atLeastOnce();

        mocks.replayAll();

        // Execute

        JSONArray actual = converter.getRevisions(build);
        JSONObject jsonObject = actual.getJSONObject(0);

        // Verify
        mocks.verifyAll();
        assertFalse(actual.isEmpty());
        assertEquals("12345", jsonObject.get("revision"));
        assertEquals("foo", jsonObject.get("repository_type"));
        assertEquals("bar://omg.wtf/bbq", jsonObject.get("repository_url"));
    }

    /**
     * Forced builds have no revisions.  Return empty set
     */
    @Test
    public void noRevisionsReturnsPreviousBuild() throws IOException {
        //Setup
        AbstractBuild build2 = mocks.createMock("build2", AbstractBuild.class);
        converter = mocks.createMock("converter",
                converterBuilder.addMockedMethod("getRepositoryInfo", Run.class));
        expect(converter.getRepositoryInfo(build)).andReturn(repositoryInfo);

        ChangeLogSet changeLogSet = new FakeChangeLogSet(build);
        ChangeLogSet changeLogSet2 = new FakeChangeLogSet(build, new FakeChangeLogEntry("252525"));
        expect(build.getChangeSet()).andReturn(changeLogSet).atLeastOnce();
        AbstractBuild previousBuild = build.getPreviousBuild();
        expect(previousBuild).andReturn(build2);
        expect(build2.getChangeSet()).andReturn(changeLogSet2).atLeastOnce();

        mocks.replayAll();

        // Execute

        JSONArray actual = converter.getRevisions(build);
        JSONObject jsonObject = actual.getJSONObject(0);

        // Verify
        mocks.verifyAll();
        assertFalse(actual.isEmpty());
        assertEquals("252525", jsonObject.get("revision"));
    }

    @Test
    public void usernameRemoved() throws URISyntaxException {
        // Setup
        mocks.replayAll();

        // Execute
        URI actual = converter.stripUserAndPassword("http://us%65r@foo.bar.com/foo/bar");

        // Verify
        mocks.verifyAll();
        URI expected = new URI("http://foo.bar.com/foo/bar");
        assertEquals(expected, actual);
    }

    @Test
    public void usernameAndPasswordRemoved() throws URISyntaxException {
        // Setup
        mocks.replayAll();

        // Execute
        URI actual = converter.stripUserAndPassword("http://user:%40b3doen@foo.bar.com/foo/bar");

        // Verify
        mocks.verifyAll();
        URI expected = new URI("http://foo.bar.com/foo/bar");
        assertEquals(expected, actual);
    }

    @Test
    public void badURIReturnsNull() throws URISyntaxException {
        // Setup
        mocks.replayAll();

        // Execute
        URI actual = converter.stripUserAndPassword("http://user:#$%$$^@foo.bar.com/foo/bar");

        // Verify
        mocks.verifyAll();
        assertNull(actual);
    }

    @Test
    public void cleanURIReturnsSame() throws URISyntaxException {
        // Setup
        mocks.replayAll();

        // Execute
        String expected = "http://foo.bar.com:2121/foo/bar";
        URI actual = converter.stripUserAndPassword(expected);

        // Verify
        mocks.verifyAll();
        assertEquals(expected, actual.toString());
    }

    @Test
    public void timeFormatChecksOut() {
        // Setup

        // Execute
        String actual = converter.convertTime(new Date(0L));

        // Verify
        assertEquals("1970-01-01T00:00:00Z", actual);
    }
}
