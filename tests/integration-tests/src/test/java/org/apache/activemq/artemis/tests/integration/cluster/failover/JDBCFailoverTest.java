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
package org.apache.activemq.artemis.tests.integration.cluster.failover;


import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.config.impl.SecurityConfiguration;
import org.apache.activemq.artemis.core.config.impl.Validators;
import org.apache.activemq.artemis.core.config.storage.DatabaseStorageConfiguration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.NodeManager;
import org.apache.activemq.artemis.core.server.impl.ActiveMQServerImpl;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.spi.core.security.ActiveMQJAASSecurityManager;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager;
import org.apache.activemq.artemis.spi.core.security.jaas.InVMLoginModule;
import org.apache.activemq.artemis.tests.util.InVMNodeManagerServer;

import java.lang.management.ManagementFactory;
import java.sql.DriverManager;
import java.util.Map;

public class JDBCFailoverTest extends FailoverTest {
   @Override
   public void setUp() throws Exception {
      try {
         DriverManager.getConnection("jdbc:derby:;shutdown=true");
      } catch (Exception ignored) {
      }
      super.setUp();
   }

   @Override
   protected ConfigurationImpl createBasicConfig(int serverID) {
      ConfigurationImpl configuration = new ConfigurationImpl().setSecurityEnabled(false).setClusterPassword(CLUSTER_PASSWORD).setJournalDatasync(false);

      DatabaseStorageConfiguration dbStorageConfiguration = new DatabaseStorageConfiguration();
            dbStorageConfiguration.setJdbcConnectionUrl(getTestJDBCConnectionUrl());
            dbStorageConfiguration.setBindingsTableName("BINDINGS");
            dbStorageConfiguration.setMessageTableName("MESSAGE");
            dbStorageConfiguration.setLargeMessageTableName("LARGE_MESSAGE");
            dbStorageConfiguration.setPageStoreTableName("PAGE_STORE");
            dbStorageConfiguration.setJdbcDriverClassName(getJDBCClassName());
      configuration.setStoreConfiguration(dbStorageConfiguration);
      return configuration;
   }

   protected ActiveMQServer createInVMFailoverServer(final boolean realFiles,
                                                        final Configuration configuration,
                                                        final int pageSize,
                                                        final int maxAddressSize,
                                                        final Map<String, AddressSettings> settings,
                                                        NodeManager nodeManager,
                                                        final int id) {
        ActiveMQServer server;
        ActiveMQSecurityManager securityManager = new ActiveMQJAASSecurityManager(InVMLoginModule.class.getName(), new SecurityConfiguration());
        configuration.setPersistenceEnabled(realFiles);
        server = addServer(new ActiveMQServerImpl(configuration, ManagementFactory.getPlatformMBeanServer(), securityManager));

        try {
           server.setIdentity("Server " + id);

           for (Map.Entry<String, AddressSettings> setting : settings.entrySet()) {
              server.getAddressSettingsRepository().addMatch(setting.getKey(), setting.getValue());
           }

           AddressSettings defaultSetting = new AddressSettings();
           defaultSetting.setPageSizeBytes(pageSize);
           defaultSetting.setMaxSizeBytes(maxAddressSize);

           server.getAddressSettingsRepository().addMatch("#", defaultSetting);

           return server;
        } finally {
           addServer(server);
        }
     }
}
