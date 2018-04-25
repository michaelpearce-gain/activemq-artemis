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
package org.apache.activemq.artemis.tests.smoke.common;

import org.apache.activemq.artemis.arquillian.BrokerFuture;
import org.junit.After;
import org.junit.Before;

import javax.jms.ConnectionFactory;

public abstract class Replicated2NodeTestBase extends ReplicatedTestBase {

   protected static final String LIVE = "live1";

   protected static final String REPLICA = "replica1";

   private ConnectionFactory liveConnectionFactory;

   private ConnectionFactory backupConnectionFactory;

   @Before
   public void before() throws Exception {
      startBrokers();
      liveConnectionFactory = createConnectionFactory(LIVE, getProtocol(), true);
      backupConnectionFactory = createConnectionFactory(REPLICA, getProtocol(), true);
   }

   protected void startBrokers() throws InterruptedException {
      controller.create(LIVE, getLiveBrokerFile(getLiveBrokerConfig()));
      BrokerFuture live1 = controller.start(LIVE);
      live1.awaitBrokerStart(60000);
      controller.create(REPLICA, getBackupBrokerFile(getBackupBrokerConfig()));
      controller.start(REPLICA);
      checkLivesAndBackups(LIVE, 60000, 1, 1);
   }

   @After
   public void stopBrokers() {
      controller.stop(REPLICA, true);
      controller.stop(LIVE, true);
   }

   /*protected void sendMessagesTobackup(int num) throws Exception {
      try (Connection connection = backupConnectionFactory.createConnection()) {
         Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE);
         MessageProducer producer = session.createProducer(session.createQueue("replicatedQueue"));
         for (int j = 0; j < num; j++) {
            producer.send(session.createTextMessage("message" + j));
         }
      }
   }

   protected void receiveMessagesFromBackup(int num) throws Exception {
      try (Connection connection = liveConnectionFactory.createConnection()) {
         Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE);
         MessageConsumer consumer = session.createConsumer(session.createQueue("replicatedQueue"));
         connection.start();
         for (int j = 0; j < num; j++) {
            Message message = consumer.receiveNoWait();
            Assert.assertNotNull(message);
            System.out.println("message = " + message);
         }
      }
   }*/

}
