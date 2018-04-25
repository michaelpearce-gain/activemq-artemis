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
package org.apache.activemq.artemis.test;

import org.apache.activemq.artemis.configuration.XMLUpdater;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.CoreAddressConfiguration;
import org.apache.activemq.artemis.core.deployers.impl.FileConfigurationParser;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.List;

public class XMLUpdaterTest {

   @Test
   public void testReplaceAddresses() throws Exception {
      URL sourceUrl = getClass().getResource("/broker-queues.xml");
      File sourceFile = new File(sourceUrl.getFile());
      URL targetUrl = getClass().getResource("/broker.xml");
      File targetFile = new File(targetUrl.getFile());

      File newFile = new File("./target/test.xml");
      XMLUpdater xmlUpdater = new XMLUpdater();
      xmlUpdater.updateXml(sourceFile, targetFile, newFile);

      FileConfigurationParser parser = new FileConfigurationParser();
      Configuration configuration = parser.parseMainConfig(new FileInputStream(newFile));
      List<CoreAddressConfiguration> addressConfigurations = configuration.getAddressConfigurations();
      Assert.assertEquals(addressConfigurations.size(), 4);

   }
}
