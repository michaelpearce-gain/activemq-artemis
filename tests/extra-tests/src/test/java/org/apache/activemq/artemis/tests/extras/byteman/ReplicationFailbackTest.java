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
package org.apache.activemq.artemis.tests.extras.byteman;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.HAPolicyConfiguration;
import org.apache.activemq.artemis.core.config.ha.ReplicaPolicyConfiguration;
import org.apache.activemq.artemis.core.config.ha.ReplicatedPolicyConfiguration;
import org.apache.activemq.artemis.core.protocol.core.impl.wireformat.ReplicationLiveIsStoppingMessage;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.tests.util.ActiveMQTestBase;
import org.apache.activemq.artemis.tests.util.ReplicatedBackupUtils;
import org.apache.activemq.artemis.tests.util.TransportConfigurationUtils;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BMUnitRunner.class)
public class ReplicationFailbackTest extends ActiveMQTestBase {

   private static final CountDownLatch ruleFired = new CountDownLatch(1);

   private static final CountDownLatch ruleFired2 = new CountDownLatch(1);
   private ActiveMQServer backupServer;
   private ActiveMQServer liveServer;

   /*
   * simple test to induce a potential race condition where the server's acceptors are active, but the server's
   * state != STARTED
   */
   @Test
   @BMRules(
      rules = {@BMRule(
         name = "pause live message",
         targetClass = "org.apache.activemq.artemis.core.server.impl.ActiveMQServerImpl",
         targetMethod = "freezeConnections",
         targetLocation = "AT EXIT",
         action = "org.apache.activemq.artemis.tests.extras.byteman.ReplicationFailbackTest.pause();"),
            @BMRule(
         name = "pause stopping",
         targetClass = "org.apache.activemq.artemis.core.server.impl.SharedNothingLiveActivation",
         targetMethod = "sendLiveIsStopping",
         targetLocation = "AT EXIT",
         action = "org.apache.activemq.artemis.tests.extras.byteman.ReplicationFailbackTest.pause2();")})
   public void testFailback() throws Exception {
      TransportConfiguration liveConnector = TransportConfigurationUtils.getNettyConnector(true, 0);
      TransportConfiguration liveAcceptor = TransportConfigurationUtils.getNettyAcceptor(true, 0);
      TransportConfiguration backupConnector = TransportConfigurationUtils.getNettyConnector(false, 0);
      TransportConfiguration backupAcceptor = TransportConfigurationUtils.getNettyAcceptor(false, 0);

      Configuration backupConfig = createDefaultInVMConfig().setBindingsDirectory(getBindingsDir(0, true)).setJournalDirectory(getJournalDir(0, true)).setPagingDirectory(getPageDir(0, true)).setLargeMessagesDirectory(getLargeMessagesDir(0, true));



      Configuration liveConfig = createDefaultInVMConfig();

      ReplicatedBackupUtils.configureReplicationPair(backupConfig, backupConnector, backupAcceptor, liveConfig, liveConnector, liveAcceptor);
      ReplicatedPolicyConfiguration policyConfiguration = (ReplicatedPolicyConfiguration) liveConfig.getHAPolicyConfiguration();

      policyConfiguration.setCheckForLiveServer(true);

      ReplicaPolicyConfiguration haPolicyConfiguration = (ReplicaPolicyConfiguration) backupConfig.getHAPolicyConfiguration();

      haPolicyConfiguration.setAllowFailBack(true);

      liveServer = createServer(liveConfig);

      backupServer = createServer(backupConfig);

      liveServer.start();

      backupServer.start();
      
      ActiveMQTestBase.waitForRemoteBackup(null, 30, true, backupServer);

      ServerLocator locator = ActiveMQClient.createServerLocator(true, liveConnector);

      ClientSessionFactory sessionFactory = locator.createSessionFactory();

      ClientSession session = sessionFactory.createSession();

      liveServer.stop();

      liveServer.start();

      ActiveMQTestBase.waitForComponent(liveServer, 30);

      ActiveMQTestBase.waitForRemoteBackupSynchronization(backupServer);

      System.out.println("ReplicationFailbackTest.testFailback");
   }

   public static void breakIt() {
      ruleFired.countDown();
      try {
         /* before the fix this sleep would put the "live" server into a state where the acceptors were started
          * but the server's state != STARTED which would cause the backup to fail to announce
          */
         Thread.sleep(2000);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }

   private  static boolean firstTime = false;

   public static void pause() {
         /*try {
            ruleFired.await(30000, TimeUnit.MILLISECONDS);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }*/
   }

   public static void pause2() {
      /*try {
         ruleFired2.await(30000, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }*/
   }

   protected ClientSession createSession(ClientSessionFactory sf1,
                                         boolean xa,
                                         boolean autoCommitSends,
                                         boolean autoCommitAcks) throws Exception {
      return addClientSession(sf1.createSession(xa, autoCommitSends, autoCommitAcks));
   }
}
