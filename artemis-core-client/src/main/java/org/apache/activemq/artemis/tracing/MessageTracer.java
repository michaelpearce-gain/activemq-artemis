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
package org.apache.activemq.artemis.tracing;

import org.apache.activemq.artemis.api.core.Message;

import java.util.Map;

public interface MessageTracer {

   default void init(Map<String, String> params) {
   }

   String BROKERED_SPAN_NAME = "brokered";
   String SEND_SPAN_NAME = "send";
   String RECEIVE_SPAN_NAME = "receive";
   String QUEUED_SPAN_NAME = "queued";
   String PAGED_SPAN_NAME = "paged";

   void accepted(Message message);

   void paged(Message span);

   void depaged(Message message);

   void enqueued(Message message, String queue);

   void dequeued(Message message);

   void delivered(Message message, long consumerID);

   void acked(Message message);

   boolean isEnabled();

   void setEnabled(boolean enabled);
}
