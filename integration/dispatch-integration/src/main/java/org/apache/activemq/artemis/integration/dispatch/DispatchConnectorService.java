/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.integration.dispatch;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.Interceptor;
import org.apache.activemq.artemis.core.postoffice.PostOffice;
import org.apache.activemq.artemis.core.postoffice.impl.PostOfficeImpl;
import org.apache.activemq.artemis.core.protocol.core.Packet;
import org.apache.activemq.artemis.core.protocol.core.impl.wireformat.MessagePacket;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.ConnectorService;
import org.apache.activemq.artemis.core.server.management.ManagementService;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.activemq.artemis.utils.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DispatchConnectorService implements ConnectorService
{
   private final String url;
   private final String linkPrefix;
   private final String connectorAddress;
   private final String connectorPort;
   private final PostOffice postOffice;
   private final ScheduledExecutorService scheduledThreadPool;
   private final String qos;
   private DispatchManagementImpl dispatchManagement;
   private boolean isStarted = false;

   public DispatchConnectorService(Map<String, Object> configuration, PostOffice postOffice, ScheduledExecutorService scheduledThreadPool)
   {
      this.postOffice = postOffice;
      this.scheduledThreadPool = scheduledThreadPool;
      url = (String) configuration.get(DispatchConstants.URL);
      linkPrefix = (String) configuration.get(DispatchConstants.LINK_PREFIX);
      connectorAddress = (String) configuration.get(DispatchConstants.CONNECTOR_ADDRESS);
      connectorPort = (String) configuration.get(DispatchConstants.CONNECTOR_PORT);
      qos = (String) configuration.get(DispatchConstants.QOS);
      dispatchManagement = new DispatchManagementImpl(url, postOffice, qos);
      final ActiveMQServer server = ((PostOfficeImpl) postOffice).getServer();
      server.getRemotingService().addOutgoingInterceptor(new Interceptor()
      {
         @Override
         public boolean intercept(Packet packet, RemotingConnection connection) throws ActiveMQException
         {
            if (packet instanceof MessagePacket)
            {
               ((MessagePacket) packet).getMessage().putStringProperty("BROKER_PASS_THRU", server.getNodeID().toString());
               System.out.println("DispatchConnectorService.intercept********************************************************************");
            }
            return true;
         }
      });

      server.getRemotingService().addIncomingInterceptor(new Interceptor()
      {
         @Override
         public boolean intercept(Packet packet, RemotingConnection connection) throws ActiveMQException
         {
            if (packet instanceof MessagePacket)
            {
               ((MessagePacket) packet).getMessage().putStringProperty("BROKER_PASS_THRU", server.getNodeID().toString());
               System.out.println("DispatchConnectorService.intercept********************************************************************");
            }
            return true;
         }
      });
   }

   @Override
   public String getName()
   {
      return "dispatch";
   }

   @Override
   public void start() throws Exception
   {
      if (isStarted)
      {
         return;
      }
      System.out.println("connecting to " + url);
      ManagementService managementService = ((PostOfficeImpl) postOffice).getServer().getManagementService();
      managementService.registerInRegistry("dispatch.management", dispatchManagement);

      scheduledThreadPool.schedule(new Runnable()
      {
         @Override
         public void run()
         {
            setup();
         }
      }, 1000, TimeUnit.MILLISECONDS);
      isStarted = true;
   }

   private void setup()
   {
      try
      {
         if (!dispatchManagement.connectorExists())
         {
            JSONObject jsonObject = dispatchManagement.createConnector(connectorAddress, connectorPort);
            System.out.println(jsonObject);
         }

         if (linkPrefix != null)
         {
            if (!dispatchManagement.linkExists(linkPrefix))
            {
               JSONObject jsonObject = dispatchManagement.createLinkAddress(linkPrefix);
               System.out.println(jsonObject);
            }
         }
      }
      catch (Exception e)
      {
         scheduledThreadPool.schedule(new Runnable()
         {
            @Override
            public void run()
            {
               setup();
            }
         }, 1000, TimeUnit.MILLISECONDS);
      }
   }


   @Override
   public void stop() throws Exception
   {
      isStarted = false;
   }

   @Override
   public boolean isStarted()
   {
      return false;
   }
}
