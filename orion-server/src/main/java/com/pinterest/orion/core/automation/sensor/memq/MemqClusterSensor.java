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
package com.pinterest.orion.core.automation.sensor.memq;

import java.util.Map;

import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.memq.MemqCluster;

public class MemqClusterSensor extends MemqSensor {

  private Map<String, Object> config;

  @Override
  public String getName() {
    return "memqbrokersensor";
  }

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    super.initialize(config);
  }

  @Override
  public void sense(MemqCluster cluster) throws Exception {
    try {
      NodeInfo info = new NodeInfo();
      info.setClusterId(cluster.getClusterId());
      info.setHostname("test");
      info.setIp("127.0.0.1");
      info.setNodeType("i3.2xlarge");
      info.setNodeId("01");
      cluster.addNodeWithoutAgent(info);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

}
