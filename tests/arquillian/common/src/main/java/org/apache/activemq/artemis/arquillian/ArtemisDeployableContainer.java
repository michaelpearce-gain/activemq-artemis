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
package org.apache.activemq.artemis.arquillian;

import java.io.File;

/**
 * This class should be implemented by any container that extends the DeployableContainer Arquillian interface
 * */
public interface ArtemisDeployableContainer {

   /*
   * Create a broker with the given broker.xml that will be used to override any entries in the created broker
   * */
   void createBroker(File configuration);

   /*
   * Start the broker
   * */
   void startBroker();

   /*
   * Kill the broker process
   * */
   void kill();

   /*
   * Stop the broker gracefully
   * */
   void stopBroker(boolean wait);

   /*
   * return the url for the core client
   * */
   String getCoreConnectUrl();

   /*
   * return the hostname or ip of the broker
   * */
   String getConnectHost();

   /*
   * return the port for the specific protocol.
   * */
   String getConnectPort(String protocol);
}
