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
package com.pinterest.orion.server.api;

import java.util.List;
import java.util.stream.Collectors;

import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.alert.AlertMessage;

public class ClusterSummary {
  
  private String clusterId;
  private String name;
  private String type;
  private int nodes;
  private int agents;
  private List<AlertMessage> alerts;
  private List<Action> actions;
  private boolean isHealthy;
  private boolean isUnderMaintenance;
  
  public ClusterSummary(Cluster cluster) {
    this.clusterId = cluster.getClusterId();
    this.name = cluster.getClusterId();
    this.type = cluster.getType();
    this.nodes = cluster.getNodeMap().size();
    this.agents = (int) cluster.getNodeMap().values().stream().filter(n->n.isAgentPresent()).count();
    this.alerts = cluster.getActionEngine().getAlertsList().stream().filter(a -> !a.isRead())
            .collect(Collectors.toList());
    this.actions = cluster.getActionEngine().getTrackedActionsList();
    this.isHealthy = cluster.isHealthy();
    this.isUnderMaintenance = cluster.isUnderMaintenance();
  }
  
  /**
   * @return the alerts
   */
  public List<AlertMessage> getAlerts() {
    return alerts;
  }

  /**
   * @param alerts the alerts to set
   */
  public void setAlerts(List<AlertMessage> alerts) {
    this.alerts = alerts;
  }

  /**
   * @return the operations
   */
  public List<Action> getActions() {
    return actions;
  }

  /**
   * @param actions the actions to set
   */
  public void setActions(List<Action> actions) {
    this.actions = actions;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return the clusterId
   */
  public String getClusterId() {
    return clusterId;
  }
  /**
   * @param clusterId the clusterId to set
   */
  public void setClusterId(String clusterId) {
    this.clusterId = clusterId;
  }
  /**
   * @return the type
   */
  public String getType() {
    return type;
  }
  /**
   * @param type the type to set
   */
  public void setType(String type) {
    this.type = type;
  }
  /**
   * @return the nodeCount
   */
  public int getNodes() {
    return nodes;
  }
  /**
   * @param nodeCount the nodeCount to set
   */
  public void setNodes(int nodes) {
    this.nodes = nodes;
  }
  /**
   * @return the isHealthy
   */
  public boolean isHealthy() {
    return isHealthy;
  }
  /**
   * @param isHealthy the isHealthy to set
   */
  public void setHealthy(boolean isHealthy) {
    this.isHealthy = isHealthy;
  }

  /**
   * @return the agents
   */
  public int getAgents() {
    return agents;
  }

  /**
   * @param agents the agents to set
   */
  public void setAgents(int agents) {
    this.agents = agents;
  }

  /**
   * @return the underMaintenance
   */

  public boolean isUnderMaintenance() {
    return isUnderMaintenance;
  }

  /**
   * @param underMaintenance the underMaintenance to set
   */
  public void setUnderMaintenance(boolean underMaintenance) {
    isUnderMaintenance = underMaintenance;
  }
}
