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

import java.util.HashMap;
import java.util.Map;

public class PluginConfig {
  @JsonProperty
  private String key;

  @JsonProperty("class")
  private String clazz = null;

  @JsonProperty
  private Map<String, Object> configuration;

  @JsonProperty
  private boolean enabled = false;

  @JsonProperty
  private boolean endpointEnabled = false;
  // TODO turn the access scope into a enum member variable that captures all scopes

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getClazz() {
    return clazz;
  }

  public void setClazz(String clazz) {
    this.clazz = clazz;
  }

  public Map<String, Object> getConfiguration() {
    return configuration;
  }

  public void setConfiguration(Map<String, Object> configuration) {
    this.configuration = configuration;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isEndpointEnabled() {
    return endpointEnabled;
  }

  public void merge(PluginConfig baseConfig) {
    if(baseConfig.clazz != null){
      this.clazz = baseConfig.clazz;
    }
    if(this.configuration == null){
      this.configuration = baseConfig.configuration;
    } else if (baseConfig.configuration != null){
      Map<String, Object> newConfiguration = new HashMap<>(baseConfig.configuration);
      newConfiguration.putAll(this.configuration);
      this.configuration = newConfiguration;
    }
    this.endpointEnabled = baseConfig.endpointEnabled;
  }

  @Override
  public String toString() {
    return "PluginConfig{" +
        "key='" + key + '\'' +
        ", class='" + clazz + '\'' +
        ", config=" + configuration +
        '}';
  }
}
