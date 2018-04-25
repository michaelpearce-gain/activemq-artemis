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

package org.apache.activemq.artemis.tests.smoke.crossprotocol;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.IllegalStateException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.tests.smoke.categories.SingleNode;
import org.apache.activemq.artemis.tests.smoke.common.SingleNodeTestBase;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Arquillian.class)
@Category(SingleNode.class)
public class MultiThreadConvertTest extends SingleNodeTestBase {

   private static final Logger LOG = LoggerFactory.getLogger(MultiThreadConvertTest.class);

   @Before
   @Override
   public void before()  {
      disableCheckThread();
      super.before();
   }

   public String getTopicName() {
      return "test-topic-1";
   }

   @Test(timeout = 60000)
   @RunAsClient
   public void testSendLotsOfDurableMessagesOnTopicWithManySubscribersPersistent() throws Exception {
      doTestSendLotsOfDurableMessagesOnTopicWithManySubscribers(DeliveryMode.PERSISTENT);
   }

   @Test(timeout = 60000)
   @RunAsClient
   public void testSendLotsOfDurableMessagesOnTopicWithManySubscribersNonPersistent() throws Exception {
      doTestSendLotsOfDurableMessagesOnTopicWithManySubscribers(DeliveryMode.NON_PERSISTENT);
   }

   private void doTestSendLotsOfDurableMessagesOnTopicWithManySubscribers(int durability) throws Exception {

      final int MSG_COUNT = 400;
      final int SUBSCRIBER_COUNT = 4;
      final int DELIVERY_MODE = durability;

      JmsConnectionFactory amqpFactory = (JmsConnectionFactory) createConnectionFactory(LIVE, PROTOCOL.AMQP, false);//new JmsConnectionFactory("amqp://127.0.0.1:5672");

      Connection amqpConnection = amqpFactory.createConnection();
      final ExecutorService executor = Executors.newFixedThreadPool(SUBSCRIBER_COUNT);

      try (ActiveMQConnectionFactory coreFactory = (ActiveMQConnectionFactory) createConnectionFactory(LIVE, PROTOCOL.CORE, false)) {
         final CountDownLatch subscribed = new CountDownLatch(SUBSCRIBER_COUNT);
         final CountDownLatch done = new CountDownLatch(MSG_COUNT * SUBSCRIBER_COUNT);
         final AtomicBoolean error = new AtomicBoolean(false);

         for (int i = 0; i < SUBSCRIBER_COUNT; ++i) {
            executor.execute(() -> {
               Session coreSession = null;
               Connection coreConnection = null;
               try {
                  coreConnection = coreFactory.createConnection();
                  coreSession = coreConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                  Topic topic = coreSession.createTopic(getTopicName());
                  MessageConsumer coreConsumer = coreSession.createConsumer(topic);

                  subscribed.countDown(); // Signal ready

                  coreConnection.start();

                  for (int j = 0; j < MSG_COUNT; j++) {
                     Message received = coreConsumer.receive(TimeUnit.SECONDS.toMillis(5));
                     done.countDown();

                     if (received.getJMSDeliveryMode() != DELIVERY_MODE) {
                        throw new IllegalStateException("Message durability state is not corret.");
                     }
                  }

               } catch (Throwable t) {
                  LOG.error("Error during message consumption: ", t);
                  error.set(true);
               } finally {
                  try {
                     coreConnection.close();
                  } catch (Throwable e) {
                  }
               }
            });
         }

         assertTrue("Receivers didn't signal ready", subscribed.await(10, TimeUnit.SECONDS));

         // Send using AMQP and receive using Core JMS client.
         Session amqpSession = amqpConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
         Topic topic = amqpSession.createTopic(getTopicName());
         MessageProducer producer = amqpSession.createProducer(topic);
         producer.setDeliveryMode(DELIVERY_MODE);

         for (int i = 0; i < MSG_COUNT; i++) {
            TextMessage message = amqpSession.createTextMessage("test");
            message.setJMSCorrelationID(UUID.randomUUID().toString());
            producer.send(message);
         }

         assertTrue("did not read all messages, waiting on: " + done.getCount(), done.await(30, TimeUnit.SECONDS));
         assertFalse("should not be any errors on receive", error.get());
      } finally {
         try {
            amqpConnection.close();
         } catch (Exception e) {
         }

         executor.shutdown();

      }
   }
}
