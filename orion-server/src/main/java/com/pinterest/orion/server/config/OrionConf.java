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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pinterest.orion.configs.StatsConfiguration;
import com.pinterest.orion.core.configs.ClusterConfig;
import com.pinterest.orion.core.configs.PluginConfig;

import io.dropwizard.Configuration;

public class OrionConf extends Configuration {
  
  public static final String ADMIN_ROLE = "admin";
  public static final String MGMT_ROLE = "mgmt";
  private List<ClusterConfig> clusterConfigs;
  private List<String> customApiFactoryClasses;
  private List<String> adminGroups;
  private PluginConfig actionAuditorConfigs;
  private PluginConfig clusterStateSinkConfigs;
  private Map<String, Object> alertConfigs;
  private OrionPluginConfig plugins;
  private StatsConfiguration statsConfiguration = new StatsConfiguration();
  private Map<String, Object> additionalConfigs = new HashMap<>();
  private PluginConfig metricsStoreConfigs;
  private List<PluginConfig> globalSensorConfigs;

  public List<ClusterConfig> getClusterConfigs() {
    return clusterConfigs;
  }

  public void setClusterConfigs(
      List<ClusterConfig> clusterConfigs) {
    this.clusterConfigs = clusterConfigs;
  }

  public OrionPluginConfig getPlugins() {
    return plugins;
  }

  public void setPlugins(OrionPluginConfig pluginConfig) {
    this.plugins = pluginConfig;
  }

  /**
   * @return the customApiFactoryClasses
   */
  public List<String> getCustomApiFactoryClasses() {
    return customApiFactoryClasses;
  }

  /**
   * @param customApiFactoryClasses the customApiFactoryClasses to set
   */
  public void setCustomApiFactoryClasses(List<String> customApiFactoryClasses) {
    this.customApiFactoryClasses = customApiFactoryClasses;
  }


  public List<String> getAdminGroups() {
    return adminGroups;
  }

  /**
   * @param adminGroups the adminGroups to set
   */
  public void setAdminGroups(List<String> adminGroups) {
    this.adminGroups = adminGroups;
  }

  /**
   * @return the alertConfigs
   */
  public Map<String, Object> getAlertConfigs() {
    return alertConfigs;
  }

  /**
   * @param alertConfigs the alertConfigs to set
   */
  public void setAlertConfigs(Map<String, Object> alertConfigs) {
    this.alertConfigs = alertConfigs;
  }

  /**
   * @return the auditSinkConfig
   */
  public PluginConfig getActionAuditorConfigs() {
    return actionAuditorConfigs;
  }

  /**
   * @param actionAuditorConfigs the auditSinkConfig to set
   */
  public void setActionAuditorConfigs(PluginConfig actionAuditorConfigs) {
    this.actionAuditorConfigs = actionAuditorConfigs;
  }

  /**
   * @return the clusterStateSinkConfig
   */
  public PluginConfig getClusterStateSinkConfigs() {
    return clusterStateSinkConfigs;
  }

  /**
   * @param clusterStateSinkConfigs the clusterStateSinkConfig to set
   */
  public void setClusterStateSinkConfigs(
      PluginConfig clusterStateSinkConfigs) {
    this.clusterStateSinkConfigs = clusterStateSinkConfigs;
  }

  /**
   * @return the statsConfiguration
   */
  public StatsConfiguration getStatsConfiguration() {
    return statsConfiguration;
  }

  /**
   * @param statsConfiguration the statsConfiguration to set
   */
  public void setStatsConfiguration(StatsConfiguration statsConfiguration) {
    this.statsConfiguration = statsConfiguration;
  }

  public Map<String, Object> getAdditionalConfigs() {
    return additionalConfigs;
  }

  public void setAdditionalConfigs(Map<String, Object> additionalConfigs) {
    this.additionalConfigs = additionalConfigs;
  }

  /**
   * @return the metricsStoreConfigs
   */
  public PluginConfig getMetricsStoreConfigs() {
    return metricsStoreConfigs;
  }

  /**
   * @param metricsStoreConfigs the metricsStoreConfigs to set
   */
  public void setMetricsStoreConfigs(PluginConfig metricsStoreConfigs) {
    this.metricsStoreConfigs = metricsStoreConfigs;
  }

  public List<PluginConfig> getGlobalSensorConfigs() {
    return globalSensorConfigs;
  }

  public void setGlobalSensorConfigs(List<PluginConfig> globalSensorConfigs) {
    this.globalSensorConfigs = globalSensorConfigs;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "OrionConf [clusterConfigs=" + clusterConfigs + ", customApiFactoryClasses="
        + customApiFactoryClasses + ", adminGroups=" + adminGroups + ", actionAuditorConfigs="
        + actionAuditorConfigs + ", clusterStateSinkConfigs=" + clusterStateSinkConfigs
        + ", alertConfigs=" + alertConfigs + ", plugins=" + plugins + ", statsConfiguration="
        + statsConfiguration + ", additionalConfigs=" + additionalConfigs + ", metricsStoreConfigs="
        + metricsStoreConfigs + "]";
  }
}
