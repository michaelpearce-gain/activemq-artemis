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
package org.apache.activemq.artemis.core.server.cluster.ha;

import org.apache.activemq.artemis.jdbc.store.drivers.AbstractJDBCDriver;
import org.apache.activemq.artemis.jdbc.store.sql.SQLProvider;
import org.apache.activemq.artemis.journal.ActiveMQJournalLogger;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;

public class NodeManagerJDBCDriver extends AbstractJDBCDriver {
   private final int queryTimeout;
   protected volatile PreparedStatement lockCreateStatement;
   protected volatile PreparedStatement lockUpdateStatement;

   public NodeManagerJDBCDriver() {
      super();
      this.queryTimeout = 0;
   }

   public NodeManagerJDBCDriver(SQLProvider sqlProvider, String jdbcConnectionUrl, String jdbcDriverClass, int queryTimeout) {
      super(sqlProvider, jdbcConnectionUrl, jdbcDriverClass);
      this.queryTimeout = queryTimeout;
   }

   public NodeManagerJDBCDriver(DataSource dataSource, SQLProvider provider, int queryTimeout) {
      super(dataSource, provider);
      this.queryTimeout = queryTimeout;
   }

   @Override
   protected void prepareStatements() throws SQLException {
      lockCreateStatement = connection.prepareStatement(sqlProvider.getLockCreateStatementSQL());
      lockUpdateStatement = connection.prepareStatement(sqlProvider.getLockUpdateStatementSQL());
   }

   @Override
   protected void createSchema() throws SQLException {
      createTable(sqlProvider.getCreateLockTableSQL());
   }

   @Override
   public void stop() throws SQLException {
      try {
         if (lockCreateStatement != null) {
            lockCreateStatement.cancel();
         }
      } catch (SQLFeatureNotSupportedException e) {
         ActiveMQJournalLogger.LOGGER.warn("Failed to cancel locking query on dataSource", e);
      }
      try {
         if (lockUpdateStatement != null) {
            lockUpdateStatement.cancel();
         }
      } catch (SQLFeatureNotSupportedException e) {
         ActiveMQJournalLogger.LOGGER.warn("Failed to cancel locking query on dataSource", e);
      }
      super.stop();
   }

   public void awaitLiveNode(long lockAcquireSleepInterval) {
      while (true) {
         try {
            lockCreateStatement.execute();
            break;
         } catch (Exception e) {
            try {
               ActiveMQJournalLogger.LOGGER.debug("Lock failure: "+ e, e);
            } finally {
               // Let's make sure the database connection is properly
               // closed when an error occurs so that we're not leaking
               // connections
               if (null != connection) {
                  try {
                     connection.rollback();
                  } catch (SQLException e1) {
                     ActiveMQJournalLogger.LOGGER.debug("Caught exception during rollback on connection: " + e1, e1);
                  }
                  try {
                     connection.close();
                  } catch (SQLException e1) {
                     ActiveMQJournalLogger.LOGGER.debug("Caught exception while closing connection: " + e1, e1);
                  }

                  connection = null;
               }
            }
         } /*finally {
            if (null != lockCreateStatement) {
               try {
                  lockCreateStatement.close();
               } catch (SQLException e1) {
                  ActiveMQJournalLogger.LOGGER.debug("Caught while closing statement: " + e1, e1);
               }
               lockCreateStatement = null;
            }
         }*/

         ActiveMQJournalLogger.LOGGER.info("Failed to acquire lock.  Sleeping for " + lockAcquireSleepInterval + " milli(s) before trying again...");
         try {
            Thread.sleep(lockAcquireSleepInterval);
         } catch (InterruptedException ie) {
            ActiveMQJournalLogger.LOGGER.warn("Master lock retry sleep interrupted", ie);
         }
      }
   }

   public boolean keepAlive() throws IOException {
      boolean result = false;
      try {
         lockUpdateStatement.setLong(1, System.currentTimeMillis());
         setQueryTimeout(lockUpdateStatement);
         int rows = lockUpdateStatement.executeUpdate();
         if (rows == 1) {
            result=true;
         }
      } catch (Exception e) {
         ActiveMQJournalLogger.LOGGER.error("Failed to update database lock: " + e, e);
      } finally {
         if (lockUpdateStatement != null) {
            try {
               lockUpdateStatement.close();
            } catch (SQLException e) {
               ActiveMQJournalLogger.LOGGER.error("Failed to close statement",e);
            }
            lockUpdateStatement = null;
         }
      }
      return result;
   }

   protected void setQueryTimeout(Statement statement) throws SQLException {
      if (queryTimeout > 0) {
         statement.setQueryTimeout(queryTimeout);
      }
   }
}
