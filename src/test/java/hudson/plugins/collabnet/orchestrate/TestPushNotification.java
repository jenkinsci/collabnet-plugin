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

import hudson.EnvVars;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import java.io.IOException;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the TestPushNotification class.
 */
public class TestPushNotification {

    /** A mock run. */
    private Run run;

    /** A mock listener. */
    private TaskListener listener;

    private PushNotification pushNotification;

    private Job job;
    private EnvVars envVars;
    private String JOB_URL = "http://jenkins.test.box/jenkins/";
    private String BUILD_URL = "http://jenkins.test.box/jenkins/job/Notifier1.0/43/";
    private int BUILD_NUMBER = 123;

    /** Sets up the tests. */
    @Before
    public void setUp() throws IOException, InterruptedException {
        envVars = new EnvVars();
        run = mock(Run.class);
        listener = mock(TaskListener.class);
        job = mock(Job.class);
        pushNotification = new PushNotification();
        updateMock();
    }

    private void updateMock() throws IOException, InterruptedException {
        envVars.put("JENKINS_URL", JOB_URL);
        envVars.put("BUILD_URL", BUILD_URL);
        when(run.getEnvironment(listener)).thenReturn(envVars);
        when(run.getNumber()).thenReturn(BUILD_NUMBER);
    }

    @Test
    public void testBuildSuccess() throws IOException {
        String json = pushNotification.getPayload(run, listener,"SUCCESS", true);
        assertEquals("Successful",JSONObject.fromObject(json).get("status"));
    }

    @Test
    public void testBuildFailure() throws IOException {
        String json = pushNotification.getPayload(run, listener, "FAILURE", false);        ;
        assertEquals("Failed", JSONObject.fromObject(json).get("status"));
    }

    @Test
    public void testBuildUnstable() throws IOException {
        String json = pushNotification.getPayload(run, listener, "UNSTABLE", false);        ;
        assertEquals("Unstable", JSONObject.fromObject(json).get("status"));
    }

    @Test
    public void testBuildNumber() throws IOException {
        String json = pushNotification.getPayload(run, listener, "SUCCESS", false);        ;
        int id = Integer.parseInt(JSONObject.fromObject(json).get("buildId").toString());
        assertEquals(BUILD_NUMBER, id);
    }

}
