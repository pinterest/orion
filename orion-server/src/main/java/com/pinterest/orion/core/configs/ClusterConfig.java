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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pinterest.orion.server.config.OrionPluginConfig;

import java.util.Collections;
import java.util.Map;

public class ClusterConfig {
  @JsonProperty
  private String clusterId;

  @JsonProperty
  private String type;

  @JsonProperty
  private Map<String, Object> configuration = Collections.EMPTY_MAP;

  private OrionPluginConfig plugins;

  public String getClusterId() {
    return clusterId;
  }

  public void setClusterId(String clusterId) {
    this.clusterId = clusterId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Map<String, Object> getConfiguration() {
    return configuration;
  }

  public void setConfiguration(Map<String, Object> configuration) {
    this.configuration = configuration;
  }

  public OrionPluginConfig getPlugins() {
    return plugins;
  }

  public void setPlugins(OrionPluginConfig plugins) {
    this.plugins = plugins;
  }
}
