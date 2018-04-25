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
package org.apache.activemq.artemis.broker;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.cli.process.ProcessBuilder;
import org.apache.activemq.artemis.configuration.XMLUpdater;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.deployers.impl.FileConfigurationParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class BrokerProcess {

   private final String artemisHome;

   private final String artemisInstance;

   private Process broker;

   private XMLUpdater xmlUpdater = new XMLUpdater();


   private Configuration configuration;

   public BrokerProcess(String artemisHome, String artemisInstance) {

      this.artemisHome = artemisHome;
      this.artemisInstance = artemisInstance;
   }

   public void stop() {
      if (broker != null) {
         broker.destroy();
      }
   }

   public void kill() throws InterruptedException {
      if (broker != null) {
         broker.destroyForcibly().waitFor();
      }
   }

   public void createBroker(File configuration, String artemisCreateCommand) {
      File artemisHome = new File(this.artemisHome);
      File instanceHome = new File(artemisInstance);
      try {
         Path dataDir = Paths.get(artemisInstance);

         if (dataDir.toFile().exists()) {
            Files.walkFileTree(dataDir, new SimpleFileVisitor<Path>() {
               @Override
               public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                  Files.delete(file);
                  return FileVisitResult.CONTINUE;
               }

               @Override
               public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                  Files.delete(dir);
                  return FileVisitResult.CONTINUE;
               }
            });
         }
         ArrayList<String> args = new ArrayList<>();
         args.add("create");
         String[] split = artemisCreateCommand.split(" ");
         for (String s : split) {
            args.add(s);
         }
         args.add(instanceHome.getAbsolutePath());

         String[] theArgs = new String[args.size()];
         Process build = ProcessBuilder.build(artemisInstance,
               artemisHome,
               false,
               args.toArray(theArgs));
         build.waitFor();
         initConfiguration();
         if (configuration != null) {
            File brokerXml = new File(instanceHome + "/etc/broker.xml");
            xmlUpdater.updateXml(configuration, brokerXml, brokerXml);
         }
      } catch (Exception e) {
         throw new IllegalStateException("unable to create broker", e);
      }
   }
   public void startBroker() {
      File instanceHome = new File(artemisInstance);
      try {
         broker = ProcessBuilder.build(artemisInstance, instanceHome, false, "run");
      } catch (Exception e) {
         throw new IllegalStateException("unable to start broker", e);
      }
   }

   public void stopBroker(boolean wait) {
      File instanceHome = new File(artemisInstance);
      try {
         ProcessBuilder.build(artemisInstance, instanceHome, false, "stop");
         if (wait) {
            this.broker.waitFor();
         }
      } catch (Exception e) {
         throw new IllegalStateException("unable to start broker", e);
      }
   }

   private void initConfiguration() throws Exception {
      if (configuration == null) {
         FileConfigurationParser parser = new FileConfigurationParser();
         configuration = parser.parseMainConfig(new FileInputStream(new File(artemisInstance + "/etc/broker.xml")));
      }
   }

   public String getCoreConnectURL() {
      String host = null;
      String port = null;
      Set<TransportConfiguration> acceptorConfigurations = configuration.getAcceptorConfigurations();
      for (TransportConfiguration acceptorConfiguration : acceptorConfigurations) {
         String factoryClassName = acceptorConfiguration.getFactoryClassName();
         if (factoryClassName.equals("org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory")) {
            Map<String, Object> params = acceptorConfiguration.getParams();
            for (Object key : params.keySet()) {
               if (key.equals("protocols")) {
                  String protocols = (String) params.get(key);
                  if (protocols.contains("CORE")) {
                     host = (String) params.get("host");
                     port = (String) params.get("port");
                     break;
                  }
               }
            }
         }
      }
      if (host == null || host.equals("0.0.0.0")) {
         host = "localhost";
      }
      if (port == null) {
         port = "61616";
      }
      return "tcp://" + host + ":" + port;
   }

   public String getConnectHost() {
      String host = null;
      Set<TransportConfiguration> acceptorConfigurations = configuration.getAcceptorConfigurations();
      for (TransportConfiguration acceptorConfiguration : acceptorConfigurations) {
         String factoryClassName = acceptorConfiguration.getFactoryClassName();
         if (factoryClassName.equals("org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory")) {
            Map<String, Object> params = acceptorConfiguration.getParams();
            for (Object key : params.keySet()) {
               if (key.equals("protocols")) {
                  String protocols = (String) params.get(key);
                  if (protocols.contains("CORE")) {
                     host = (String) params.get("host");
                     break;
                  }
               }
            }
         }
      }
      if (host == null || host.equals("0.0.0.0")) {
         host = "localhost";
      }

      return host;
   }

   public String getConnectPort(String protocol) {
      String port = null;
      System.out.println("BrokerProcess.getConnectPort " + protocol);
      Set<TransportConfiguration> acceptorConfigurations = configuration.getAcceptorConfigurations();
      for (TransportConfiguration acceptorConfiguration : acceptorConfigurations) {
         String factoryClassName = acceptorConfiguration.getFactoryClassName();
         if (factoryClassName.equals("org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory")) {
            Map<String, Object> params = acceptorConfiguration.getParams();
            for (Object key : params.keySet()) {
               if (key.equals("protocols")) {
                  String protocols = (String) params.get(key);
                  if (protocols.contains(protocol)) {
                     port = (String) params.get("port");
                     break;
                  }
               }
            }
         }
      }
      if (port == null) {
         port = "61616";
      }
      return port;
   }
}
