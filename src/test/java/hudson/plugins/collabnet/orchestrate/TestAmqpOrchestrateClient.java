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

import com.rabbitmq.client.*;

import hudson.plugins.collabnet.orchestrate.AmqpOrchestrateClient;
import hudson.plugins.collabnet.util.MockUtils;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

/**
 * Tests the AmqpOrchestrateClient class.
 */
public class TestAmqpOrchestrateClient {

    /** Mock helper. */
    private MockUtils mocks;

    /** Client to test. */
    private AmqpOrchestrateClient orchestrateClient;

    /** MQ Connection factory mock */
    private ConnectionFactory mqConnectionFactory;

    /** Sets up the tests. */
    @Before
    public void setUp() {
        mocks = new MockUtils();
        mqConnectionFactory = mocks.createMock("mqConnectionFactory", ConnectionFactory.class);
        orchestrateClient = new AmqpOrchestrateClient(mqConnectionFactory);
    }

    /** Tests that a post fails when server is down */
    @Test(expected = IOException.class)
    public void postBuildFailsWithUnreachableHost() throws Exception {
        // set up
        String serverUrl = "amqp://test.host";

        // exercise
        orchestrateClient.postBuild(serverUrl, "someUser", "somePassword", "someData");
    }

    /** Tests that the MQ client attempts to use credentials when provided */
    @Test
    public void postBuildUsesProvidedCredentials() throws Exception {
        // set up
        String serverUrl = "amqp://test.host";
        String serverUsername = "someuser";
        String serverPassword= "somepassword";
        String messageBody = "somemessage";

        Connection conn = mocks.createMock("mqConn", Connection.class);
        Channel channel = mocks.createNiceMock("mqChannel", Channel.class);
        AMQP.Queue.DeclareOk ok = mocks.createMock("mqOk", AMQP.Queue.DeclareOk.class);

        mqConnectionFactory.setUri(serverUrl);
        expectLastCall().once();
        mqConnectionFactory.setUsername(serverUsername);
        expectLastCall().once();
        mqConnectionFactory.setPassword(serverPassword);
        expectLastCall().once();

        expect(mqConnectionFactory.newConnection()).andReturn(conn);
        expect(conn.createChannel()).andReturn(channel);
        expect(channel.queueDeclare(orchestrateClient.getRoutingKey(), true, false, false, null)).andReturn(ok);

        channel.close();
        expectLastCall().once();

        conn.close();
        expectLastCall().once();

        mocks.replayAll();

        // exercise
        orchestrateClient.postBuild(serverUrl, serverUsername, serverPassword, messageBody);

        // verify
        mocks.verifyAll();
    }


    /** Tests that the MQ client does not use credentials when not provided */
    @Test
    public void postBuildUsesNoCredentialsWhenNull() throws Exception {
        // set up
        String serverUrl = "amqp://test.host";
        String serverUsername = null;
        String serverPassword= null;
        String messageBody = "somemessage";

        Connection conn = mocks.createMock("mqConn", Connection.class);
        Channel channel = mocks.createNiceMock("mqChannel", Channel.class);
        AMQP.Queue.DeclareOk ok = mocks.createMock("mqOk", AMQP.Queue.DeclareOk.class);

        mqConnectionFactory.setUri(serverUrl);
        expectLastCall().once();

        expect(mqConnectionFactory.newConnection()).andReturn(conn);
        expect(conn.createChannel()).andReturn(channel);
        expect(channel.queueDeclare(orchestrateClient.getRoutingKey(), true, false, false, null)).andReturn(ok);

        channel.close();
        expectLastCall().once();

        conn.close();
        expectLastCall().once();

        mocks.replayAll();

        // exercise
        orchestrateClient.postBuild(serverUrl, serverUsername, serverPassword, messageBody);

        // verify
        mocks.verifyAll();
    }

    /** Validate the exchange name */
    @Test
    public void defaultExchangeIsUsed() {
        String defaultExchangeName = "";
        TestCase.assertEquals("The 'default' exchange should be used", defaultExchangeName,
                orchestrateClient.getExchangeName());
    }

    /** Validate the queue / routing key */
    @Test
    public void orchestrateQueueIsUsed() {
        String orchestrateQueueName = "orchestrate.builds";
        TestCase.assertEquals("The 'orchestrate.builds' routing key should be used", orchestrateQueueName,
                orchestrateClient.getRoutingKey());
    }

    /** Validate the message durability setting */
    @Test
    public void persistentMessageTypeIsUsed() {
        AMQP.BasicProperties persistentMessageType = MessageProperties.PERSISTENT_TEXT_PLAIN;
        TestCase.assertEquals("The persistent message type should be used", persistentMessageType,
                orchestrateClient.getProperties());
    }


}
