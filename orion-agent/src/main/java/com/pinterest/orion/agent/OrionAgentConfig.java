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
package com.pinterest.orion.agent;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pinterest.orion.configs.StatsConfiguration;
import com.pinterest.orion.utils.NetworkUtils;

public class OrionAgentConfig {

  public static final String CMD_LOG_DIRECTORY = "cmdLogDirectory";

  private static final Logger logger = Logger.getLogger(OrionAgentConfig.class.getCanonicalName());

  private Map<String, String> agentConfigs;
  private Map<String, String> connectionConfigs;
  private StatsConfiguration statsConfigs = new StatsConfiguration();
  private String metricsFilepath = "/etc/orion-agent/metrics/kafka-agent-metrics.json";
  private String metricsPrefix = "orion.agent";

  public String getMetricsFilepath() {
    return metricsFilepath;
  }

  public void setMetricsFilepath(String metricsFilepath) {
    this.metricsFilepath = metricsFilepath;
  }

  /**
   * @return the agentConfigs
   */
  public Map<String, String> getAgentConfigs() {
    return agentConfigs;
  }

  /**
   * @param agentConfigs the agentConfigs to set
   */
  public void setAgentConfigs(Map<String, String> agentConfigs) {
    this.agentConfigs = agentConfigs;
  }

  public Map<String, String> getConnectionConfigs() {
    return connectionConfigs;
  }

  public void setConnectionConfigs(Map<String, String> connectionConfigs) {
    this.connectionConfigs = connectionConfigs;
  }

  public String getHostname() {
    return NetworkUtils.getHostnameForLocalhost();
  }

  public String getIp() {
    return NetworkUtils.getIpForHost();
  }

  public String getVersion() {
    Properties props = new Properties();
    try {
      props.load(getClass().getClassLoader().getResourceAsStream("project.properties"));
    } catch (IOException ioe) {
      logger.log(Level.WARNING, "Failed to get agent version", ioe);
    }
    return props.getProperty("version", "null");
  }

  public String getDefaultClusterId() {
    return agentConfigs.get("clusterId");
  }

  @JsonIgnore
  public String getCmdLogDirectory() {
    return agentConfigs.getOrDefault(CMD_LOG_DIRECTORY, "/tmp/orion-agent/logs");
  }

  public String getOrionServerUrl() {
    return agentConfigs.getOrDefault("serverUrl", "http://localhost:8080") + "/register";
  }

  public long getPollInterval() {
    return Long.parseLong(agentConfigs.getOrDefault("pollInterval", "5000"));
  }

  public long getMetricsPollInterval() {
    return Long.parseLong(agentConfigs.getOrDefault("metricsPollInterval", "30000"));
  }

  public boolean isReadOnly() {
    return Boolean.parseBoolean(agentConfigs.getOrDefault("readOnly", "true"));
  }

  public boolean isEnableServiceMetrics() {
    return Boolean.parseBoolean(agentConfigs.getOrDefault("enableServiceMetrics", "false"));
  }

  public boolean isEnableHeartbeat() {
    return Boolean.parseBoolean(agentConfigs.getOrDefault("enableHeartbeat", "true"));
  }

  public String getMetricsPrefix() {
    return metricsPrefix;
  }

  public void setMetricsPrefix(String metricsPrefix) {
    this.metricsPrefix = metricsPrefix;
  }

  /**
   * @return the statsConfigs
   */
  public StatsConfiguration getStatsConfigs() {
    return statsConfigs;
  }

  /**
   * @param statsConfigs the statsConfigs to set
   */
  public void setStatsConfigs(StatsConfiguration statsConfigs) {
    this.statsConfigs = statsConfigs;
  }

}
