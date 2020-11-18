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
package com.pinterest.orion.core;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

import com.pinterest.orion.core.actions.ActionFactory;
import com.pinterest.orion.core.actions.alert.AlertFactory;
import com.pinterest.orion.core.actions.audit.ActionAuditor;
import com.pinterest.orion.core.automation.operator.OperatorFactory;
import com.pinterest.orion.core.automation.sensor.SensorFactory;
import com.pinterest.orion.core.metrics.MetricsStore;
import com.pinterest.orion.server.config.OrionPluginConfig;

public class ClusterManager {

  private SensorFactory sensorFactory;
  private OperatorFactory operatorFactory;
  private ActionFactory actionFactory;
  private AlertFactory alertFactory;
  private ConcurrentMap<String, Cluster> clusters;
  private ActionAuditor actionAuditor;
  private ClusterStateSink stateSink;
  private MetricsStore metricsStore;
  private CostCalculator costCalculator;

  public ClusterManager(SensorFactory sensorFactory,
                        OperatorFactory operatorFactory,
                        ActionFactory actionFactory,
                        AlertFactory alertFactory,
                        ActionAuditor actionAuditor,
                        ClusterStateSink stateSink,
                        MetricsStore metricsStore,
                        CostCalculator costCalculator) {
    this.sensorFactory = sensorFactory;
    this.operatorFactory = operatorFactory;
    this.actionFactory = actionFactory;
    this.alertFactory = alertFactory;
    this.actionAuditor = actionAuditor;
    this.stateSink = stateSink;
    this.metricsStore = metricsStore;
    this.costCalculator = costCalculator;
    this.clusters = new ConcurrentSkipListMap<>();
  }

  public ConcurrentMap<String, Cluster> getClusters() {
    return clusters;
  }

  public ActionFactory getActionFactory() {
    return actionFactory;
  }

  public SensorFactory getSensorFactory() {
    return sensorFactory;
  }

  public OperatorFactory getOperatorFactory() {
    return operatorFactory;
  }

  public AlertFactory getAlertFactory() {
    return alertFactory;
  }

  public ActionAuditor getActionAuditor() {
    return actionAuditor;
  }

  public Cluster getCluster(String clusterId) {
    return clusters.get(clusterId);
  }

  public void addCluster(Cluster cluster) {
    clusters.put(cluster.getClusterId(), cluster);
  }

  public void addClusterPluginConfigs(String clusterId, OrionPluginConfig pluginConfig) throws Exception {
    actionFactory.addClusterConfigs(clusterId, pluginConfig.getActionConfigs());
    sensorFactory.addClusterConfigs(clusterId, pluginConfig.getSensorConfigs());
    operatorFactory.addClusterConfigs(clusterId, pluginConfig.getOperatorConfigs());
    alertFactory.addClusterConfigs(clusterId, pluginConfig.getAlertConfigs());
  }

  public ClusterStateSink getClusterStateSink() {
    return stateSink;
  }

  public void loadClusterActionsFromActionAuditor() {
    if(actionAuditor != null) {
      actionAuditor.loadActions(this);
    }
  }
  
  public CostCalculator getCostCalculator() {
    return costCalculator;
  }

  /**
   * @return the metricsStore
   */
  public MetricsStore getMetricsStore() {
    return metricsStore;
  }
  
}
