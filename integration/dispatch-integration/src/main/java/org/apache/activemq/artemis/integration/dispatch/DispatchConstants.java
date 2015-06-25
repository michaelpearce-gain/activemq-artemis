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

import java.util.HashSet;
import java.util.Set;

public class DispatchConstants
{
   public static final Set<String> ALLOWABLE_PROPERTIES = new HashSet<>();

   public static final Set<String> REQUIRED_PROPERTIES = new HashSet<>();

   public static final String URL = "url";

   public static final String LINK_PREFIX = "prefix";

   public static final String CONNECTOR_ADDRESS = "connector-address";

   public static final String CONNECTOR_PORT = "connector-port";

   public static final String QOS = "qos";
   static
   {
      ALLOWABLE_PROPERTIES.add(URL);
      ALLOWABLE_PROPERTIES.add(LINK_PREFIX);
      ALLOWABLE_PROPERTIES.add(CONNECTOR_ADDRESS);
      ALLOWABLE_PROPERTIES.add(CONNECTOR_PORT);
      ALLOWABLE_PROPERTIES.add(QOS);
   }

   static
   {
      REQUIRED_PROPERTIES.add(URL);
      REQUIRED_PROPERTIES.add(CONNECTOR_ADDRESS);
      REQUIRED_PROPERTIES.add(CONNECTOR_PORT);
      REQUIRED_PROPERTIES.add(QOS);
   }
}
