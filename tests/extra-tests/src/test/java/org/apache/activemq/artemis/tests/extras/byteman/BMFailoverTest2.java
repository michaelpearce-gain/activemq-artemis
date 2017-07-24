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

import org.apache.activemq.artemis.api.core.ActiveMQTransactionOutcomeUnknownException;
import org.apache.activemq.artemis.api.core.ActiveMQTransactionRolledBackException;
import org.apache.activemq.artemis.api.core.ActiveMQUnBlockedException;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.client.ActiveMQClientMessageBundle;
import org.apache.activemq.artemis.core.client.impl.ClientMessageImpl;
import org.apache.activemq.artemis.core.client.impl.ClientSessionFactoryInternal;
import org.apache.activemq.artemis.core.client.impl.ClientSessionInternal;
import org.apache.activemq.artemis.core.postoffice.Binding;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.transaction.impl.XidImpl;
import org.apache.activemq.artemis.tests.integration.cluster.failover.FailoverTestBase;
import org.apache.activemq.artemis.tests.integration.cluster.util.TestableServer;
import org.apache.activemq.artemis.tests.util.RandomUtil;
import org.apache.activemq.artemis.utils.UUIDGenerator;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(BMUnitRunner.class)

@BMRules(
      rules = { @BMRule(
                     name = "trace broker wait",
                     targetClass = "org.apache.activemq.artemis.core.server.impl.InVMNodeManager",
                     targetMethod = "awaitLiveNode",
                     targetLocation = "EXIT",
                     action = "org.apache.activemq.artemis.tests.extras.byteman.BMFailoverTest2.waitUntilNotified()"),
            @BMRule(
            name = "trace clientsessionimpl commit",
            targetClass = "org.apache.activemq.artemis.core.server.impl.ServerSessionImpl",
            targetMethod = "commit",
            targetLocation = "ENTRY",
            action = "org.apache.activemq.artemis.tests.extras.byteman.BMFailoverTest2.serverToStop.getServer().fail(true)"),
            })
public class BMFailoverTest2 extends FailoverTestBase {

   private ServerLocator locator;
   private ClientSessionFactoryInternal sf;
   private ClientSessionFactoryInternal sf2;
   public static TestableServer serverToStop;

   @Before
   @Override
   public void setUp() throws Exception {
      super.setUp();
      stopped = false;
      locator = getServerLocator();
   }

   private static boolean stopped = false;

   static CountDownLatch latch = new CountDownLatch(1);

   public static void waitUntilNotified() {
      try {
         latch.await(30000, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }

   @Test
   public void testFailoverOnCommit3() throws Exception {
      serverToStop = liveServer;
      locator = getServerLocator().setFailoverOnInitialConnection(true);
      createSessionFactory(-1);
      ClientSession session = sf.createSession(true, true);
      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);

      ClientProducer producer = addClientProducer(session.createProducer(FailoverTestBase.ADDRESS));

      sendMessages(session, producer, 20);

      session.close();

      session = sf.createSession(false, false);
      session.start();
      ClientConsumer consumer = session.createConsumer(FailoverTestBase.ADDRESS);
      for (int i = 0; i < 10; i++) {
         ClientMessage receive = consumer.receive(5000);
         assertNotNull(receive);
      }
      try {
         session.commit();
         fail("should have thrown an exception");
      } catch (Exception e) {
         System.out.println("BMFailoverTest.testFailoverOnCommit3");
      }

      for (int i = 0; i < 10; i++) {
         ClientMessage receive = consumer.receive(5000);
         assertNotNull(receive);
      }
      latch.countDown();
      waitForBackup();
      try {
         session.commit();
         fail("should have thrown an exception");
      } catch (ActiveMQTransactionOutcomeUnknownException e) {
         System.out.println("BMFailoverTest.testFailoverOnCommit3");
      }
      for (int i = 0; i < 10; i++) {
         ClientMessage receive = consumer.receive(5000);
         assertNotNull(receive);
      }
      session.commit();
      ClientMessage receive = consumer.receive(5000);
      assertNotNull(receive);
   }



   @Override
   protected TransportConfiguration getAcceptorTransportConfiguration(final boolean live) {
      return getNettyAcceptorTransportConfiguration(live);
   }

   @Override
   protected TransportConfiguration getConnectorTransportConfiguration(final boolean live) {
      return getNettyConnectorTransportConfiguration(live);
   }

   private ClientSession createSessionAndQueue() throws Exception {
      ClientSession session = createSession(sf, false, false);

      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);
      return session;
   }

   private ClientSession createXASessionAndQueue() throws Exception {
      ClientSession session = addClientSession(sf.createSession(true, true, true));

      session.createQueue(FailoverTestBase.ADDRESS, FailoverTestBase.ADDRESS, null, true);
      return session;
   }

   protected ClientSession createSession(ClientSessionFactory sf1,
                                         boolean autoCommitSends,
                                         boolean autoCommitAcks) throws Exception {
      return addClientSession(sf1.createSession(autoCommitSends, autoCommitAcks));
   }

   protected ClientSession createSession(ClientSessionFactory sf1,
                                         boolean xa,
                                         boolean autoCommitSends,
                                         boolean autoCommitAcks) throws Exception {
      return addClientSession(sf1.createSession(xa, autoCommitSends, autoCommitAcks));
   }

   private void createSessionFactory() throws Exception {
      locator.setBlockOnNonDurableSend(true).setBlockOnDurableSend(true).setReconnectAttempts(15);

      sf = createSessionFactoryAndWaitForTopology(locator, 2);
   }

   private void createSessionFactory(int reconnectAttempts) throws Exception {
      locator.setBlockOnNonDurableSend(true).setBlockOnDurableSend(true).setReconnectAttempts(reconnectAttempts);

      sf = createSessionFactoryAndWaitForTopology(locator, 2);
   }

   private void createSessionFactory2() throws Exception {
      sf2 = createSessionFactoryAndWaitForTopology(locator, 2);
   }
}
