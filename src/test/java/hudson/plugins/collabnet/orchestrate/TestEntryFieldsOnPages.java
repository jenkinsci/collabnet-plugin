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

import com.gargoylesoftware.htmlunit.WebAssert;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.FreeStyleProject;
import hudson.plugins.collabnet.orchestrate.BuildNotifier;
import hudson.util.Secret;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Tests the entry fields on the configuration pages.
 */
public class TestEntryFieldsOnPages {

    /** Spins up a Jenkins instance to run these tests. Also provides Jenkins objects we can use for testing. */
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    public FreeStyleProject project;

    public JenkinsRule.WebClient webClient;

    @Before
    public void setUp() throws Exception {

        project = jenkinsRule.createFreeStyleProject();
        webClient = jenkinsRule.createWebClient();
    }

    /** Tests the configuration properties on the global page. */
    @Test
    public void jobConfigurationHasConnectionInfoAndSourceKey() throws Exception {

        BuildNotifier orcPublisher = new BuildNotifier(null,"testUrl", "testUsername", Secret.fromString("testPwd"), "sourceKey");

        project.getPublishersList().add(orcPublisher);

        HtmlPage page = webClient.getPage(project, "configure");

        WebAssert.assertElementPresent(page, "orchestrate-serverUrl");
        WebAssert.assertElementPresent(page, "orchestrate-serverUsername");
        WebAssert.assertElementPresent(page, "orchestrate-serverPassword");
        WebAssert.assertElementPresent(page, "orchestrate-sourceKey");
    }

    /** Validates the Server Url help text */
    @Test
    public void configurationHelpHasServerUrlExamples() throws Exception {
        HtmlPage page = webClient.goTo("descriptor/hudson.plugins.collabnet.orchestrate.BuildNotifier/help/serverUrl");
        WebAssert.assertTextNotPresent(page, "http://api.example.com");
        WebAssert.assertTextPresent(page, "amqp://mq.example.com:5672");
    }
}
