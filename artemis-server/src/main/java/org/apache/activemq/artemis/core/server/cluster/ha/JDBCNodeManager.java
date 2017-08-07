/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.core.server.cluster.ha;

import org.apache.activemq.artemis.api.core.ActiveMQIllegalStateException;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.config.storage.DatabaseStorageConfiguration;
import org.apache.activemq.artemis.core.server.ActivateCallback;
import org.apache.activemq.artemis.core.server.ActiveMQServerLogger;
import org.apache.activemq.artemis.core.server.NodeManager;
import org.apache.activemq.artemis.core.server.impl.ActiveMQServerImpl;
import org.apache.activemq.artemis.jdbc.store.drivers.JDBCUtils;
import org.apache.activemq.artemis.jdbc.store.sql.SQLProvider;
import org.apache.activemq.artemis.utils.UUID;
import org.apache.activemq.artemis.utils.UUIDGenerator;

import java.io.IOException;
import java.sql.SQLException;

public class JDBCNodeManager extends NodeManager {
   protected final ActiveMQServerImpl activeMQServer;
   protected final DatabaseStorageConfiguration storeConfiguration;
   //make this configurable
   private long lockAcquireSleepInterval = 1000;
   NodeManagerJDBCDriver jdbcDriver;

    protected boolean createTablesOnStartup;
    protected int queryTimeout = -1;
   protected SQLProvider sqlProvider;
   private UUID uuid;

   public JDBCNodeManager(ActiveMQServerImpl activeMQServer, DatabaseStorageConfiguration storeConfiguration) {
      super(false, null);
      this.activeMQServer = activeMQServer;
      this.storeConfiguration = storeConfiguration;
      storeConfiguration.setSqlProvider(JDBCUtils.getSQLProviderFactory(storeConfiguration.getJdbcConnectionUrl()));
      sqlProvider = storeConfiguration.getSqlProviderFactory().create(storeConfiguration.getNodeManagerTableName(), SQLProvider.DatabaseStoreType.NODE_MANAGER);
      jdbcDriver = new NodeManagerJDBCDriver(sqlProvider,storeConfiguration.getJdbcConnectionUrl(), storeConfiguration.getJdbcDriverClassName(), queryTimeout);
   }

   public void configure(SQLProvider sqlProvider) throws IOException {
        this.sqlProvider = sqlProvider;
    }


    public int getQueryTimeout() {
        return queryTimeout;
    }

    public void setQueryTimeout(int queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    public void setCreateTablesOnStartup(boolean createTablesOnStartup) {
        this.createTablesOnStartup = createTablesOnStartup;
    }

    @Override
    public void start() throws SQLException {
      this.uuid = UUIDGenerator.getInstance().generateUUID();
      setUUID(uuid);
      jdbcDriver.start();
        /*String lockCreateStatementSQL = sqlProvider.getLockCreateStatementSQL();

        Connection connection = null;
         Statement statement = null;
         try {
             connection = getConnection();
             statement = connection.createStatement();
             setQueryTimeout(statement);

             ActiveMQJournalLogger.LOGGER.debug("Executing SQL: " + lockCreateStatementSQL);
              try {
                  statement.execute(lockCreateStatementSQL);
              } catch (SQLException e) {
                 ActiveMQJournalLogger.LOGGER.info("Could not create lock tables; they could already exist." + " Failure was: "
                          + lockCreateStatementSQL + " Message: " + e.getMessage() + " SQLState: " + e.getSQLState()
                          + " Vendor code: " + e.getErrorCode());
              }
         } catch (SQLException e) {
            ActiveMQJournalLogger.LOGGER.warn("Could not create lock tables; Failure Message: " + e.getMessage() + " SQLState: " + e.getSQLState()
                     + " Vendor code: " + e.getErrorCode(), e);
         } finally {
             close(statement);
             close(connection);
         }*/
    }

   @Override
   public void awaitLiveNode() throws Exception {
      jdbcDriver.awaitLiveNode(lockAcquireSleepInterval);
   }

   @Override
   public void awaitLiveStatus() throws Exception {

   }

   @Override
   public void startBackup() throws Exception {

   }

   @Override
   public ActivateCallback startLiveNode() throws Exception {
      //todo add state to db as failing back
      /*state = FAILING_BACK;
      liveLock.acquire();*/
      return new ActivateCallback() {
         @Override
         public void preActivate() {
         }

         @Override
         public void activated() {
         }

         @Override
         public void deActivate() {
         }

         @Override
         public void activationComplete() {
            try {
               //todo add state to db as live
            } catch (Exception e) {
               ActiveMQServerLogger.LOGGER.warn(e.getMessage(), e);
            }
         }
      };
   }

   @Override
   public void pauseLiveServer() throws Exception {

   }

   @Override
   public void crashLiveServer() throws Exception {

   }

   @Override
   public void releaseBackup() throws Exception {

   }

   @Override
   public SimpleString readNodeId() throws ActiveMQIllegalStateException, IOException {
      return new SimpleString(uuid.toString());
   }

   @Override
   public boolean isAwaitingFailback() throws Exception {
      return false;
   }

   @Override
   public boolean isBackupLive() throws Exception {
      return false;
   }

   @Override
   public void interrupt() {

   }
}
