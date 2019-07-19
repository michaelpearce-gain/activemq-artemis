/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.jms.example;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.util.GlobalTracer;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.apache.qpid.jms.JmsConnectionFactory;

import java.io.IOException;

/**
 * A simple example which shows how to use a QueueBrowser to look at messages of a queue without removing them from the queue
 */
public class MessageTracingExample {

   public static void main(final String[] args) throws Exception {

      // This example will send and receive an AMQP message
      sendConsumeAMQP();
   }

   private static void sendConsumeAMQP() throws JMSException {

      try {
         System.in.read();
      } catch (IOException e) {
         e.printStackTrace();
      }
     /* int numMessages = 100;
      Configuration.SamplerConfiguration samplerConfig = Configuration.SamplerConfiguration.fromEnv()
            .withType(ConstSampler.TYPE)
            .withParam(1);

      Configuration.ReporterConfiguration reporterConfig = Configuration.ReporterConfiguration.fromEnv()
            .withLogSpans(true);

      Configuration config = new Configuration("qpid-jms")
            .withSampler(samplerConfig)
            .withReporter(reporterConfig);

      JaegerTracer tracer = config.getTracer();
      GlobalTracer.registerIfAbsent(tracer);
      System.out.println("MessageTracingExample.sendConsumeAMQP " + tracer);
      org.apache.qpid.jms.tracing.JmsTracer jmsTracer =
            org.apache.qpid.jms.tracing.opentracing.OpenTracingTracerFactory.create(tracer);
      Connection connection = null;
      JmsConnectionFactory connectionFactory = new JmsConnectionFactory("amqp://localhost:5672?jms.tracing=opentracing");

      connectionFactory.setTracer(jmsTracer);

      System.out.println("MessageTracingExample.sendConsumeAMQP " + jmsTracer);
      try {

         // Create an amqp qpid 1.0 connection
         connection = connectionFactory.createConnection();

         // Create a session
         Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

         // Create a sender
         Queue queue = session.createQueue("examples");
         MessageProducer sender = session.createProducer(null);

         connection.start();

         // create a moving receiver, this means the message will be removed from the queue
         MessageConsumer consumer = session.createConsumer(queue);
         for (int i = 0; i < numMessages; i++ ) {
            // receive the simple message
            TextMessage m = (TextMessage) consumer.receive();
            sender.send(m.getJMSReplyTo(), session.createTextMessage("response: " + m.getText()));
         }
      } finally {
         if (connection != null) {
            // close the connection
            connection.close();
         }
      }*/
   }

}
