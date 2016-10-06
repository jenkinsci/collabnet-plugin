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
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeoutException;

/**
 * Client class for communicating builds to EventQ using Amqp protocol
 */
public class AmqpOrchestrateClient implements OrchestrateClient {

    private static final String DEFAULT_QUEUE_NAME = "orchestrate.builds";
    private static final String DEFAULT_EXCHANGE_NAME = "";

    private ConnectionFactory factory = new ConnectionFactory();

    // these are almost constants -- but could conceivably need configuration per job down the road
    private String exchangeName;
    private String routingKey;
    private AMQP.BasicProperties properties;

    /**
     * Creates an EventQ MQ client with default settings
     */
    public AmqpOrchestrateClient() {
        this.exchangeName = DEFAULT_EXCHANGE_NAME; // the default exchange
        this.routingKey = DEFAULT_QUEUE_NAME; // the routing key / queue name
        this.properties = MessageProperties.PERSISTENT_TEXT_PLAIN; // the message type (durable)
    }

    /**
     * Creates an EventQ MQ client with the given ConnectionFactory (for testing)
     * @param connectionFactory
     */
    public AmqpOrchestrateClient(ConnectionFactory connectionFactory) {
        this();
        this.factory = connectionFactory;
    }

    /** {@inheritDoc} */
    public void postBuild(String serverUrl, String serverUsername, String serverPassword, String buildInformation) throws IOException {

        Connection conn = null;
        Channel channel = null;
        try {

            // verify serverUrl, and clear trailing slash if need be
            URI uri = new URI(serverUrl);
            if (serverUrl.endsWith("/")) {
                serverUrl = serverUrl.substring(0, serverUrl.lastIndexOf("/"));
            }

            factory.setUri(serverUrl);
            // add username / password credentials to request, if needed
            if (!StringUtils.isBlank(serverUsername)) {
                factory.setUsername(serverUsername);
                factory.setPassword(serverPassword);
            }
            conn = factory.newConnection();
            channel = conn.createChannel();
            channel.queueDeclare(routingKey, true, false, false, null);

            byte[] messageBodyBytes = buildInformation.getBytes("UTF-8");
            channel.basicPublish(exchangeName, routingKey, properties, messageBodyBytes);
        }
        catch (Exception e) {
            String errorMsg = "Unable to send build info to the message queue";
            throw new IOException(errorMsg, e);
        }
        finally {
            if (channel != null)  {
                try {
					channel.close();
				} catch (TimeoutException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            if (conn != null) {
                conn.close();
            }
        }
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public AMQP.BasicProperties getProperties() {
        return properties;
    }
}
