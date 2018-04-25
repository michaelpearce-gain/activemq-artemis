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


import org.apache.activemq.artemis.tests.smoke.categories.Replicated6Node;
import org.apache.activemq.artemis.tests.smoke.common.Replicated6NodeTestBase;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@Category(Replicated6Node.class)
public class Replicated6NodeFailbackTest extends Replicated6NodeTestBase {

   @Test
   @RunAsClient
   public void checkFailbackWithKill() throws Exception {
      for (int i = 0; i < 10; i++) {
         //Thread.sleep(5000);
         controller.kill(LIVE_1);


         awaitBrokerStart(REPLICA_1);

         System.out.println("***************************** restarting live1 node *********************************");

         controller.start(LIVE_1);

         System.out.println("***************************** restarted live1 node *********************************");

         awaitBrokerStart(LIVE_1);

         checkLivesAndBackups(LIVE_1, 30000, 3, 3);
        // Thread.sleep(5000);
         controller.kill(LIVE_2);

         System.out.println("***************************** restarting live2 node *********************************");

         awaitBrokerStart(REPLICA_2);

         controller.start(LIVE_2);

         System.out.println("***************************** restarted live2 node *********************************");

         awaitBrokerStart(LIVE_2);

         checkLivesAndBackups(LIVE_2, 30000, 3, 3);
        // Thread.sleep(5000);
         controller.kill(LIVE_3);

         System.out.println("***************************** restarting live3 node *********************************");

         awaitBrokerStart(REPLICA_3);

         controller.start(LIVE_3);

         System.out.println("***************************** restarted live3 node *********************************");

         awaitBrokerStart(LIVE_3);

         checkLivesAndBackups(LIVE_3, 30000, 3, 3);
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
