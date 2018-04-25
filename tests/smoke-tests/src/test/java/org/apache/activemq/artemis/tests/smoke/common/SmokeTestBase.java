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

package org.apache.activemq.artemis.tests.smoke.common;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.arquillian.ArtemisContainerController;
import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;
import org.apache.activemq.artemis.tests.util.ActiveMQTestBase;
import org.apache.activemq.artemis.util.ServerUtil;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.jboss.arquillian.test.api.ArquillianResource;

import javax.jms.ConnectionFactory;

public class SmokeTestBase extends ActiveMQTestBase {

   public enum PROTOCOL {
      AMQP,
      CORE
   }

   @ArquillianResource
   protected ArtemisContainerController controller;

   ArrayList<Process> processes = new ArrayList();

   public static final String basedir = System.getProperty("basedir");


   protected File getLiveBrokerFile(String config) {
      if (config != null) {
         URL targetUrl = getClass().getResource(config);
         return new File(targetUrl.getFile());
      }
      return null;
   }

   public String getLiveBrokerConfig() {
      return null;
   }

   protected ConnectionFactory createConnectionFactory(String containerQualifier, PROTOCOL protocol, boolean failover) throws Exception {
      String host = controller.getConnectHost(containerQualifier);
      String port = controller.getConnectPort(containerQualifier, protocol.toString());
      if (protocol == PROTOCOL.CORE) {
         return createCoreConnectionFactory(host, port, failover);
      } else {
         return createAMQPConnectionFactory(host, port, failover);
      }
   }

   private ConnectionFactory createAMQPConnectionFactory(String host, String port, boolean failover) throws Exception {
      if (failover) {
         return new JmsConnectionFactory("failover:(amqp://" + host + ":" + port + ")?failover.reconnectDelay=1000&failover.maxReconnectAttempts=30");
      } else {
         return new JmsConnectionFactory("amqp://" + host + ":" + port);
      }
   }

   private ConnectionFactory createCoreConnectionFactory(String host, String port, boolean failover) throws Exception {
      String url = "tcp://" + host + ":" + port;
      ActiveMQJMSConnectionFactory factory = (ActiveMQJMSConnectionFactory) ActiveMQJMSClient.createConnectionFactory(url, "cf");
      if (failover) {
         factory.setInitialConnectAttempts(30);
         factory.setRetryInterval(1000);
      }
      return factory;
   }

   public String getServerLocation(String serverName) {
      return basedir + "/target/" + serverName;
   }

   public void cleanupData(String serverName) {
      String location = getServerLocation(serverName);
      deleteDirectory(new File(location, "data"));
   }

   public void addProcess(Process process) {
      processes.add(process);
   }

   public Process startServer(String serverName, int portID, int timeout) throws Exception {
      Process process = ServerUtil.startServer(getServerLocation(serverName), serverName, portID, timeout);
      addProcess(process);
      return process;
   }

}
