/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis;


import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import java.net.URLDecoder;


@Path("/artemis")
@ApplicationScoped
public class ArtemisEndpoint {

   Logger logger = Logger.getLogger(ArtemisBootstrapService.class);

   @Inject
   private ArtemisBootstrapService artemisBootstrapService;

   @GET
   @Produces("text/plain")
   public Response doGet() {
      return Response.ok("Bootstrap Available!").build();
   }

   @GET
   @Produces("text/plain")
   @Path("/create")
   public Response doCreate(@QueryParam("configuration") String configuration,
                            @QueryParam("artemisCreateCommand") String artemisCreateCommand) {
      try {
         //not sure why but xml isnt decoded
         artemisBootstrapService.create(configuration == null ? null : URLDecoder.decode(configuration, "UTF-8"), artemisCreateCommand);
      } catch (Throwable e) {
         logger.error("unable to create broker", e);
         return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
      }
      return Response.ok("creating Artemis Broker").build();
   }

   @GET
   @Produces("text/plain")
   @Path("/start")
   public Response doStart() {
      try {
         artemisBootstrapService.start();
      } catch (Throwable e) {
         logger.error("unable to start broker", e);
         return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
      }
      return Response.ok("starting Artemis Broker").build();
   }

   @GET
   @Produces("text/plain")
   @Path("/stop")
   public Response doStop(@DefaultValue("true") @PathParam("wait") Boolean wait) {
      try {
         artemisBootstrapService.stop(wait);
      } catch (Throwable e) {
         logger.error("unable to stop broker", e);
         return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
      }
      return Response.ok("stopping Artemis Broker").build();
   }

   @GET
   @Produces("text/plain")
   @Path("/kill")
   public Response doKill() {
      try {
         artemisBootstrapService.kill();
      } catch (Throwable e) {
         logger.error("unable to kill broker", e);
         return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
      }
      return Response.ok("killing Artemis Broker").build();
   }
}