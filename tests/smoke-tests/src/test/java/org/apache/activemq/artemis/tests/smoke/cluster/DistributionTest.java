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

import org.apache.activemq.artemis.tests.smoke.categories.ThreeNode;
import org.apache.activemq.artemis.tests.smoke.common.ThreeNodeTestBase;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(Arquillian.class)
@Category(ThreeNode.class)
public class DistributionTest extends ThreeNodeTestBase {
   private static int NUM_MESSAGES = 10000;

   @Test
   @RunAsClient
   public void testOnDemandWithRedistribution() throws Exception {
      Connection live1Connection = null;
      Connection live2Connection = null;
      Connection live3Connection = null;
      try {
         ConnectionFactory live2ConnectionFactory = createConnectionFactory(LIVE_2, getProtocol(), false);

         live2Connection = live2ConnectionFactory.createConnection();

         live2Connection.start();

         Session live2Session = live2Connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

         Queue replicatedQueue = live2Session.createQueue("replicatedQueue");

         MessageConsumer live2Consumer = live2Session.createConsumer(replicatedQueue);

         CountDownLatch live2latch = new CountDownLatch(NUM_MESSAGES / 10);
         AtomicReference<Integer> messageCount2 = new AtomicReference<>(0);
         live2Consumer.setMessageListener(message -> {
            live2latch.countDown();
            messageCount2.getAndSet(messageCount2.get() + 1);
            if (live2latch.getCount() == 0) {
               System.out.println("Consumer on Broker 3 received " + messageCount2.get() + " messages");
               try {
                  live2Consumer.setMessageListener(null);
               } catch (JMSException e) {
                  e.printStackTrace();
               }
            }
         });

         ConnectionFactory live3ConnectionFactory = createConnectionFactory(LIVE_3, getProtocol(), false);

         live3Connection = live3ConnectionFactory.createConnection();

         live3Connection.start();

         Session live3Session = live3Connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

         MessageConsumer live3Consumer = live3Session.createConsumer(replicatedQueue);

         CountDownLatch live3latch = new CountDownLatch(NUM_MESSAGES / 2);
         AtomicReference<Integer> messageCount3 = new AtomicReference<>(0);
         live3Consumer.setMessageListener(message -> {
            messageCount3.getAndSet(messageCount3.get() + 1);
            live3latch.countDown();
            if (live3latch.getCount() == 0) {
               System.out.println("Consumer on Broker 2 received " + messageCount3.get() + " messages");
               try {
                  live3Consumer.setMessageListener(null);
               } catch (JMSException e) {
                  e.printStackTrace();
               }
            }
         });

         ConnectionFactory live1ConnectionFactory = createConnectionFactory(LIVE_1, getProtocol(), false);

         live1Connection = live1ConnectionFactory.createConnection();

         Session live1Session = live1Connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

         MessageProducer producer = live1Session.createProducer(null);

         Thread t = new Thread(() -> {
            for (int i = 0; i < NUM_MESSAGES; i++) {
               try {
                  producer.send(replicatedQueue, live1Session.createMessage());
               } catch (Exception e) {
                  e.printStackTrace();
               }
            }
            System.out.println("sent " + NUM_MESSAGES + " messages");
         });

         t.start();

         Assert.assertTrue(live2latch.await(30, TimeUnit.SECONDS));
         Assert.assertTrue(live3latch.await(30, TimeUnit.SECONDS));

         live2Consumer.close();

         for (int i = 0; i < NUM_MESSAGES / 10 * 4; i++) {
            Message receive = live3Consumer.receive(5000);
            Assert.assertNotNull(receive);
         }

         System.out.println("received " + NUM_MESSAGES / 10 * 4 + " redistributed messages");
      } finally {
         if (live1Connection != null) {
            try {
               live1Connection.close();
            } catch (JMSException e) {
            }
         }
         if (live2Connection != null) {
            try {
               live2Connection.close();
            } catch (JMSException e) {
            }
         }
         if (live3Connection != null) {
            try {
               live3Connection.close();
            } catch (JMSException e) {
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
