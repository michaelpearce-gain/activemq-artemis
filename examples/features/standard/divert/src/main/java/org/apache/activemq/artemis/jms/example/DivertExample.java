/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.jms.example;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.InitialContext;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.core.client.impl.ServerLocatorImpl;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

/**
 * This examples demonstrates the use of ActiveMQ Artemis "Diverts" to transparently divert or copy messages
 * from one address to another.
 *
 * Please see the readme.html for more information.
 */
public class DivertExample {

   public static void main(final String[] args) throws Exception {
         Connection connection = null;
         InitialContext initialContext = null;
         try {

            ServerLocator locator = new ServerLocatorImpl(false, new TransportConfiguration(NettyConnectorFactory.class.getName()));
            ClientSessionFactory factory = locator.createSessionFactory();
            ClientSession clientSession = factory.createSession();
            ClientProducer clientProducer = clientSession.createProducer();
            clientProducer.send(new SimpleString("exampleQueue"), clientSession.createMessage(true));
            locator.close();

            // Step 1. Create an initial context to perform the JNDI lookup.
            initialContext = new InitialContext();

            // Step 2. Perform a lookup on the queue
            Queue queue = (Queue) initialContext.lookup("queue/exampleQueue");

            // Step 3. Perform a lookup on the Connection Factory
            ConnectionFactory cf = (ConnectionFactory) initialContext.lookup("ConnectionFactory");

            // Step 4.Create a JMS Connection
            connection = cf.createConnection();

            // Step 5. Create a JMS Session
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Step 9. Create a JMS Message Consumer
            MessageConsumer messageConsumer = session.createConsumer(queue);

            // Step 10. Start the Connection
            connection.start();

            // Step 11. Receive the message
            Message messageReceived =  messageConsumer.receive(5000);

            System.out.println("messageReceived = " + messageReceived);
         } finally {
            // Step 12. Be sure to close our JMS resources!
            if (initialContext != null) {
               initialContext.close();
            }
            if (connection != null) {
               connection.close();
            }
         }
      }
}
