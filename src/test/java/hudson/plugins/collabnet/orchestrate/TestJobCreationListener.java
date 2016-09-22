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

import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Project;
import hudson.plugins.collabnet.orchestrate.BuildNotifier;
import hudson.plugins.collabnet.orchestrate.JobCreationListener;
import hudson.plugins.collabnet.util.MockUtils;
import hudson.util.DescribableList;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

/**
 * Exercise the JobCreationListener
 */
public class TestJobCreationListener {

    private MockUtils mocks;

    @Before
    public void setUp() throws IOException {
        mocks = new MockUtils();
    }

    /**
     * Verifiy that a copied job having the BuildNotifier post-build action
     * will get the source key set to null
     */
    @Test
    public void testOnCreatedWithProjectHavingOrchestrateBuildNotifier() {

        // setup mocks
        Project projectCopy = mocks.createMock("copy", FreeStyleProject.class);
        DescribableList publishers = mocks.createMock("publishers", DescribableList.class);

        BuildNotifier buildNotifier = mocks.createMock("buildNotifier", BuildNotifier.class);

        expect(projectCopy.getPublishersList()).andReturn(publishers);
        expect(publishers.get(BuildNotifier.class)).andReturn(buildNotifier);
        buildNotifier.setSourceKey(null);
        expectLastCall().once();
        mocks.replayAll();

        // exercise
        JobCreationListener jobCreationListener = new JobCreationListener();
        jobCreationListener.onCreated(projectCopy);

        // verify
        mocks.verifyAll();
    }

    /**
     * Test that a job without the BuildNotifier action can still be copied
     */
    @Test
    public void testOnCreatedWithProjectNotHavingOrchestrateBuildNotifier() {

        // setup mocks
        Project projectCopy = mocks.createMock("copy", FreeStyleProject.class);
        DescribableList publishers = mocks.createMock("publishers", DescribableList.class);

        expect(projectCopy.getPublishersList()).andReturn(publishers);
        expect(publishers.get(BuildNotifier.class)).andReturn(null);
        mocks.replayAll();

        // exercise
        JobCreationListener jobCreationListener = new JobCreationListener();
        jobCreationListener.onCreated(projectCopy);

        // verify
        mocks.verifyAll();
    }

    /**
     * Test that Items which are not projects can still be copied
     */
    @Test
    public void testOnCreatedWithNonProject() {

        // setup mocks
        Job jobCopy = mocks.createMock("copy", Job.class);

        mocks.replayAll();

        // exercise
        JobCreationListener jobCreationListener = new JobCreationListener();
        jobCreationListener.onCreated(jobCopy);

        //verify
        mocks.verifyAll();
    }
}
