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

public abstract class Replicated6NodeTestBase extends ReplicatedTestBase {

   public static final String LIVE_1 = "live1";

   public static final String REPLICA_1 = "replica1";

   public static final String LIVE_2 = "live2";

   public static final String REPLICA_2 = "replica2";

   public static final String LIVE_3 = "live3";

   public static final String REPLICA_3 = "replica3";

   protected ConnectionFactory live1ConnectionFactory;

   protected ConnectionFactory backup1ConnectionFactory;

   protected ConnectionFactory live2ConnectionFactory;

   protected ConnectionFactory backup2ConnectionFactory;

   protected ConnectionFactory live3ConnectionFactory;

   protected ConnectionFactory backup3ConnectionFactory;

   @Before
   public void startBrokers() throws Exception {
      controller.create(LIVE_1, getLiveBrokerFile(getLiveBrokerConfig()));

      BrokerFuture live1 = controller.start(LIVE_1);

      live1.awaitBrokerStart(60000);

      controller.create(REPLICA_1, getBackupBrokerFile(getBackupBrokerConfig()));

      controller.start(REPLICA_1);

      checkLivesAndBackups(LIVE_1, 60000, 1, 1);

      controller.create(LIVE_2, getLiveBrokerFile(getLiveBrokerConfig()));

      BrokerFuture live2 = controller.start(LIVE_2);

      live2.awaitBrokerStart(60000);

      controller.create(REPLICA_2,  getBackupBrokerFile(getBackupBrokerConfig()));

      controller.start(REPLICA_2);

      checkLivesAndBackups(LIVE_1, 60000, 2, 2);

      controller.create(LIVE_3, getLiveBrokerFile(getLiveBrokerConfig()));

      BrokerFuture live3 = controller.start(LIVE_3);

      live3.awaitBrokerStart(60000);

      controller.create(REPLICA_3,  getBackupBrokerFile(getBackupBrokerConfig()));

      controller.start(REPLICA_3);

      checkLivesAndBackups(LIVE_1, 60000, 3, 3);

      live1ConnectionFactory = createConnectionFactory(LIVE_1, getProtocol(), true);
      backup1ConnectionFactory = createConnectionFactory(REPLICA_1, getProtocol(), true);
      live2ConnectionFactory = createConnectionFactory(LIVE_2, getProtocol(), true);
      backup2ConnectionFactory = createConnectionFactory(REPLICA_2, getProtocol(), true);
      live3ConnectionFactory = createConnectionFactory(LIVE_3, getProtocol(), true);
      backup3ConnectionFactory = createConnectionFactory(REPLICA_3, getProtocol(), true);
   }

   @After
   public void stopBrokers() {
      controller.kill(REPLICA_1);
      controller.kill(REPLICA_2);
      controller.kill(REPLICA_3);
      controller.kill(LIVE_1);
      controller.kill(LIVE_2);
      controller.kill(LIVE_3);
   }
}
