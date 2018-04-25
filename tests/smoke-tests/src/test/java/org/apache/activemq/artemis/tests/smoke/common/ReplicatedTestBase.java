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

import org.apache.activemq.artemis.api.core.JsonUtil;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.arquillian.ArtemisContainerController;
import org.apache.activemq.artemis.core.client.impl.ServerLocatorInternal;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.io.File;
import java.net.URL;
import java.util.Calendar;

public abstract class ReplicatedTestBase extends SmokeTestBase {

   @ArquillianResource
   protected ArtemisContainerController controller;

   @Override
   public abstract String getLiveBrokerConfig();

   public abstract String getBackupBrokerConfig();

   public abstract PROTOCOL getProtocol();

   protected void awaitBrokerStart(String container) {
      try {
         String broker = controller.getCoreConnectUrl(container);
         broker += "?retryInterval=1000&retryIntervalMultiplier=1.0&initialConnectAttempts=120";
         ServerLocatorInternal serverLocator1 = (ServerLocatorInternal) ActiveMQClient.createServerLocator(broker);
         serverLocator1.connect();
      } catch (Exception e) {
         Assert.fail("Unable to start broker " + container + " " + e.getMessage());
      }
   }


   protected void checkLivesAndBackups(String container, int timeout, int lives, int backups) throws InterruptedException {
      long start = System.currentTimeMillis();
      Calendar instance = Calendar.getInstance();
      instance.setTimeInMillis(start);
      System.out.println("instance = " + instance.getTime());
      String reply = null;
      do {
         try  {
            reply = controller.managementRequest(container, "broker", "listNetworkTopology");

            if (reply == null) {
               Thread.sleep(10);
            } else {
               JsonArray array = JsonUtil.readJsonArray(reply);
               int currentLiveNodes = 0;
               int currentBackupNodes = 0;

               for (JsonValue anArray : array) {
                  JsonObject jsonObject = (JsonObject) anArray;
                  boolean live = jsonObject.containsKey("live");
                  if (live) {
                     currentLiveNodes++;
                  }
                  boolean backup = jsonObject.containsKey("backup");
                  if (backup) {
                     currentBackupNodes++;
                  }
               }
               if (currentLiveNodes == lives && currentBackupNodes == backups) {
                  System.out.println("correct topology = " + reply);
                  return;
               }
            }
         } catch (Throwable t) {
            Thread.sleep(10);
         }
      }
      while (System.currentTimeMillis() - start < timeout);
      instance.setTimeInMillis(System.currentTimeMillis());
      System.out.println("instance = " + instance.getTime());
      Assert.fail("topology incorrect: " + reply);
   }

   protected File getBackupBrokerFile(String config) {
      if (config != null) {
         URL targetUrl = getClass().getResource(config);
         return new File(targetUrl.getFile());
      }
      return null;
   }
}
