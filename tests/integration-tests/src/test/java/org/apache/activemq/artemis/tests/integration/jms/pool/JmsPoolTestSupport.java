/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.tests.integration.jms.pool;

import java.util.Set;

import javax.jms.JMSException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;
import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.RoutingType;
import org.apache.activemq.artemis.tests.integration.client.AcknowledgeTest;
import org.apache.activemq.artemis.tests.integration.management.ManagementControlHelper;
import org.apache.activemq.artemis.tests.util.ActiveMQTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmsPoolTestSupport  extends ActiveMQTestBase {

   protected static final Logger LOG = LoggerFactory.getLogger(JmsPoolTestSupport.class);
   @Rule
   public TestName name = new TestName();
   protected ActiveMQServer server;

   @Before
   public void setUp() throws Exception {
      LOG.info("========== start " + getTestName() + " ==========");
      server = createServer(true);
   }

   @After
   public void tearDown() throws Exception {
      if (server != null) {
         try {
            server.stop();
            server = null;
         } catch (Exception ex) {
            LOG.warn("Suppress error on shutdown: {}", ex);
         }
      }

      super.tearDown();

      LOG.info("========== tearDown " + getTestName() + " ==========");
   }

   public String getTestName() {
      return name.getMethodName();
   }

   protected ActiveMQServerControl getProxyToBroker() throws MalformedObjectNameException, JMSException {
      return server.getActiveMQServerControl();
   }

   /*protected ConnectorViewMBean getProxyToConnectionView(String connectionType) throws Exception {
      ObjectName connectorQuery = new ObjectName(
            "org.apache.activemq:type=Broker,brokerName=" + server.getBrokerName() + ",connector=clientConnectors,connectorName=" + connectionType + "_/*//*");

      Set<ObjectName> results = server.getManagementContext().queryNames(connectorQuery, null);

      if (results == null || results.isEmpty() || results.size() > 1) {
         throw new Exception("Unable to find the exact Connector instance.");
      }

      ConnectorViewMBean proxy = (ConnectorViewMBean) server.getManagementContext()
            .newProxyInstance(results.iterator().next(), ConnectorViewMBean.class, true);
      return proxy;
   }*/

   protected QueueControl getProxyToQueue(String name) throws  JMSException {
      return (QueueControl) server.getManagementService().getResource("queue." + name);
   }

   /*protected QueueViewMBean getProxyToTopic(String name) throws MalformedObjectNameException, JMSException {
      return
   }*/
}