/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.activemq.artemis.tests.smoke.expire;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.artemis.tests.smoke.categories.SingleNode;
import org.apache.activemq.artemis.tests.smoke.common.SingleNodeTestBase;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@Category(SingleNode.class)
public class TestSimpleExpire extends SingleNodeTestBase {

   @Before
   @Override
   public void before() {
      disableCheckThread();
      super.before();
   }

   @Test
   @RunAsClient
   public void testSendExpire() throws Exception {
      ConnectionFactory factory = createConnectionFactory(LIVE, PROTOCOL.AMQP, false);
      Connection connection = null;
      try {
         connection = factory.createConnection();
         Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

         Queue queue = session.createQueue("q0");
         MessageProducer producer = session.createProducer(queue);
         producer.setDeliveryMode(DeliveryMode.PERSISTENT);

         producer.setTimeToLive(1000);
         for (int i = 0; i < 20000; i++) {
            producer.send(session.createTextMessage("expired"));
            if (i % 5000 == 0) {
               session.commit();
               System.out.println("Sent " + i + " + messages");
            }

         }

         session.commit();

         Thread.sleep(5000);
         producer.setTimeToLive(0);
         for (int i = 0; i < 500; i++) {
            producer.send(session.createTextMessage("ok"));

         }
         session.commit();

         MessageConsumer consumer = session.createConsumer(queue);
         connection.start();


         for (int i = 0; i < 500; i++) {
            TextMessage txt = (TextMessage) consumer.receive(10000);
            Assert.assertNotNull(txt);
            Assert.assertEquals("ok", txt.getText());
         }

         session.commit();
      } catch (Exception e) {
         if (connection != null) {
            connection.close();
         }
      }
   }

   @Override
   public String getLiveBrokerConfig() {
      return "/servers/expire/broker.xml";
   }
}
