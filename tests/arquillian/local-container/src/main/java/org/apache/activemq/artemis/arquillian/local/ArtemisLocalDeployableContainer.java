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
package org.apache.activemq.artemis.arquillian.local;

import org.apache.activemq.artemis.arquillian.ArtemisDeployableContainer;
import org.apache.activemq.artemis.broker.BrokerProcess;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ArtemisLocalDeployableContainer implements DeployableContainer<ArtemisContainerConfiguration>, ArtemisDeployableContainer {

   private BrokerProcess brokerProcess;

   private ArtemisContainerConfiguration containerConfiguration;

   private String coreConnectUrl;

   private String connectHost;

   private Map<String, String> connectPorts = new HashMap<>();

   public ArtemisLocalDeployableContainer() {
      super();
   }

   @Override
   public Class<ArtemisContainerConfiguration> getConfigurationClass() {
      return ArtemisContainerConfiguration.class;
   }

   @Override
   public void setup(ArtemisContainerConfiguration containerConfiguration) {
      this.containerConfiguration = containerConfiguration;
   }

   @Override
   public void start() throws LifecycleException {
      brokerProcess = new BrokerProcess(containerConfiguration.getArtemisHome(), containerConfiguration.getArtemisInstance());
   }

   @Override
   public void stop() throws LifecycleException {
      brokerProcess.stop();
      System.out.println("****************** Destroying Broker Process ********************");
   }

   @Override
   public ProtocolDescription getDefaultProtocol() {
      return new ProtocolDescription("artemis local");
   }

   @Override
   public void kill() {
      try {
         brokerProcess.kill();
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void stopBroker(boolean wait) {
      brokerProcess.stopBroker(wait);
   }

   @Override
   public void createBroker(File configuration) {
      brokerProcess.createBroker(configuration, containerConfiguration.getArtemisCreateCommand());
   }

   @Override
   public void startBroker() {
      brokerProcess.startBroker();
   }

   @Override
   public String getCoreConnectUrl() {
      if (coreConnectUrl == null) {
         coreConnectUrl = brokerProcess.getCoreConnectURL();
      }
      return coreConnectUrl;
   }

   @Override
   public String getConnectHost() {
      if (connectHost == null) {
         connectHost = brokerProcess.getConnectHost();
      }
      return connectHost;
   }

   @Override
   public String getConnectPort(String protocol) {
      if (connectPorts.get(protocol) == null) {
         connectPorts.put(protocol, brokerProcess.getConnectPort(protocol));
      }
      return connectPorts.get(protocol);
   }

   @Override
   public void deploy(Descriptor descriptor) throws DeploymentException {
   }

   @Override
   public void undeploy(Descriptor descriptor) throws DeploymentException {
   }

   @Override
   public void undeploy(Archive archive) throws DeploymentException {
   }

   @Override
   public ProtocolMetaData deploy(Archive archive) throws DeploymentException {
      return null;
   }
}
