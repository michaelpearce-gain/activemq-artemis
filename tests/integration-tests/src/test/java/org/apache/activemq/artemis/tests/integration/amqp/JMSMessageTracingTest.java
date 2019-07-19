/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.tests.integration.amqp;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.apache.activemq.artemis.core.settings.impl.AddressFullMessagePolicy;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.tracing.MessageTracer;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.activemq.artemis.tracing.opentracing.OpenTracingTracer;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JMSMessageTracingTest extends JMSClientTestSupport {

   protected static final Logger LOG = LoggerFactory.getLogger(JMSMessageTracingTest.class);
   private final MockTracer mockTracer;

   public JMSMessageTracingTest() {
      mockTracer = new MockTracer();
      DummyTracer.mockTracer = mockTracer;
   }

   @Override
   protected String getConfiguredProtocols() {
      return "AMQP,OPENWIRE,CORE";
   }

   @Override
   protected org.apache.activemq.artemis.core.config.Configuration createDefaultConfig(boolean netty) throws Exception {
      org.apache.activemq.artemis.core.config.Configuration defaultConfig = super.createDefaultConfig(netty);
      defaultConfig.setMessageTracingClass(OpenTracingTracer.class.getName() + ";enabled=false;open-tracing-factory=" + DummyTracer.class.getName());
      Map<String, AddressSettings> addressesSettings = defaultConfig.getAddressesSettings();
      AddressSettings as = new AddressSettings();
      as.setAddressFullMessagePolicy(AddressFullMessagePolicy.PAGE);
      as.setMaxSizeBytes(1000);
      as.setPageSizeBytes(200);
      addressesSettings.put("testQueue", as);
      return defaultConfig;
   }

   @Test
   public void BasicSend() throws JMSException {
      server.getmessageTracer().setEnabled(true);
      try (Connection connection1 = createConnection(); Connection connection2 = createConnection();) {

         Session session1 = connection1.createSession(false, Session.AUTO_ACKNOWLEDGE);
         Session session2 = connection2.createSession(false, Session.AUTO_ACKNOWLEDGE);

         javax.jms.Queue queue1 = session1.createQueue("testQueue");
         javax.jms.Queue queue2 = session2.createQueue("testQueue");

         final MessageConsumer consumer2 = session2.createConsumer(queue2);

         MessageProducer producer = session1.createProducer(queue1);
         producer.setDeliveryMode(DeliveryMode.PERSISTENT);

         TextMessage message = session1.createTextMessage();
         message.setText("msg 1");
         producer.send(message);

         connection1.start();

         Message received = consumer2.receive(100);
         //make sure the ack has happened
         connection1.close();
         List<MockSpan> mockSpans = mockTracer.finishedSpans();
         checkSpans(mockSpans, 0, false);
      }
   }

   @Test
   public void tesMultipleSend() throws JMSException {
      server.getmessageTracer().setEnabled(true);
      int numMessages = 3;
      try (Connection connection1 = createConnection(); Connection connection2 = createConnection();) {

         Session session1 = connection1.createSession(false, Session.AUTO_ACKNOWLEDGE);
         Session session2 = connection2.createSession(false, Session.AUTO_ACKNOWLEDGE);

         javax.jms.Queue queue1 = session1.createQueue("testQueue");
         javax.jms.Queue queue2 = session2.createQueue("testQueue");

         final MessageConsumer consumer2 = session2.createConsumer(queue2);

         MessageProducer producer = session1.createProducer(queue1);
         producer.setDeliveryMode(DeliveryMode.PERSISTENT);

         for (int i = 0; i < numMessages; i++) {
            TextMessage message = session1.createTextMessage();
            message.setText("msg" + i);
            producer.send(message);
         }

         connection1.start();
         for (int i = 0; i < numMessages; i++) {
            Message received = consumer2.receive(100);
         }
         connection1.close();
         List<MockSpan> mockSpans = mockTracer.finishedSpans();
         checkSpans(mockSpans, 0, false);
         checkSpans(mockSpans, 1, false);
         checkSpans(mockSpans, 2, true);
      }
   }

   private void checkSpans(List<MockSpan> mockSpans, int pos, boolean paged) {
      MockSpan parentSpan = getParentSpan(mockSpans, pos);
      long parentid = parentSpan.context().spanId();
      Map<String, MockSpan> childSpans = getChildSpans(mockSpans, parentid);
      Assert.assertEquals(2, childSpans.size());
      MockSpan brokeredSpan = childSpans.get(MessageTracer.BROKERED_SPAN_NAME);
      Assert.assertNotNull(brokeredSpan);
      MockSpan receiveSpan = childSpans.get(MessageTracer.RECEIVE_SPAN_NAME);
      Assert.assertNotNull(receiveSpan);
      Map<String, MockSpan> brokeredChildSpans = getChildSpans(mockSpans, brokeredSpan.context().spanId());
      if (paged) {
         Assert.assertEquals(3, brokeredChildSpans.size());
         MockSpan queuedSpan = brokeredChildSpans.get(MessageTracer.QUEUED_SPAN_NAME);
         Assert.assertNotNull(queuedSpan);
         MockSpan sendSpan = brokeredChildSpans.get(MessageTracer.SEND_SPAN_NAME);
         Assert.assertNotNull(sendSpan);
         MockSpan pagedSpan = brokeredChildSpans.get(MessageTracer.PAGED_SPAN_NAME);
         Assert.assertNotNull(pagedSpan);
      } else {
         Assert.assertEquals(2, brokeredChildSpans.size());
         MockSpan queuedSpan = brokeredChildSpans.get(MessageTracer.QUEUED_SPAN_NAME);
         Assert.assertNotNull(queuedSpan);
         MockSpan sendSpan = brokeredChildSpans.get(MessageTracer.SEND_SPAN_NAME);
         Assert.assertNotNull(sendSpan);
         for (MockSpan mockSpan : mockSpans) {
            System.out.println("JMSMessageTracingTest.testDeliveryMode");
         }
      }
   }

   @Override
   protected String getJmsConnectionURIOptions() {
      return "jms.tracing=opentracing";
   }

   @Override
   protected void configureConnectionFactory(JmsConnectionFactory factory) {
      org.apache.qpid.jms.tracing.JmsTracer jmsTracer =
            org.apache.qpid.jms.tracing.opentracing.OpenTracingTracerFactory.create(mockTracer);
      factory.setTracer(jmsTracer);
   }


   private Map<String, MockSpan> getChildSpans(List<MockSpan> mockSpans, long parentid) {
      Map<String, MockSpan> childSpans = new HashMap<>();
      for (MockSpan mockSpan : mockSpans) {
         if (mockSpan.parentId() == parentid) {
            childSpans.put(mockSpan.operationName(), mockSpan);
         }
      }
      return childSpans;
   }

   private MockSpan getParentSpan(List<MockSpan> mockSpans, int pos) {
      int parentPos = 0;
      for (MockSpan mockSpan : mockSpans) {
         if (mockSpan.parentId() == 0 && parentPos++ == pos) {
            return mockSpan;
         }
      }
      Assert.fail("cant locate parent span");
      return null;
   }

}
