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
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;

public class TracingContext {
   private Span receiveSpan;
   private SpanContext spanContext;
   private Scope receiveScope;
   private Span queueSpan;
   private Span deliveringSpan;
   private Scope queueScope;
   private Scope deliveryScope;
   private Span pagedSpan;
   private Scope pagedScope;
   private Span brokeredSpan;
   private Scope brokeredScope;

   public TracingContext(Span span, SpanContext spanContext, Scope scope) {
      receiveSpan = span;
      this.spanContext = spanContext;
      receiveScope = scope;
   }

   public Span getReceiveSpan() {
      return receiveSpan;
   }

   public void setReceiveSpan(Span receiveSpan) {
      this.receiveSpan = receiveSpan;
   }

   public SpanContext getSpanContext() {
      return spanContext;
   }

   public void setSpanContext(SpanContext spanContext) {
      this.spanContext = spanContext;
   }

   public void setQueueSpan(Span queueSpan) {
      this.queueSpan = queueSpan;
   }

   public Span getQueueSpan() {
      return queueSpan;
   }

   public void setDeliveringSpan(Span deliveringSpan) {
      this.deliveringSpan = deliveringSpan;
   }

   public Scope getReceiveScope() {
      return receiveScope;
   }

   public void setReceiveScope(Scope receiveScope) {
      this.receiveScope = receiveScope;
   }

   public Span getDeliveringSpan() {
      return deliveringSpan;
   }

   public void setQueueScope(Scope queueScope) {
      this.queueScope = queueScope;
   }

   public Scope getQueueScope() {
      return queueScope;
   }

   public void setDeliveryScope(Scope deliveryScope) {
      this.deliveryScope = deliveryScope;
   }

   public Scope getDeliveryScope() {
      return deliveryScope;
   }

   public void setPagedSpan(Span pagedSpan) {
      this.pagedSpan = pagedSpan;
   }

   public Span getPagedSpan() {
      return pagedSpan;
   }

   public void setPagedScope(Scope pagedScope) {
      this.pagedScope = pagedScope;
   }

   public Scope getPagedScope() {
      return pagedScope;
   }

   public void setBrokeredSpan(Span brokeredSpan) {
      this.brokeredSpan = brokeredSpan;
   }

   public Span getBrokeredSpan() {
      return brokeredSpan;
   }

   public void setBrokeredScope(Scope brokeredScope) {
      this.brokeredScope = brokeredScope;
   }

   public Scope getBrokeredScope() {
      return brokeredScope;
   }
}
