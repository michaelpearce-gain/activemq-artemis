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
package org.apache.activemq.artemis;

import org.apache.activemq.artemis.broker.BrokerProcess;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.PrintWriter;

public class ArtemisBootstrapService {
   BrokerProcess brokerProcess;

   Logger logger = Logger.getLogger(ArtemisBootstrapService.class);

   public ArtemisBootstrapService() {
      String artemisHome = System.getProperty("ARTEMIS_HOME");
      if (artemisHome == null) {
         logger.error("Please set ARTEMIS_HOME to point to a valid Artemis instance");
      }
      brokerProcess = new BrokerProcess(artemisHome, "broker");
   }

   public void create(String configuration, String artemisCreateCommand) throws Exception {
      File file = null;
      if (configuration != null && configuration.length() > 0) {
         file = new File("broker.xml");
         file.createNewFile();
         try (PrintWriter out = new PrintWriter("broker.xml")) {
            out.println(configuration);
         }
      }
      brokerProcess.createBroker(file, artemisCreateCommand);
   }

   public void start() throws Exception {
      brokerProcess.startBroker();
   }

   public void stop(boolean wait) {
      brokerProcess.stopBroker(wait);
/*      File absoluteHome = new File(artemisHome);
      try {
         broker = ProcessBuilder.build("artemis standalone", absoluteHome, false, "stop");
      } catch (Exception e) {
         throw new IllegalStateException("unable to start broker", e);
      }*/
   }

   public void kill() {
      try {
         brokerProcess.kill();
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }
}
