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
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.collabnet.orchestrate.AmqpOrchestrateClient;
import hudson.plugins.collabnet.orchestrate.BuildNotifier;
import hudson.plugins.collabnet.orchestrate.BuildToOrchestrateAPI;
import hudson.plugins.collabnet.orchestrate.OrchestrateClient;
import hudson.plugins.collabnet.util.MockUtils;
import hudson.tasks.BuildStepMonitor;
import hudson.util.Secret;
import org.apache.http.HttpResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.*;

/**
 * Unit tests for the TestBuildNotifier class.
 */
public class TestBuildNotifier {

    /** Spins up a Jenkins instance to run these tests. Also provides Jenkins objects we can use for testing. */
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    /** The association key to test with. */
    private String associationKey;

    /** The server URL to connect to. */
    private String serverUrl;

    /** The username for authentication against the Orchestrate or MQ server */
    private String serverUsername;

    /** The  password for authentication against the Orchestrate or MQ server */
    private Secret serverPassword;

    /** Helper for mocks. */
    private MockUtils mocks;

    /** A mock listener. */
    private BuildListener listener;

    /** A mock PrintStream. */
    private PrintStream printer;

    /** A mock OutputStream which can be inspected */
    ByteArrayOutputStream outStream;

    /** The notifier to test. */
    private BuildNotifier notifier;

    /** Ctf traceability params */
    private String ctfUrl;
    private String ctfUser;
    private Secret ctfPassword;


    /** Sets up the tests. */
    @Before
    public void setUp() {
        serverUrl = "http://orchestrate.test";
        serverUsername = "username";
        serverPassword = Secret.fromString("password");
        associationKey = "q20394SAfasrd";
        ctfUrl = "http://teamforge.test/sf/project";
        ctfUser = "admin";
        ctfPassword = Secret.fromString("password");
        notifier = new BuildNotifier(null,serverUrl, serverUsername, serverPassword, associationKey);
        mocks = new MockUtils();
        listener = mocks.createMock("listener", BuildListener.class);
        outStream = new ByteArrayOutputStream(2048);
        printer = new PrintStream(outStream);
        expect(listener.getLogger()).andReturn(printer);
        
    }

    /** Check that we don't use 'old' step monitor functionality. */
    @Test
    public void notifierHasNoStepMonitor() {
        assertEquals(BuildStepMonitor.NONE, notifier.getRequiredMonitorService());
    }

    /** Tests that the build is converted and sent to the server during the perform method. */
    @Test
    public void performBuildConversionAndSendToServer() throws Exception {
        // set up
        AbstractBuild build = expectSendToServer(serverUrl, associationKey);

        // exercise
        notifier.perform(build, null, listener);

        // verify
        mocks.verifyAll();
    }

    /**
     * Configures the mocks to expect to send the build result to the server.
     *
     * @param url the expected url
     * @param key the expected source key
     * @return the expected build
     * @throws IOException if an error occurs
     */
    private AbstractBuild expectSendToServer(String url, String key) throws IOException {
        BuildToOrchestrateAPI converter = mocks.createMock("converter", BuildToOrchestrateAPI.class);
        AbstractBuild build = mocks.createMock("build", AbstractBuild.class);
        OrchestrateClient orchestrateClient = mocks.createMock("orchestrateClient", OrchestrateClient.class);
        HttpResponse response = mocks.createMock("response", HttpResponse.class);
        notifier.setConverter(converter);
        notifier.setOrchestrateClient(orchestrateClient);

        printer.println("Starting to send build information to TeamForge EventQ...");

        String json = "somedata";
        expect(converter.toOrchestrateAPI(build, key)).andReturn(json);
        orchestrateClient.postBuild(url, serverUsername, Secret.toString(serverPassword), json);
        mocks.replayAll();
        return build;
    }

    /** Checks that a build is marked unstable when an exception happens communicating with the server. */
    @Test
    public void exceptionDuringPerformCausesUnstableBuild() throws Exception {
        // set up
        BuildToOrchestrateAPI converter = mocks.createMock("converter", BuildToOrchestrateAPI.class);
        AbstractBuild build = mocks.createMock("build", AbstractBuild.class);
        OrchestrateClient orchestrateClient = mocks.createMock("orchestrateClient", OrchestrateClient.class);
        notifier.setOrchestrateClient(orchestrateClient);
        notifier.setConverter(converter);

        printer.println("Starting to send build information to TeamForge EventQ...");
        expect(converter.toOrchestrateAPI(build, associationKey)).andReturn("json");
        orchestrateClient.postBuild(serverUrl, serverUsername, Secret.toString(serverPassword), "json");
        expectLastCall().andThrow(new IOException());
        build.setResult(Result.UNSTABLE);

        mocks.replayAll();

        // exercise
        notifier.perform(build, null, listener);

        // verify
        mocks.verifyAll();
    }

    /** Strip user-provided input before trying to contact the server. */
    @Test
    public void performBuildStripsServerAndSourceKeys() throws Exception {
        // set up
        notifier = new BuildNotifier(null," " + serverUrl + " ", serverUsername, serverPassword, " " + associationKey + " ");
        AbstractBuild build = expectSendToServer(serverUrl, associationKey);

        // exercise
        notifier.perform(build, null, listener);

        // verify
        mocks.verifyAll();
    }

    /**
     * Since configuration can be modified outside of the Jenkins UI, verify that we don't try to contact the
     * Orchestrate server when one is not specified.
     */
    @Test
    public void performDoesNotTryToContactEmptyServer() throws Exception {
        // set up
        notifier = new BuildNotifier(null,"", serverUsername, serverPassword, "somekey");
        AbstractBuild build = mocks.createMock("build", AbstractBuild.class);

        printer.println("Starting to send build information to TeamForge EventQ...");
        printer.println("The URL to the TeamForge EventQ server is missing.");

        build.setResult(Result.UNSTABLE);

        mocks.replayAll();

        // exercise
        notifier.perform(build, null, listener);

        // verify
        mocks.verifyAll();
    }

    /**
     * Since configuration can be modified outside of the Jenkins UI, verify that we don't try to contact the
     * EventQ server when the source key is not specified.
     */
    @Test
    public void performDoesNotTryToContactServerWithNoSourceKey() throws Exception {
        // set up
        notifier = new BuildNotifier(null,"http://orchestrate.test", serverUsername, serverPassword, "");
        AbstractBuild build = mocks.createMock("build", AbstractBuild.class);

        printer.println("Starting to send build information to TeamForge EventQ...");
        printer.println("The source key for TeamForge EventQ build source is missing.");

        build.setResult(Result.UNSTABLE);

        mocks.replayAll();

        // exercise
        notifier.perform(build, null, listener);

        // verify
        mocks.verifyAll();
    }

    /**
     * If the converter throws an IllegalStateException (as happens when Jenkins root url
     * is not configured), the notification will stop.
     */
    @Test
    public void performDoesNotTryToContactServerWithoutJenkinsRootURL() throws Exception {
        // set up
        String sourceKey = "somekey";
        notifier = new BuildNotifier(null,"http://orchestrate.test", serverUsername, serverPassword, sourceKey);
        AbstractBuild build = mocks.createMock("build", AbstractBuild.class);
        BuildToOrchestrateAPI converter = mocks.createMock("converter", BuildToOrchestrateAPI.class);
        notifier.setConverter(converter);
        expect(converter.toOrchestrateAPI(build, sourceKey)).andThrow(new IllegalStateException());

        printer.println("Starting to send build information to TeamForge Orchestrate...");
        printer.println("The Jenkins instance has no root url");

        build.setResult(Result.UNSTABLE);

        mocks.replayAll();

        // exercise
        notifier.perform(build, null, listener);

        // verify
        mocks.verifyAll();
    }


    /**
     * The perform method will execute "postBuild" on the AMQP
     * client implementation based on serverUrl protocol
     */
    @Test
    public void performUsesAmqpClientClass() throws Exception {
        // set up
        notifier = new BuildNotifier(null,"amqp://mq.example.com:5672", serverUsername, serverPassword, "somekey");
        AbstractBuild build = mocks.createMock("build", AbstractBuild.class);
        BuildToOrchestrateAPI converter = mocks.createMock("converter", BuildToOrchestrateAPI.class);
        notifier.setConverter(converter);
        expect(converter.toOrchestrateAPI(build, "somekey")).andReturn("{}");
        build.setResult(Result.UNSTABLE);
        printer.println("Starting to send build information to TeamForge EventQ...");
        mocks.replayAll();

        // exercise
        notifier.perform(build, null, listener);

        // verify
        assertTrue("The AMQP client should be selected",
                notifier.getOrchestrateClient() instanceof AmqpOrchestrateClient);
    }

    /**
     * The perform method will logs failures in the build console log
     */
    @Test
    public void performLogsNotifierActivity() throws Exception {
        // set up
        notifier = new BuildNotifier(null,"amqp://mq.example.com:5672", serverUsername, serverPassword, "somekey");
        AbstractBuild build = mocks.createMock("build", AbstractBuild.class);
        BuildToOrchestrateAPI converter = mocks.createMock("converter", BuildToOrchestrateAPI.class);
        notifier.setConverter(converter);
        expect(converter.toOrchestrateAPI(build, "somekey")).andReturn("{}");
        build.setResult(Result.UNSTABLE);
        mocks.replayAll();

        // exercise
        notifier.perform(build, null, listener);

        // verify log
        String logOutput = outStream.toString();
        assertTrue("Log should indicate when notifier starts", logOutput.contains("TeamForge EventQ Build Notifier - Sending build information"));
        assertTrue("Log should indicate when notifier fails", logOutput.contains("TeamForge EventQ Build Notifier - Build information NOT sent"));
    }
}
