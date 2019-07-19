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

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.server.MessageReference;
import org.apache.activemq.artemis.core.server.ServerConsumer;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.server.impl.AckReason;
import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerPlugin;
import org.apache.activemq.artemis.core.transaction.Transaction;

public class TracingPlugin implements ActiveMQServerPlugin {

  /* @Override
   public void beforeSend(ServerSession session,
                          Transaction tx,
                          Message message,
                          boolean direct,
                          boolean noAutoCreateQueue) throws ActiveMQException {

      try {
         System.out.println("Introducing latency for message send: " + message);
         Thread.sleep(500);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }
*/
/*   @Override
   public void messageAcknowledged(MessageReference ref, AckReason reason, ServerConsumer consumer) throws ActiveMQException {
      try {
         System.out.println("Introducing latency for message ack: " + ref.getMessage());
         Thread.sleep(500);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }*/
}