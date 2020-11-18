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
package com.pinterest.orion.core.automation.operator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.pinterest.orion.core.configs.PluginConfig;
import com.pinterest.orion.core.configs.PluginFactory;

public class OperatorFactory extends PluginFactory<Operator, PluginConfig> {

  public List<Operator> getAllOperatorInstances(String clusterId) throws Exception {
    Map<String, PluginConfig> map = new HashMap<>(getKeyToConfigMap());
    Map<String, PluginConfig> clusterMap = getClusterKeyToConfigMap().get(clusterId);
    if(clusterMap != null){
      map.putAll(clusterMap);
    }
    return createOperatorInstancesFromConfigs(map.values());
  }

  public List<Operator> createOperatorInstancesFromConfigs(Collection<PluginConfig> pluginConfigs) throws Exception {
    List<Operator> operatorInstances = new ArrayList<>();
    Map<String, Object> emptyMap = Collections.emptyMap();
    for (PluginConfig pluginConfig : pluginConfigs) {
      if(!pluginConfig.isEnabled()) {
        continue;
      }
      Operator operator = createInstance(pluginConfig);
      if(pluginConfig.getConfiguration() != null){
        operator.initialize(pluginConfig.getConfiguration());
      } else {
        operator.initialize(emptyMap);
      }
      operatorInstances.add(operator);
    }
    return operatorInstances;
  }
}
