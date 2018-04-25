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
package org.apache.activemq.artemis.arquillian.remote;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

public class ArtemisRemoteContainerConfiguration implements ContainerConfiguration {
   private String host;

   private String corePort;

   private String amqpPort;

   private String mqttPort;

   private String bootstrapHost;

   private String bootstrapPort;

   private String artemisCreateCommand;

   @Override
   public void validate() throws ConfigurationException {

   }

   public String getHost() {
      return host;
   }

   public void setHost(String host) {
      this.host = host;
   }

   public String getCorePort() {
      return corePort;
   }

   public void setCorePort(String corePort) {
      this.corePort = corePort;
   }

   public String getAmqpPort() {
      return amqpPort;
   }

   public void setAmqpPort(String amqpPort) {
      this.amqpPort = amqpPort;
   }

   public String getMqttPort() {
      return mqttPort;
   }

   public void setMqttPort(String mqttPort) {
      this.mqttPort = mqttPort;
   }

   public String getBootstrapHost() {
      return bootstrapHost;
   }

   public void setBootstrapHost(String bootStrapHost) {
      this.bootstrapHost = bootStrapHost;
   }

   public String getBootstrapPort() {
      return bootstrapPort;
   }

   public void setBootstrapPort(String bootStrapPort) {
      this.bootstrapPort = bootStrapPort;
   }

   public String getArtemisCreateCommand() {
      return artemisCreateCommand;
   }

   public void setArtemisCreateCommand(String artemisCreateCommand) {
      this.artemisCreateCommand = artemisCreateCommand;
   }
}
