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

import org.apache.activemq.artemis.arquillian.ArtemisDeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ArtemisRemoteDeployableContainer implements DeployableContainer<ArtemisRemoteContainerConfiguration>, ArtemisDeployableContainer {

   private ArtemisRemoteContainerConfiguration configuration;
   private String baseURL;

   @Override
   public void createBroker(File configuration) {
      String xml = "";
      if (configuration != null) {
         try {
            xml = readFile(configuration.getAbsolutePath(), Charset.defaultCharset());
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
      String[] urlParameters = new String[4];
      urlParameters[0] = "artemisCreateCommand";
      urlParameters[1] = this.configuration.getArtemisCreateCommand();
      urlParameters[2] = "configuration";
      urlParameters[3] = xml;

      execute("create", urlParameters);
   }


   @Override
   public void startBroker() {
      String[] urlParameters = new String[0];
      execute("start", urlParameters);
   }

   @Override
   public void stopBroker(boolean wait) {
      String[] urlParameters = new String[2];
      urlParameters[0] = "wait";
      urlParameters[1] = wait ? "true" : "false";
      execute("stop", urlParameters);
   }

   @Override
   public String getCoreConnectUrl() {
      return "tcp://" + configuration.getHost() + ":" + configuration.getCorePort();
   }

   @Override
   public String getConnectHost() {
      return configuration.getHost();
   }

   @Override
   public String getConnectPort(String protocol) {
      if ("amqp".equals(protocol)) {
         return configuration.getAmqpPort();
      } else if ("mqtt".equals(protocol)) {
         return configuration.getMqttPort();
      }
      return configuration.getCorePort();
   }

   @Override
   public void kill() {
      execute("kill", null);
   }



   @Override
   public Class<ArtemisRemoteContainerConfiguration> getConfigurationClass() {
      return ArtemisRemoteContainerConfiguration.class;
   }

   @Override
   public void setup(ArtemisRemoteContainerConfiguration artemisRemoteContainerConfiguration) {
      this.configuration = artemisRemoteContainerConfiguration;
   }

   @Override
   public void start() throws LifecycleException {

   }

   @Override
   public void stop() throws LifecycleException {

   }

   @Override
   public ProtocolDescription getDefaultProtocol() {
      return new ProtocolDescription("artemis remote");
   }

   @Override
   public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
      return null;
   }

   @Override
   public void undeploy(Archive<?> archive) throws DeploymentException {

   }

   @Override
   public void deploy(Descriptor descriptor) throws DeploymentException {

   }

   @Override
   public void undeploy(Descriptor descriptor) throws DeploymentException {

   }

   private void execute(String target, String[] urlParameters) {
      try {

         URI baseUri = getURL(target);
         URI uri = applyParameters(baseUri, urlParameters);
         System.out.println("uri = " + uri);
         HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
         conn.setRequestMethod("GET");

         if (conn.getResponseCode() != 200) {
            BufferedReader br = new BufferedReader(new InputStreamReader(
                  (conn.getInputStream())));

            String output;
            System.out.println("Output from Server .... \n");
            while ((output = br.readLine()) != null) {
               System.out.println(output);
            }
            throw new RuntimeException("Failed : HTTP error code : "
                  + conn.getResponseCode());
         }
         conn.disconnect();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   private URI getURL(String target) throws  URISyntaxException {
      if (baseURL == null) {
         String host = configuration.getBootstrapHost();
         String port = configuration.getBootstrapPort();
         baseURL = "http://" + host + ":" + port + "/artemis/";
      }
      return new URI(baseURL + target);
   }

   static String readFile(String path, Charset encoding)
         throws IOException {
      byte[] encoded = Files.readAllBytes(Paths.get(path));
      return new String(encoded, encoding);
   }

   private URI applyParameters(URI baseUri, String[] urlParameters) {
      if (urlParameters == null) {
         return baseUri;
      }
      StringBuilder query = new StringBuilder();
      boolean first = true;
      for (int i = 0; i < urlParameters.length; i += 2) {
         if (first) {
            first = false;
         } else {
            query.append("&");
         }
         try {
            query.append(urlParameters[i]).append("=")
                  .append(URLEncoder.encode(urlParameters[i + 1], "UTF-8"));
         } catch (UnsupportedEncodingException ex) {
            /* As URLEncoder are always correct, this exception
             * should never be thrown. */
            throw new RuntimeException(ex);
         }
      }
      try {
         return new URI(baseUri.getScheme(), baseUri.getAuthority(),
               baseUri.getPath(), query.toString(), null);
      } catch (URISyntaxException ex) {
         /* As baseUri and query are correct, this exception
          * should never be thrown. */
         throw new RuntimeException(ex);
      }
   }

   public static void main(String[] args) {
      ArtemisRemoteDeployableContainer container = new ArtemisRemoteDeployableContainer();
      ArtemisRemoteContainerConfiguration configuration = new ArtemisRemoteContainerConfiguration();
      configuration.setBootstrapHost("localhost");
      configuration.setBootstrapPort("8080");
      configuration.setArtemisCreateCommand("--allow-anonymous --user admin --password password");
      container.setup(configuration);
      File file = new File("./tests/smoke-tests/src/test/resources/servers/replicated/broker.xml");
      container.createBroker(file);
      container.startBroker();
      container.stopBroker(true);
   }
}
