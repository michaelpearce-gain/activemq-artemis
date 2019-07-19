package org.apache.activemq.artemis.tracing;
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

import org.apache.activemq.artemis.api.core.Message;

public class NOOPTracer implements MessageTracer {

   public NOOPTracer() {
      super();
   }


   @Override
   public void accepted(Message message) {

   }

   @Override
   public void paged(Message span) {

   }

   @Override
   public void depaged(Message message) {

   }

   @Override
   public void enqueued(Message message, String queue) {

   }

   @Override
   public void dequeued(Message message) {

   }

   @Override
   public void delivered(Message message, long l) {

   }

   @Override
   public void acked(Message message) {

   }

   @Override
   public boolean isEnabled() {
      return false;
   }

   @Override
   public void setEnabled(boolean enabled) {

   }
}
