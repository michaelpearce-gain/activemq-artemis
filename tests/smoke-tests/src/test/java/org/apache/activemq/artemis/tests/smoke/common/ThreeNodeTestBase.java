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

public abstract class ThreeNodeTestBase extends ReplicatedTestBase {

   public static final String LIVE_1 = "live1";

   public static final String LIVE_2 = "live2";

   public static final String LIVE_3 = "live3";

   protected ConnectionFactory live1ConnectionFactory;

   protected ConnectionFactory live2ConnectionFactory;

   protected ConnectionFactory live3ConnectionFactory;

   @Before
   public void startBrokers() throws Exception {
      controller.create(LIVE_1, getLiveBrokerFile(getLiveBrokerConfig()));

      BrokerFuture live1 = controller.start(LIVE_1);

      live1.awaitBrokerStart(60000);

      checkLivesAndBackups(LIVE_1, 60000, 1, 0);

      controller.create(LIVE_2, getLiveBrokerFile(getLiveBrokerConfig()));

      BrokerFuture live2 = controller.start(LIVE_2);

      live2.awaitBrokerStart(60000);

      checkLivesAndBackups(LIVE_1, 60000, 2, 0);

      controller.create(LIVE_3, getLiveBrokerFile(getLiveBrokerConfig()));

      BrokerFuture live3 = controller.start(LIVE_3);

      live3.awaitBrokerStart(60000);

      checkLivesAndBackups(LIVE_1, 60000, 3, 0);

      live1ConnectionFactory = createConnectionFactory(LIVE_1, getProtocol(), true);
      live2ConnectionFactory = createConnectionFactory(LIVE_2, getProtocol(), true);
      live3ConnectionFactory = createConnectionFactory(LIVE_3, getProtocol(), true);
   }

   @After
   public void stopBrokers() {
      controller.kill(LIVE_1);
      controller.kill(LIVE_2);
      controller.kill(LIVE_3);
   }
}
