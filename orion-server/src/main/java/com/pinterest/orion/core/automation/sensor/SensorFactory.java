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
package com.pinterest.orion.core.automation.sensor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.configs.PluginFactory;
import com.pinterest.orion.core.configs.SensorConfig;

public class SensorFactory extends PluginFactory<Sensor, SensorConfig> {

  public List<Sensor> getAllSensorInstances(String clusterId) throws Exception {
    Map<String, SensorConfig> map = new HashMap<>(getKeyToConfigMap());
    Map<String, SensorConfig> clusterMap = getClusterKeyToConfigMap().get(clusterId);
    if(clusterMap != null){
      map.putAll(clusterMap);
    }
    return createSensorInstancesFromConfigs(map.values());
  }

  public List<Sensor> createSensorInstancesFromConfigs(Collection<SensorConfig> sensorConfigs) throws Exception {
    List<Sensor> sensorInstances = new ArrayList<>();
    Map<String, Object> emptyMap = Collections.emptyMap();
    for(SensorConfig sensorConfig : sensorConfigs) {
      if(sensorConfig.getInterval() <= 0){
        throw new PluginConfigurationException("Sensor interval for " + sensorConfig.getClazz() + " has to be larger than 0." + sensorConfigs);
      }
      if(!sensorConfig.isEnabled()) {
        continue;
      }
      Sensor sensor = createInstance(sensorConfig);
      sensor.setSensorIdentifier(sensorConfig.getKey());
      sensor.setInterval(sensorConfig.getInterval());
      if(sensorConfig.getConfiguration() != null) {
        sensor.initialize(sensorConfig.getConfiguration());
      } else {
        sensor.initialize(emptyMap);
      }
      sensorInstances.add(sensor);
    }
    return sensorInstances;
  }
}
