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
package com.pinterest.orion.server.config;

import java.util.List;

import com.pinterest.orion.core.configs.ActionConfig;
import com.pinterest.orion.core.configs.AlertConfig;
import com.pinterest.orion.core.configs.PluginConfig;
import com.pinterest.orion.core.configs.SensorConfig;

public class OrionPluginConfig {
  private List<SensorConfig> sensorConfigs;
  private List<PluginConfig> operatorConfigs;
  private List<ActionConfig> actionConfigs;
  private List<AlertConfig> alertConfigs;

  public List<SensorConfig> getSensorConfigs() {
    return sensorConfigs;
  }

  public void setSensorConfigs(
      List<SensorConfig> sensorConfigs) {
    this.sensorConfigs = sensorConfigs;
  }

  public List<PluginConfig> getOperatorConfigs() {
    return operatorConfigs;
  }

  public void setOperatorConfigs(
      List<PluginConfig> operatorConfigs) {
    this.operatorConfigs = operatorConfigs;
  }

  public List<ActionConfig> getActionConfigs() {
    return actionConfigs;
  }

  public void setActionConfigs(
      List<ActionConfig> actionConfigs) {
    this.actionConfigs = actionConfigs;
  }

  public List<AlertConfig> getAlertConfigs() {
    return alertConfigs;
  }

  public void setAlertConfigs(
      List<AlertConfig> alertConfigs) {
    this.alertConfigs = alertConfigs;
  }

  @Override
  public String toString() {
    return "OrionPluginConfig{" +
        "sensorConfigs=" + sensorConfigs +
        ", operatorConfigs=" + operatorConfigs +
        ", actionConfigs=" + actionConfigs +
        ", alertConfigs=" + alertConfigs +
        '}';
  }
}
