/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.tests.smoke.cluster;

import org.apache.activemq.artemis.tests.integration.amqp.AmqpTestSupport;
import org.apache.activemq.artemis.tests.smoke.categories.ThreeNode;
import org.apache.activemq.artemis.tests.smoke.common.ThreeNodeTestBase;
import org.apache.activemq.transport.amqp.client.AmqpClient;
import org.apache.activemq.transport.amqp.client.AmqpConnection;
import org.apache.activemq.transport.amqp.client.AmqpMessage;
import org.apache.activemq.transport.amqp.client.AmqpReceiver;
import org.apache.activemq.transport.amqp.client.AmqpSender;
import org.apache.activemq.transport.amqp.client.AmqpSession;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(Arquillian.class)
@Category(ThreeNode.class)
public class AMQPTestClientDistributionTest extends ThreeNodeTestBase {
   private static int NUM_MESSAGES = 10000;
   private String replicatedQueue = "replicatedQueue";

   @Test
   @RunAsClient
   public void testOnDemandWithRedistribution() throws Exception {
      AmqpTestSupport amqpTestSupport = new AmqpTestSupport();
      AmqpConnection connection1 = null;
      AmqpConnection connection2 = null;
      AmqpConnection connection3 = null;
      try {
         String live1URI = new String("tcp://" + controller.getConnectHost(LIVE_1) + ":" + controller.getConnectPort(LIVE_1, "amqp"));
         String live2URI = new String("tcp://" + controller.getConnectHost(LIVE_2) + ":" + controller.getConnectPort(LIVE_2, "amqp"));
         String live3URI = new String("tcp://" + controller.getConnectHost(LIVE_3) + ":" + controller.getConnectPort(LIVE_3, "amqp"));

         AmqpClient live2AmqpClient = amqpTestSupport.createAmqpClient(new URI(live2URI));

         connection2 = live2AmqpClient.connect();

         AmqpSession session2 = connection2.createSession();

         AmqpReceiver receiver2 = session2.createReceiver(replicatedQueue);

         receiver2.flow(NUM_MESSAGES / 10);

         CountDownLatch live2latch = new CountDownLatch(NUM_MESSAGES / 10);

         AtomicReference<Integer> messageCount2 = new AtomicReference<>(0);

         Thread t2 = new Thread(() -> {
            while (live2latch.getCount() != 0) {
               try {
                  AmqpMessage receive = receiver2.receive();
               } catch (Exception e) {
                  e.printStackTrace();
               }
               live2latch.countDown();
               messageCount2.getAndSet(messageCount2.get() + 1);
            }
            System.out.println("Consumer on Broker 2 received " + messageCount2.get() + " messages");
         });

         t2.start();

         AmqpClient live3AmqpClient = amqpTestSupport.createAmqpClient(new URI(live3URI));

         connection3 = live3AmqpClient.connect();

         AmqpSession session3 = connection3.createSession();

         AmqpReceiver receiver3 = session3.createReceiver(replicatedQueue);

         receiver3.flow(NUM_MESSAGES / 2);

         CountDownLatch live3latch = new CountDownLatch(NUM_MESSAGES / 2);

         AtomicReference<Integer> messageCount3 = new AtomicReference<>(0);

         Thread t3 = new Thread(() -> {
            while (live3latch.getCount() != 0) {
               try {
                  AmqpMessage receive = receiver3.receive();
               } catch (Exception e) {
                  e.printStackTrace();
               }
               live3latch.countDown();
               messageCount3.getAndSet(messageCount3.get() + 1);
            }

            System.out.println("Consumer on Broker 3 received " + messageCount3.get() + " messages");
         });

         t3.start();


         AmqpClient live1AmqpClient = amqpTestSupport.createAmqpClient(new URI(live1URI));

         connection1 = live1AmqpClient.connect();

         AmqpSession session1 = connection1.createSession();

         AmqpSender anonymousSender = session1.createSender(replicatedQueue);

         CountDownLatch latch1 = new CountDownLatch(NUM_MESSAGES);

         Thread t = new Thread(() -> {
            for (int i = 0; i < NUM_MESSAGES; i++) {
               try {
                  AmqpMessage message = new AmqpMessage();
                  message.setText("Test-Message");
                  message.setApplicationProperty("msgId", i);
                  // message.setAddress(replicatedQueue);
                  anonymousSender.send(message);
                  latch1.countDown();
               } catch (Exception e) {
                  e.printStackTrace();
                  break;
               }
            }
            System.out.println("sent " + NUM_MESSAGES + " messages");
         });

         t.start();

         Assert.assertTrue(live2latch.await(30, TimeUnit.SECONDS));
         Assert.assertTrue(live3latch.await(30, TimeUnit.SECONDS));

         connection2.close();

         receiver3.flow(NUM_MESSAGES / 2);

         for (int i = 0; i < NUM_MESSAGES / 10 * 4; i++) {
            AmqpMessage receive = receiver3.receive();
            Assert.assertNotNull(receive);
         }

         System.out.println("received " + NUM_MESSAGES / 10 * 4 + " redistributed messages");

         Assert.assertTrue(latch1.await(30, TimeUnit.SECONDS));
      } finally {
         if (connection1 != null) {
            try {
               connection1.close();
            } catch (Exception e) {
            }
         }
         if (connection2 != null) {
            try {
               connection2.close();
            } catch (Exception e) {
            }
         }
         if (connection3 != null) {
            try {
               connection3.close();
            } catch (Exception e) {
            }
         }
      }
   }

   @Override
   public String getLiveBrokerConfig() {
      return "/servers/clustered/ondemand-broker.xml";
   }

   @Override
   public String getBackupBrokerConfig() {
      return null;
   }

   @Override
   public PROTOCOL getProtocol() {
      return PROTOCOL.AMQP;
   }
}
