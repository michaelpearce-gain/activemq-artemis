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

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.postoffice.PostOffice;
import org.apache.activemq.artemis.core.postoffice.impl.PostOfficeImpl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.utils.json.JSONArray;
import org.apache.activemq.artemis.utils.json.JSONException;
import org.apache.activemq.artemis.utils.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;


public class DispatchManagementImpl implements DispatchManagement
{
   private final String url;
   private final String qos;
   private final ActiveMQServer server;

   public DispatchManagementImpl(String url, PostOffice postOffice, String qos)
   {
      this.url = url;
      this.qos = qos;
      server = ((PostOfficeImpl) postOffice).getServer();
   }

   @Override
   public String getServiceInfo() throws Exception
   {
      JSONArray jsonArray = executeQuery("qdmanage query -b " + url + " --type waypoint");
      HashMap<String, Object> map = new HashMap<>();
      map.put("serverID", "artemis" + qos);
      map.put("QOS", qos);
      SimpleString managementAddress = server.getManagementService().getManagementAddress();
      map.put("managementAddress", managementAddress.toString());
      JSONObject server = new JSONObject(map);
      ArrayList<Object> collection = new ArrayList<>();
      collection.add(server);
      for (int i = 0; i < jsonArray.length(); i++)
      {
         JSONObject o = (JSONObject) jsonArray.get(i);
         if (("artemis" + qos).equals(o.getString("connector")))
         {
            collection.add(o.getString("address"));
         }
      }
      JSONArray array = new JSONArray(collection);
      return array.toString();
   }

   @Override
   public boolean connectorExists() throws Exception
   {
      JSONArray jsonArray = executeQuery("qdmanage query -b " + url + " --type connector");
      return exists(jsonArray, "name", "artemis" + qos);
   }

   @Override
   public boolean linkExists(String linkPrefix) throws Exception
   {
      JSONArray jsonArray = executeQuery("qdmanage query -b " + url + " --type linkRoutePattern");
      return exists(jsonArray, "name", linkPrefix);

   }

   @Override
   public JSONObject createLinkAddress(String address) throws Exception
   {
      StringBuilder sb = new StringBuilder();
      sb.append("qdmanage create type=linkRoutePattern prefix=")
            .append(address)
            .append(" connector=")
            .append("artemis" + qos)
            .append(" -b  ")
            .append(url);
      return executeCreate(sb.toString());
   }

   @Override
   public JSONObject createConnector(String connectorAddress, String connectorPort) throws Exception
   {
      StringBuilder sb = new StringBuilder();
      sb.append("qdmanage create type=connector name=")
            .append("artemis" + qos)
            .append(" role=on-demand addr=")
            .append(connectorAddress)
            .append(" saslMechanisms=ANONYMOUS port=")
            .append(connectorPort)
            .append(" -b ")
            .append(url);
      return executeCreate(sb.toString());
   }

   @Override
   public String createWaypoint(String address) throws Exception
   {
      String name = "DISPATCH." + address;
      server.createQueue(new SimpleString(address), new SimpleString(address), null, false, false);
      /*StringBuilder sb = new StringBuilder();
      sb.append("qdmanage create type=waypoint name=")
            .append(name)
            .append(" address=")
            .append(address)
            .append(" inPhase=0")
            .append(" outPhase=1")
            .append(" connector=")
            .append("artemis" + qos)
            .append(" -b ")
            .append(url);

      System.out.println(executeCreate(sb.toString()).toString());
      sb = new StringBuilder();
      sb.append("qdmanage create type=fixedAddress prefix=")
            .append(address)
            .append(" phase=1")
            .append(" fanout=single")
            .append(" bias=closest")
            .append(" -b ")
            .append(url);
      System.out.println(executeCreate(sb.toString()).toString());
      sb = new StringBuilder();
      sb.append("qdmanage create type=fixedAddress prefix=")
            .append(address)
            .append(" phase=0")
            .append(" fanout=single")
            .append(" bias=closest")
            .append(" -b ")
            .append(url);

      return executeCreate(sb.toString()).toString();*/
      return address;
   }

   public boolean exists(JSONArray jsonArray, String key, String value) throws JSONException
   {
      boolean exists = false;
      for (int i = 0; i < jsonArray.length(); i++)
      {
         JSONObject jsonObject = (JSONObject) jsonArray.get(i);
         Object prefix = jsonObject.get(key);
         if (value.equals(prefix))
         {
            exists = true;
            break;
         }
      }
      return exists;
   }

   private JSONArray executeQuery(String query) throws Exception
   {
      Runtime runtime = Runtime.getRuntime();
      Process process = runtime.exec(query);
      ProcessLogger out = new ProcessLogger(process.getInputStream(), false);
      ProcessLogger err = new ProcessLogger(process.getErrorStream(), true);
      out.start();
      err.start();
      if (process.waitFor() == 0)
         return new JSONArray(out.sb.toString());
      throw new Exception(err.sb.toString());
   }

   private JSONObject executeCreate(String query) throws Exception
   {
      Runtime runtime = Runtime.getRuntime();
      Process process = runtime.exec(query);
      ProcessLogger out = new ProcessLogger(process.getInputStream(), false);
      ProcessLogger err = new ProcessLogger(process.getErrorStream(), true);
      out.start();
      err.start();
      if (process.waitFor() == 0)
         return new JSONObject(out.sb.toString());
      throw new Exception(err.sb.toString());
   }


   static class ProcessLogger extends Thread
   {
      StringBuffer sb = new StringBuffer();
      private final InputStream is;

      private final boolean sendToErr;

      ProcessLogger(final InputStream is,
                    final boolean sendToErr) throws ClassNotFoundException
      {
         this.is = is;
         this.sendToErr = sendToErr;
         setDaemon(false);
      }

      @Override
      public void run()
      {
         try
         {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null)
            {
               sb.append(line);
            }
         }
         catch (IOException e)
         {
            // ok, stream closed
         }

      }
   }
}
