/*******************************************************************************
 * Copyright 2020 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.pinterest.orion.server;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.pinterest.orion.server.config.OrionConf;

import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import org.junit.Test;

import java.io.File;
import java.util.Map;

public class OrionConfTest {
  private final ObjectMapper objectMapper = Jackson.newObjectMapper();
  private final YamlConfigurationFactory<OrionConf> factory =
      new YamlConfigurationFactory<>(OrionConf.class, null, objectMapper, "dw");

  @Test
  public void testOrionConf() throws Exception {
    final File yml = new File(Resources.getResource("configs/server.yml").toURI());
    final OrionConf conf = factory.build(yml);
    assertEquals("testAction", conf.getPlugins().getActionConfigs().get(0).getKey());
//    assertEquals(ActionConfig.ActionType.CLUSTER, conf.getActionConfigs().get(0).getType());
    assertEquals("value3", ((Map<String, String>) conf.getPlugins().getActionConfigs().get(0).getConfiguration().get("key2")).get("key3"));
  }

}