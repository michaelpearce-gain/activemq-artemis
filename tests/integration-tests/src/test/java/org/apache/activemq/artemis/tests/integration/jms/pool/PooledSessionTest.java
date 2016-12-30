/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.tests.integration.jms.pool;

import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicSession;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PooledSessionTest extends JmsPoolTestSupport {

   private ActiveMQConnectionFactory factory;
   private PooledConnectionFactory pooledFactory;
   private String connectionUri;

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      server.start();

      connectionUri = "tcp://localhost:61616";
      factory = new ActiveMQConnectionFactory(connectionUri);
      pooledFactory = new PooledConnectionFactory();
      pooledFactory.setConnectionFactory(factory);
      pooledFactory.setMaxConnections(1);
      pooledFactory.setBlockIfSessionPoolIsFull(false);
   }

   @Override
   @After
   public void tearDown() throws Exception {
      try {
         pooledFactory.stop();
      } catch (Exception ex) {
         // ignored
      }

      super.tearDown();
   }

   @Test(timeout = 60000)
   public void testPooledSessionStats() throws Exception {
      PooledConnection connection = (PooledConnection) pooledFactory.createConnection();

      Assert.assertEquals(0, connection.getNumActiveSessions());
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Assert.assertEquals(1, connection.getNumActiveSessions());
      session.close();
      Assert.assertEquals(0, connection.getNumActiveSessions());
      Assert.assertEquals(1, connection.getNumtIdleSessions());
      Assert.assertEquals(1, connection.getNumSessions());

      connection.close();
   }

   @Test(timeout = 60000)
   public void testMessageProducersAreAllTheSame() throws Exception {
      PooledConnection connection = (PooledConnection) pooledFactory.createConnection();
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

      Queue queue1 = session.createTemporaryQueue();
      Queue queue2 = session.createTemporaryQueue();

      PooledProducer producer1 = (PooledProducer) session.createProducer(queue1);
      PooledProducer producer2 = (PooledProducer) session.createProducer(queue2);

      assertSame(producer1.getMessageProducer(), producer2.getMessageProducer());

      connection.close();
   }

   @Test(timeout = 60000)
   public void testThrowsWhenDifferentDestinationGiven() throws Exception {
      PooledConnection connection = (PooledConnection) pooledFactory.createConnection();
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

      Queue queue1 = session.createTemporaryQueue();
      Queue queue2 = session.createTemporaryQueue();

      PooledProducer producer = (PooledProducer) session.createProducer(queue1);

      try {
         producer.send(queue2, session.createTextMessage());
         fail("Should only be able to send to queue 1");
      } catch (Exception ex) {
      }

      try {
         producer.send(null, session.createTextMessage());
         fail("Should only be able to send to queue 1");
      } catch (Exception ex) {
      }

      connection.close();
   }

   @Test(timeout = 60000)
   public void testCreateTopicPublisher() throws Exception {
      PooledConnection connection = (PooledConnection) pooledFactory.createConnection();
      TopicSession session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

      Topic topic1 = session.createTopic("Topic-1");
      Topic topic2 = session.createTopic("Topic-2");

      PooledTopicPublisher publisher1 = (PooledTopicPublisher) session.createPublisher(topic1);
      PooledTopicPublisher publisher2 = (PooledTopicPublisher) session.createPublisher(topic2);

      assertSame(publisher1.getMessageProducer(), publisher2.getMessageProducer());
      connection.close();
   }

   @Test(timeout = 60000)
   public void testQueueSender() throws Exception {
      PooledConnection connection = (PooledConnection) pooledFactory.createConnection();
      QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

      Queue queue1 = session.createTemporaryQueue();
      Queue queue2 = session.createTemporaryQueue();

      PooledQueueSender sender1 = (PooledQueueSender) session.createSender(queue1);
      PooledQueueSender sender2 = (PooledQueueSender) session.createSender(queue2);

      assertSame(sender1.getMessageProducer(), sender2.getMessageProducer());
      connection.close();
   }

   @Test(timeout = 60000)
   public void testRepeatedCreateSessionProducerResultsInSame() throws Exception {
      PooledConnection connection = (PooledConnection) pooledFactory.createConnection();

      assertTrue(pooledFactory.isUseAnonymousProducers());

      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      Destination destination = session.createTopic("test-topic");
      PooledProducer producer = (PooledProducer) session.createProducer(destination);
      MessageProducer original = producer.getMessageProducer();
      assertNotNull(original);
      session.close();

      //assertEquals(1, server.getAdminView().getDynamicDestinationProducers().length);

      for (int i = 0; i < 20; ++i) {
         session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
         producer = (PooledProducer) session.createProducer(destination);
         assertSame(original, producer.getMessageProducer());
         session.close();
      }

      //assertEquals(1, server.getAdminView().getDynamicDestinationProducers().length);

      connection.close();
      pooledFactory.clear();
   }
}
