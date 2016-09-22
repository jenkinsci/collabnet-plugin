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

import hudson.plugins.collabnet.orchestrate.BuildNotifierDescriptor;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerRequest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

/**
 * Unit tests for the BuildNotifierDescriptor class.
 */
public class TestBuildNotifierDescriptor {

    /** Spins up a Jenkins instance to run these tests. Also provides Jenkins objects we can use for testing. */
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    /** Descriptor to test. */
    private BuildNotifierDescriptor descriptor;

    /** Runs before each test. */
    @Before
    public void setUp() throws Exception {
        descriptor = new BuildNotifierDescriptor();
    }

    /** We don't care what kind of project we're running in. */
    @Test
    public void testApplicableToAllProjects() {
        assertTrue(descriptor.isApplicable(null));
    }

    /** The global server url is configured from a json object. */
    @Test
    public void serverUrlConfiguredFromJson() throws Exception {
        StaplerRequest request = EasyMock.createMock(StaplerRequest.class);

        assertNull(descriptor.getServerUrl());
        descriptor.configure(request, JSONObject.fromObject("{\"serverUrl\": \"someUrl\", \"serverUsername\": \"someUsername\", \"serverPassword\": \"somePassword\"}"));
        assertEquals("someUrl", descriptor.getServerUrl());
    }

    /** Null server urls are not allowed. */
    @Test
    public void nullServerUrlNotAllowed() {
        assertError(descriptor.doCheckServerUrl(null));
    }

    /** Empty string server urls are not allowed. */
    @Test
    public void emptyServerUrlNotAllowed() {
        assertError(descriptor.doCheckServerUrl(""));
    }

    /** If you supply something, we'll try to connect to it. */
    @Test
    public void anyStringAllowedForServerUrl() {
        assertOk(descriptor.doCheckServerUrl("some string"));
    }

    /** Null source keys are not allowed. */
    @Test
    public void nullSourceKeyNotAllowed() {
        assertError(descriptor.doCheckServerUrl(null));
    }

    /** Empty source keys are not allowed. */
    @Test
    public void emptySourceKeyNotAllowed() {
        assertError(descriptor.doCheckSourceKey(""));
    }

    /** Any string is allowed for a source key. */
    @Test
    public void anyStringAllowedForSourceKey() {
        assertOk(descriptor.doCheckSourceKey("abc"));
    }

    /** Asserts that the given validation result is OK. */
    private void assertOk(FormValidation result) {
        assertThat(result.kind, equalTo(FormValidation.Kind.OK));
    }

    /** Asserts that the given validation result is an error. */
    private void assertError(FormValidation result) {
        assertThat(result.kind, equalTo(FormValidation.Kind.ERROR));
    }

}
