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
package com.pinterest.orion.core.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.PluginException;
import com.pinterest.orion.core.actions.schema.ActionSchema;
import com.pinterest.orion.core.configs.ActionConfig;
import com.pinterest.orion.core.configs.EndpointConfig;
import com.pinterest.orion.core.configs.PluginConfig;
import com.pinterest.orion.core.configs.PluginFactory;

public class ActionFactory extends PluginFactory<Action, ActionConfig> {
  private Map<String, String> classToKeyMap = new HashMap<>();

  @Override
  protected void addGlobalConfig(ActionConfig config) {
    super.addGlobalConfig(config);
    classToKeyMap.put(config.getClazz(), config.getKey());
    getLogger().info("Global action configured: " + config.getKey());
  }

  public Map<String, Object> getActionConfiguration(String clusterId, Class<? extends Action> actionClass) {
    Map<String, Object> emptyMap = Collections.emptyMap();
    String key = classToKeyMap.get(actionClass.getName());
    if (key == null) {
      return emptyMap;
    }
    try {
      PluginConfig config = getPluginConfigOfCluster(clusterId, key);
      if (config.getConfiguration() != null) {
        return config.getConfiguration();
      }
    } catch (Exception e) {
      getLogger().warning("Failed to fetch action configs: " + e);
    }
    return emptyMap;
  }

  public Action getActionInstance(Cluster cluster, String actionKey) throws Exception {
    ActionConfig actionConfig = getPluginConfigOfCluster(cluster.getClusterId(), actionKey);
    if( actionConfig == null ||
        !actionConfig.isEnabled() ||
        !actionConfig.isEndpointEnabled()
    ) {
      throw new PluginException("Action " + actionKey + " is disabled on cluster " + cluster.getClusterId());
    }
    Action instance = createInstance(actionConfig);
    instance.setEngine(cluster.getActionEngine());
    return instance;
  }

  public boolean isActionEnabledOnCluster(String clusterId, String className) {
    try {
      if (!classToKeyMap.containsKey(className)) {
        return false;
      }
      return getPluginConfigOfCluster(clusterId, classToKeyMap.get(className)).isEnabled();
    } catch (PluginException pe) {
      return false;
    }
  }

  private ActionConfig getPluginConfigOfCluster(String clusterId, String key) throws PluginException {
    Map<String, ActionConfig> clusterConfigs = getClusterKeyToConfigMap().get(clusterId);
    if (clusterConfigs != null && clusterConfigs.containsKey(key)){
      return clusterConfigs.get(key);
    }
    if (!getKeyToConfigMap().containsKey(key)) {
      throw new PluginException("Cannot find the action: " + key + " in the action configs");
    }
    return getKeyToConfigMap().get(key);
  }

  public List<ActionSchema> getEnabledActionSchemas(String clusterId) throws Exception {
    Map<String, ActionConfig> tmpMap = new HashMap<>();
    Map<String, ActionConfig> globalMap = getKeyToConfigMap();
    if (globalMap != null) {
      for (Map.Entry<String, ActionConfig> entry : globalMap.entrySet()) {
        if (entry.getValue().isEnabled() && entry.getValue().isEndpointEnabled()) {
          tmpMap.put(entry.getKey(), entry.getValue());
        }
      }
    }
    Map<String, ActionConfig> clusterMap = getClusterKeyToConfigMap().get(clusterId);
    if (clusterMap != null) {
      for (Map.Entry<String, ActionConfig> entry : clusterMap.entrySet()) {
        if (entry.getValue().isEnabled() && entry.getValue().isEndpointEnabled()) {
          tmpMap.put(entry.getKey(), entry.getValue());
        } else {
          tmpMap.remove(entry.getKey());
        }
      }
    }

    List<ActionSchema> actionSchemas = new ArrayList<>();

    for (Map.Entry<String, ActionConfig> entry : tmpMap.entrySet()) {
      EndpointConfig endpointConfig = entry.getValue().getEndpoint();
      Map<String, Object> config = entry.getValue().getConfiguration();
      actionSchemas.add(new ActionSchema(
          entry.getKey(),
          endpointConfig.getDisplayName(),
          endpointConfig.getLabel(),
          endpointConfig.getIcon(),
          createInstance(entry.getValue()).generateSchema(config))
      );
    }

    return actionSchemas;
  }

}
