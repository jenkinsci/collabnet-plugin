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

import hudson.XmlFile;
import hudson.model.Project;
import hudson.plugins.collabnet.orchestrate.BuildNotifier;
import hudson.util.Secret;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * Tests that the plugin is encrypting it's "serverPassword" value on disk
 */
public class TestPasswordEncryption {

    /** Spins up a Jenkins instance to run these tests. Also provides Jenkins objects we can use for testing. */
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    
    Project job;
    String username;
    Secret password;
    
    @Before
    public void setUp() throws Exception {

        username = "testUsername";
        password = Secret.fromString("testPwd");
        job = jenkinsRule.createFreeStyleProject();
        
        BuildNotifier orcPublisher = new BuildNotifier(null,"amqp://example.com", username, password, "sourceKey");
        job.getPublishersList().add(orcPublisher);

    }

    /**
     * Tests that the job config xml contains the encrypted server password, not the plain-text
     * @throws Exception
     */
    @Test
    public void testJobConfigPasswordEncryption() throws Exception {
        XmlFile f = job.getConfigFile();
        String configString = f.asString();
        assertTrue("The password field for orchestrate should be encrypted",
                configString.contains("<serverPassword>" + password.getEncryptedValue() + "</serverPassword>"));
        assertFalse("The clear password for orchestrate should not be present",
                configString.contains("<serverPassword>" + password.getPlainText() + "</serverPassword>"));
    }

}
