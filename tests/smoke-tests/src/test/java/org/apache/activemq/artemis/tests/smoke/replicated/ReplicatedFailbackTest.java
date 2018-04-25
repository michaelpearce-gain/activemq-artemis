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
package org.apache.activemq.artemis.tests.smoke.replicated;


import org.apache.activemq.artemis.tests.smoke.categories.Replicated2Node;
import org.apache.activemq.artemis.tests.smoke.common.Replicated2NodeTestBase;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@Category(Replicated2Node.class)
public class ReplicatedFailbackTest extends Replicated2NodeTestBase {

   @Test
   @RunAsClient
   public void checkFailbackWithStop() throws Exception {
      for (int i = 0; i < 10; i++) {
         controller.stop(LIVE, true);

         awaitBrokerStart(REPLICA);
         //sendMessagesTobackup(100);

         System.out.println("***************************** restarting live node *********************************");

         controller.start(LIVE);

         awaitBrokerStart(LIVE);

         checkLivesAndBackups(LIVE, 30000, 1, 1);

         //receiveMessagesFromBackup(100);

         System.out.println("***************************** restarted live node *********************************");
      }
   }

   @Test
   @RunAsClient
   public void checkFailbackWithKill() throws Exception {
      for (int i = 0; i < 10; i++) {
         controller.kill(LIVE);

         awaitBrokerStart(REPLICA);
         //sendMessagesTobackup(100);

         System.out.println("***************************** restarting live node *********************************");

         controller.start(LIVE);

         awaitBrokerStart(LIVE);

         checkLivesAndBackups(LIVE, 30000, 1, 1);

         //receiveMessagesFromBackup(100);

         System.out.println("***************************** restarted live node *********************************");
      }
   }

   @Override
   public PROTOCOL getProtocol() {
      return PROTOCOL.CORE;
   }

   @Override
   public String getLiveBrokerConfig() {
      return "/servers/replicated/broker-live-check.xml";
   }

   @Override
   public String getBackupBrokerConfig() {
      return "/servers/replicated/broker-allow-failback.xml";
   }

}
