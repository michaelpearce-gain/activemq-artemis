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
package org.apache.activemq.artemis.tracing.opentracing;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapAdapter;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.tracing.MessageTracer;
import org.apache.activemq.artemis.tracing.TracingContext;
import org.apache.activemq.artemis.utils.ClassloadingUtil;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

public class OpenTracingTracer implements MessageTracer {

   private Tracer tracer;

   private boolean enabled = true;

   private OpenTracingFactory factory;

   public OpenTracingTracer() {
      super();
      factory = new GlobalTracingFactory();
   }

   @Override
   public void init(Map<String, String> params) {
      enabled = params.get("enabled") != null && Boolean.valueOf(params.get("enabled"));
      String factoryClass = params.get("open-tracing-factory");
      if (factoryClass != null) {
         factory = loadClass(factoryClass);
      }
      tracer = factory.init("Artemis");
   }

   @Override
   public void accepted(Message message) {
      if (enabled && message.getTraceID() != null) {
         Map<String, String> headers = message.getTraceID();
         System.out.println("OpenTracingTracer.accepted " + headers);
         SpanContext spanContext = null;
         if (headers != null && !headers.isEmpty()) {
            spanContext = tracer.extract(Format.Builtin.TEXT_MAP, new TextMapAdapter(headers));
         }
         System.out.println("OpenTracingTracer.accepted " + spanContext);
         Span span = tracer.buildSpan(MessageTracer.RECEIVE_SPAN_NAME)
               .ignoreActiveSpan()
               .withTag(Tags.COMPONENT, "Artemis")
               .withTag(Tags.SPAN_KIND_SERVER, "Messaging")
               .asChildOf(spanContext)
               .start();
         Scope scope = tracer.activateSpan(span);
         System.out.println("OpenTracingTracer.accepted " + span);
         message.setTracingContext(new TracingContext(span, spanContext, scope));
      }
   }

   @Override
   public void paged(Message message) {

      if (!enabled) {
         return;
      }
      TracingContext tracingContext = message.getTracingContext();
      if (tracingContext != null) {
         Span brokeredSpan = getBrokerSpan(tracingContext, tracingContext.getSpanContext());
         if (brokeredSpan != null) {
            Span pagedSpan = tracer.buildSpan(MessageTracer.PAGED_SPAN_NAME)
                  .ignoreActiveSpan()
                  .withTag("PAGED", "true")
                  .asChildOf(brokeredSpan)
                  .start();
            tracingContext.setPagedSpan(pagedSpan);
            System.out.println("OpenTracingTracer.paged " + pagedSpan);
            Scope scope = tracer.activateSpan(pagedSpan);
            tracingContext.setPagedScope(scope);
         }
      }
   }

   @Override
   public void depaged(Message message) {
      if (enabled) {
         return;
      }
   }

   @Override
   public void enqueued(Message message, String queue) {
      if (!enabled) {
         return;
      }
      TracingContext tracingContext = message.getTracingContext();
      if (tracingContext != null) {
         Span receiveSpan = tracingContext.getReceiveSpan();
         if (receiveSpan != null) {
            receiveSpan.finish();
            System.out.println("OpenTracingTracer.acked " + receiveSpan);
         }
         SpanContext spanContext = tracingContext.getSpanContext();
         if (spanContext != null) {
            final Span brokeredSpan = getBrokerSpan(tracingContext, spanContext);
            final Span queueSpan = tracer.buildSpan(MessageTracer.QUEUED_SPAN_NAME)
                  .ignoreActiveSpan()
                  .withTag("queue", queue)
                  .asChildOf(brokeredSpan)
                  .start();
            tracingContext.setQueueSpan(queueSpan);
            System.out.println("OpenTracingTracer.enqueued " + queueSpan);
            Scope scope = tracer.activateSpan(queueSpan);
            tracingContext.setQueueScope(scope);
         }
      }
   }

   private Span getBrokerSpan(TracingContext tracingContext, SpanContext spanContext) {
      if (tracingContext.getBrokeredSpan() == null) {
         final Span brokeredSpan = tracer.buildSpan(MessageTracer.BROKERED_SPAN_NAME)
               .ignoreActiveSpan()
               .asChildOf(spanContext)
               .start();
         tracingContext.setBrokeredSpan(brokeredSpan);
         Scope brokeredScope = tracer.activateSpan(brokeredSpan);
         tracingContext.setBrokeredScope(brokeredScope);
         return brokeredSpan;
      }
      return tracingContext.getBrokeredSpan();
   }

   @Override
   public void dequeued(Message message) {
      if (!enabled) {
         return;
      }
      TracingContext tracingContext = message.getTracingContext();
      if (tracingContext != null) {
         Scope queueScope = tracingContext.getQueueScope();
         if (queueScope != null) {
            queueScope.close();
         }
         Span queueSpan = tracingContext.getQueueSpan();
         if (queueSpan != null) {
            tracer.activateSpan(queueSpan);
            queueSpan.finish();
            System.out.println("OpenTracingTracer.dequeued " + queueSpan);
         }
         Scope pagedScope = tracingContext.getPagedScope();
         if (pagedScope != null) {
            pagedScope.close();
         }
         Span pageSpan = tracingContext.getPagedSpan();
         if (pageSpan != null) {
            tracer.activateSpan(pageSpan);
            pageSpan.finish();
            System.out.println("OpenTracingTracer.dequeued " + pageSpan);
         }
      }
   }

   @Override
   public void delivered(Message message, long consumerID) {
      if (!enabled) {
         return;
      }
      TracingContext tracingContext = message.getTracingContext();
      if (tracingContext != null) {
         Span brokeredSpan = tracingContext.getBrokeredSpan();
         if (brokeredSpan != null) {
            Span deliverySpan = tracer.buildSpan(MessageTracer.SEND_SPAN_NAME)
                  .ignoreActiveSpan()
                  .withTag("ConsumerID", consumerID)
                  .asChildOf(brokeredSpan).start();
            Scope scope = tracer.activateSpan(deliverySpan);
            System.out.println("OpenTracingTracer.delivered " + deliverySpan);
            tracingContext.setDeliveryScope(scope);
            tracingContext.setDeliveringSpan(deliverySpan);
         }
      }
   }

   @Override
   public void acked(Message message) {
      if (!enabled) {
         return;
      }
      TracingContext tracingContext = message.getTracingContext();
      if (tracingContext != null) {
         Scope deliveryScope = tracingContext.getDeliveryScope();
         if (deliveryScope != null) {
            deliveryScope.close();
         }
         Span deliveringSpan = tracingContext.getDeliveringSpan();
         tracer.activateSpan(deliveringSpan);
         if (deliveringSpan != null) {
            deliveringSpan.finish();
            System.out.println("OpenTracingTracer.acked " + deliveringSpan);
         }
         Scope brokeredScope = tracingContext.getBrokeredScope();
         if (brokeredScope != null) {
            brokeredScope.close();
         }
         Span brokeredSpan = tracingContext.getBrokeredSpan();
         if (brokeredSpan != null) {
            brokeredSpan.finish();
         }
      }
   }


   @Override
   public boolean isEnabled() {
      return enabled;
   }

   @Override
   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   class GlobalTracingFactory implements OpenTracingFactory {

      @Override
      public Tracer init(String service) {
         return GlobalTracer.get();
      }

      @Override
      public void shutdown() {
      }
   }

   public <T> T loadClass(final String className) {
      return AccessController.doPrivileged(new PrivilegedAction<T>() {
         @Override
         public T run() {
            return (T) ClassloadingUtil.newInstanceFromClassLoader(className);
         }
      });
   }
}
