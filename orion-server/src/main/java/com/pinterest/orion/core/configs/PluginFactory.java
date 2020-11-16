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
package com.pinterest.orion.core.configs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.pinterest.orion.core.Plugin;

public abstract class PluginFactory<T extends Plugin, U extends PluginConfig> {

  private final Logger logger = Logger.getLogger(this.getClass().getCanonicalName());
  private Map<String, U> keyToConfigMap = new HashMap<>();
  private Map<String, Map<String, U>> clusterKeyToConfigMap = new HashMap<>();

  public void initialize(List<U> pluginConfigs) {
    if(pluginConfigs == null) {
      return;
    }
    for(U config : pluginConfigs) {
      addGlobalConfig(config);
    }
  }

  public void addClusterConfigs(String clusterId, List<U> pluginConfigs) throws Exception {
    if(pluginConfigs == null) {
      return;
    }
    Map<String, U> map = new HashMap<>();
    for ( U config : pluginConfigs) {
      U mergedConfig = mergeClusterConfig(clusterId, config);
      map.put(config.getKey(), mergedConfig);
    }
    clusterKeyToConfigMap.put(clusterId, map);
  }

  protected void addGlobalConfig(U config) {
    if(config.getKey() == null || config.getClazz() == null) {
      throw new IllegalArgumentException("Plugin config " + config + " is missing the key or class");
    }
    if (keyToConfigMap.containsKey(config.getKey())) {
      throw new IllegalArgumentException("Duplicate plugin config " + config );
    }
    keyToConfigMap.put(config.getKey(), config);
  }

  protected U mergeClusterConfig(String clusterId, U pluginConfig) throws Exception {
    U globalConfig = keyToConfigMap.get(pluginConfig.getKey());
    if(globalConfig == null){
      throw new Exception("Plugin config " + pluginConfig.getKey() + " for cluster " + clusterId + " base config does not exist.");
    }
    pluginConfig.merge(globalConfig);
    return pluginConfig;
  }

  @SuppressWarnings("unchecked")
  protected T createInstance(U pluginConfig)
      throws ClassNotFoundException, IllegalAccessException, InstantiationException {
    return (T) Class.forName(pluginConfig.getClazz()).newInstance();
  }

  protected Map<String, U> getKeyToConfigMap() {
    return keyToConfigMap;
  }

  protected Map<String, Map<String, U>> getClusterKeyToConfigMap() {
    return clusterKeyToConfigMap;
  }

  protected Logger getLogger() {
    return logger;
  }
}
