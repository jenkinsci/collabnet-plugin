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
import hudson.plugins.collabnet.orchestrate.BuildToJSON;
import hudson.plugins.collabnet.orchestrate.DefaultBuildToOrchestrateAPI;
import hudson.plugins.collabnet.util.MockUtils;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.expect;

/**
 * Unit tests for the DefaultBuildToOrchestrateAPI
 */
public class TestDefaultBuildToOrchestrateAPI {

    /** The association key to test with. */
    private String associationKey;

    /** Helper for mocks. */
    private MockUtils mocks;

    /** The notifier to test. */
    private DefaultBuildToOrchestrateAPI builder;

    private AbstractBuild<?, ?> build;

    private BuildToJSON converter;

    /** Sets up the tests. */
    @Before
    public void setUp() {
        associationKey = "q20394SAfasrd";
        mocks = new MockUtils();
        build = mocks.createMock("build", AbstractBuild.class);
        converter = mocks.createMock("converter", BuildToJSON.class);

        builder = new DefaultBuildToOrchestrateAPI(converter);
    }

    /** tests successful conversion of a build to JSON. */
    @Test
    public void projectIsConvertedToJson() throws Exception {
        //setup

        JSONObject expectedBuildData = new JSONObject().accumulate("foo", "bar");
        expect(converter.getBuildData(build)).andReturn(expectedBuildData);

        mocks.replayAll();

        //execute
        String actual = builder.toOrchestrateAPI(build, associationKey);

        //verify
        mocks.verifyAll();

        JSONObject jsonObject = JSONObject.fromObject(actual);
        assertEquals("1", jsonObject.get("api_version"));
        assertEquals(associationKey, jsonObject.get("source_association_key"));
        assertEquals(expectedBuildData, jsonObject.get("build_data"));
    }
}
