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
package org.apache.activemq.artemis.tests.integration.amqp;

import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.cluster.impl.MessageLoadBalancingType;
import org.apache.activemq.artemis.protocol.amqp.broker.ProtonProtocolManagerFactory;
import org.apache.activemq.transport.amqp.client.AmqpClient;
import org.apache.activemq.transport.amqp.client.AmqpConnection;
import org.apache.activemq.transport.amqp.client.AmqpMessage;
import org.apache.activemq.transport.amqp.client.AmqpSender;
import org.apache.activemq.transport.amqp.client.AmqpSession;
import org.junit.Test;

import java.util.List;

public class AmqpClusteredTest extends AmqpClusteredTestSupport {
   @Override
   public void setUp() throws Exception {
      super.setUp();
   }

   @Test
   public void loadBalanceTest() throws Exception {

      String address = "test.address";
      String queueNamePrefix = "test.queue";
      String clusterAddress = "test";

      setupServer(0, true, true);
      setupServer(1, true, true);
      servers[0].addProtocolManagerFactory(new ProtonProtocolManagerFactory());
      servers[1].addProtocolManagerFactory(new ProtonProtocolManagerFactory());

      setupClusterConnection("cluster0", clusterAddress, MessageLoadBalancingType.STRICT, 1, true, 0, 1);
      setupClusterConnection("cluster1", clusterAddress, MessageLoadBalancingType.STRICT, 1, true, 1, 0);

      startServers(0, 1);

      List<Queue> queues;
      for (int i = 0; i < 2; i++) {
         createAddressInfo(i, address, RoutingType.ANYCAST, -1, false);
         setupSessionFactory(i, true);
         createQueue(i, address, queueNamePrefix + i, null, false, RoutingType.ANYCAST);
         addConsumer(i, i, queueNamePrefix + i, null);
      }

      for (int i = 0; i < 2; i++) {
         waitForBindings(i, address, 1, 1, true);
         waitForBindings(i, address, 1, 1, false);
      }

      final int noMessages = 30;
      send(0, address, noMessages, true, null, null);

      AmqpClient amqpClient = createAmqpClient(0);
      AmqpConnection connect = addConnection(amqpClient.createConnection());
      connect.connect();
      AmqpSession session = connect.createSession();
      AmqpSender sender = session.createSender();
      for (int i = 0; i < 10; i++) {
         AmqpMessage message = new AmqpMessage();

         message.setMessageId("msg" + i);
         message.setText("Test-Message");
         message.setAddress(address);
         sender.send(message);
      }
   }
}
