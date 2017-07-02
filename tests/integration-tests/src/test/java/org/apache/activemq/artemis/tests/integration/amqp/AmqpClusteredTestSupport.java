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

import org.apache.activemq.artemis.tests.integration.cluster.distribution.ClusterTestBase;
import org.apache.activemq.transport.amqp.client.AmqpClient;
import org.apache.activemq.transport.amqp.client.AmqpConnection;
import org.junit.After;

import java.net.URI;
import java.util.LinkedList;

public class AmqpClusteredTestSupport extends ClusterTestBase {

   protected static final int AMQP_PORT = 61616;

   protected LinkedList<AmqpConnection> connections = new LinkedList<>();

   protected AmqpConnection addConnection(AmqpConnection connection) {
      connections.add(connection);
      return connection;
   }

   @After
   @Override
   public void tearDown() throws Exception {
      for (AmqpConnection conn : connections) {
         try {
            conn.close();
         } catch (Throwable ignored) {
            ignored.printStackTrace();
         }
      }

      super.tearDown();
   }

   public String getAmqpConnectionURIOptions() {
      return "";
   }

   public URI getBrokerAmqpConnectionURI(int node) {
      try {
         int port = AMQP_PORT + node;
         String uri = "tcp://127.0.0.1:" + port;

         if (!getAmqpConnectionURIOptions().isEmpty()) {
            uri = uri + "?" + getAmqpConnectionURIOptions();
         }

         return new URI(uri);
      } catch (Exception e) {
         throw new RuntimeException();
      }
   }

   public AmqpClient createAmqpClient(int node) throws Exception {
      return createAmqpClient(getBrokerAmqpConnectionURI(node), null, null);
   }

   public AmqpClient createAmqpClient(URI brokerURI) throws Exception {
      return createAmqpClient(brokerURI, null, null);
   }

   public AmqpClient createAmqpClient(URI brokerURI, String username, String password) throws Exception {
      return new AmqpClient(brokerURI, username, password);
   }

}
