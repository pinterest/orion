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
package com.pinterest.orion.core.actions.alert;

import com.pinterest.orion.core.configs.AlertConfig;
import com.pinterest.orion.core.configs.PluginFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlertFactory extends PluginFactory<Alert, AlertConfig> {

  public List<Alert> getAlertInstancesOfLevel(String clusterId, AlertLevel level) throws Exception {
    Map<String, AlertConfig> map = new HashMap<>(getKeyToConfigMap());
    Map<String, AlertConfig> clusterMap = getClusterKeyToConfigMap().get(clusterId);
    if(clusterMap != null){
      map.putAll(clusterMap);
    }
    return createInitializedAlertInstancesFromConfigs(map.values(), level);
  }

  private List<Alert> createInitializedAlertInstancesFromConfigs(Collection<AlertConfig> alertConfigs, AlertLevel level) throws Exception {
    List<Alert> alertInstances = new ArrayList<>();
    Map<String, Object> emptyMap = Collections.emptyMap();
    for (AlertConfig alertConfig : alertConfigs) {
      if(!alertConfig.isEnabled() || !alertConfig.getLevel().contains(level)) {
        continue;
      }
      Alert alert = createInstance(alertConfig);
      if(alertConfig.getConfiguration() != null){
        alert.initialize(alertConfig.getConfiguration());
      } else {
        alert.initialize(emptyMap);
      }
      alertInstances.add(alert);
    }
    return alertInstances;
  }

}
