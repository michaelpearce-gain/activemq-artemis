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
package org.apache.activemq.artemis;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.utils.json.JSONArray;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.UnsignedByte;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.messaging.Header;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.apache.qpid.proton.driver.Connector;
import org.apache.qpid.proton.driver.Driver;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.engine.Session;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.message.impl.MessageImpl;
import org.apache.qpid.proton.messenger.Messenger;
import org.apache.qpid.proton.messenger.impl.MessengerImpl;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
//import org.junit.Test;

public class DispatchManagementTest
{
   private static final int NUM_MESSAGES = 10;
   private static final String MANAGEMENT_ADDRESS = "brokerQOS2.management";

   @Test
   public void testGetServerInfo2() throws Exception
   {
      Driver driver = Driver.Factory.create();
      Connector<Object> connector = driver.createConnector("192.168.5.1:5672", 5672, null);
      Connection connection = connector.getConnection();
      connection.open();
      Session session = connection.session();
      session.open();
      Sender sender = session.sender(MANAGEMENT_ADDRESS);
      Target target = new Target();
      target.setAddress("");
      sender.setTarget(target);
      // the C implemenation does this:
      Source source = new Source();
      source.setAddress(MANAGEMENT_ADDRESS);
      sender.setSource(source);
      sender.setSenderSettleMode(SenderSettleMode.UNSETTLED);
      sender.setReceiverSettleMode(ReceiverSettleMode.SECOND);
   }
   @Test
   public void testGetServerInfo() throws Exception
   {
      try
      {
         Messenger mng = new MessengerImpl();
         mng.start();

         Message msg = new MessageImpl();
         msg.setAddress("amqp://192.168.5.1:5672/brokerQOS1.management");
         msg.setSubject("subject");
         HashMap value = new HashMap();
         value.put("_AMQ_ResourceName", "dispatch.management");
         value.put("_AMQ_OperationName", "getServiceInfo");
         msg.setReplyTo("brokerQOS1.reply");
         byte[] bytes = new byte[1];
         bytes[0] = (byte) 0;
         msg.setBody(new Data(new Binary(bytes)));
         ApplicationProperties properties = new ApplicationProperties(value);
         msg.setApplicationProperties(properties);
         mng.put(msg);
         mng.send();
         mng.stop();
         mng = new MessengerImpl();
         mng.setIncomingWindow(0);
         mng.setOutgoingWindow(0);
         mng.start();
         mng.subscribe("amqp://192.168.5.1:5672/brokerQOS1.reply");
         mng.recv();
         msg = mng.get();
         System.out.println("msg = " + msg.getBody());
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }


   @Test
   public void testCreateWaypoint() throws Exception
   {
      try
      {
         Messenger mng = new MessengerImpl();
         mng.start();

         Message msg = new MessageImpl();
         msg.setAddress("amqp://192.168.5.1:5672/brokerQOS2.management");
         msg.setSubject("subject");
         HashMap value = new HashMap();
         value.put("_AMQ_ResourceName", "dispatch.management");
         value.put("_AMQ_OperationName", "createWaypoint");
         msg.setReplyTo("brokerQOS2.reply");
         JSONArray jsonArray = new JSONArray();
         jsonArray.put("testq2");
         SimpleString args = new SimpleString(jsonArray.toString());
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         baos.write(1);
         byte[] bytes = args.getData();
         ByteBuffer b = ByteBuffer.allocate(4);
         b.putInt(bytes.length);
         baos.write(b.array());
         baos.write(bytes);
         msg.setBody(new Data(new Binary(baos.toByteArray())));
         ApplicationProperties properties = new ApplicationProperties(value);
         msg.setApplicationProperties(properties);
         mng.put(msg);
         mng.send();
         mng.stop();
         mng = new MessengerImpl();
         mng.start();
         mng.subscribe("amqp://192.168.5.1:5672/brokerQOS2.reply");
         mng.recv();
         msg = mng.get();
         System.out.println("msg = " + msg.getBody());

      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   @Test
   public void testQueryConnector() throws Exception
   {
      try
      {
         Messenger mng = new MessengerImpl();
         mng.start();
         mng.subscribe("amqp://192.168.5.1:5872/#");
         Message msg = new MessageImpl();
         msg.setAddress("amqp://192.168.5.1:5872/broker.QOS1");
         HashMap<Object, Object> value1 = new HashMap<>();
         value1.put("attributeNames", new ArrayList<>());
         msg.setBody(new AmqpValue(value1));
         msg.setReplyTo("123456789");
         HashMap value = new HashMap();
         value.put("operation", "QUERY");
         value.put("entityType", "connector");
         value.put("type", "org.amqp.management");
         value.put("name", "self");
         ApplicationProperties properties = new ApplicationProperties(value);
         msg.setApplicationProperties(properties);
         Header header = new Header();
         header.setDurable(false);
         header.setFirstAcquirer(false);
         header.setPriority(new UnsignedByte((byte) 4));
         msg.setHeader(header);
         mng.put(msg);
         mng.send();
         mng.stop();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }


   @Test
   public void testSendBrokerAFromDispatchA() throws Exception
   {
      try
      {
         Messenger mng = new MessengerImpl();
         mng.start();
         for (int i = 0; i < NUM_MESSAGES; i++)
         {
            Message msg = new MessageImpl();
            msg.setAddress("amqp://192.168.5.1:5672/testq");
            msg.setSubject("subject");
            msg.setBody(new AmqpValue("for Broker A " + i));
            mng.put(msg);
            mng.send();
         }
         mng.stop();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   @Test
   public void testRecBrokerAFromDispatchA() throws Exception
   {
      try
      {
         Messenger mng = new MessengerImpl();
         mng.start();
         mng.subscribe("amqp://192.168.5.1:5672/testq");
         for (int i = 0; i < NUM_MESSAGES * 10000; i++)
         {
            mng.recv();
            Message msg = mng.get();
            System.out.println("msg = " + msg.getBody());
         }
         mng.stop();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   // @Test
   public void testSendBrokerBFromDispatchA() throws Exception
   {
      try
      {
         Messenger mng = new MessengerImpl();
         mng.start();
         for (int i = 0; i < NUM_MESSAGES; i++)
         {
            Message msg = new MessageImpl();
            msg.setAddress("amqp://127.0.0.1:5872/brokerB.testQueue");
            msg.setSubject("subject");
            msg.setBody(new AmqpValue("for Broker B " + i));
            mng.put(msg);
            mng.send();
         }
         mng.stop();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   //  @Test
   public void testRecBrokerBFromDispatchA() throws Exception
   {
      try
      {
         Messenger mng = new MessengerImpl();
         mng.start();
         mng.subscribe("amqp://127.0.0.1:5872/brokerB.testQueue");
         for (int i = 0; i < NUM_MESSAGES * 1000; i++)
         {
            mng.recv();
            Message msg = mng.get();
            System.out.println("msg = " + msg.getBody());
         }
         mng.stop();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }
}
