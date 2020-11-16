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

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pinterest.orion.common.AgentHeartbeat;
import com.pinterest.orion.common.CmdResult;
import com.pinterest.orion.common.NodeCmd;
import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.core.actions.ActionEngine;
import com.pinterest.orion.core.actions.ActionFactory;
import com.pinterest.orion.core.actions.audit.ActionAuditor;
import com.pinterest.orion.core.automation.AutomationEngine;
import com.pinterest.orion.core.automation.operator.Operator;
import com.pinterest.orion.core.automation.sensor.Sensor;

/**
 * {@link Cluster} is the inmemory representation of the Cluster. This is an
 * abstract class and concrete implementation of this class will determines how
 * the cluster info is populated.
 */
public abstract class Cluster extends State implements Plugin, Serializable {

  private static final long serialVersionUID = 1L;
  public static final String ATTR_METADATA_UPDATE_TIME = "mdupdatetime";
  public static final String ATTR_CONF_KEY = "conf";

  protected String clusterId;
  protected String name;
  protected String type;
  private Map<String, Node> nodeMap;
  private ActionEngine actionEngine;
  private AutomationEngine automationEngine;
  private ClusterStateSink stateSink;
  @JsonIgnore
  private AtomicBoolean maintenance = new AtomicBoolean(false);
  @JsonIgnore
  protected CostCalculator costCalculator;

  public Cluster(String id,
                 String name,
                 String type,
                 List<Sensor> monitors,
                 List<Operator> operators,
                 ActionFactory actionFactory,
                 ActionAuditor auditSink,
                 ClusterStateSink stateSink,
                 CostCalculator costCalculator) {
    this.clusterId = id;
    this.name = name;
    this.type = type;
    this.stateSink = stateSink;
    this.costCalculator = costCalculator;
    this.nodeMap = new ConcurrentHashMap<>();
    this.actionEngine = new ActionEngine(this, actionFactory, auditSink);
    this.automationEngine = new AutomationEngine(this, monitors, operators);
  }

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    setAttribute(ATTR_CONF_KEY, config);
    try {
      bootstrapClusterInfo(config);
    } catch (PluginConfigurationException ce) {
      throw ce;
    } catch (Exception e) {
      throw new PluginConfigurationException(e);
    }
    actionEngine.initialize(config);
    actionEngine.start();
    automationEngine.initialize(config);
    if (stateSink != null) {
      Cluster pastState = stateSink.deserialize(clusterId);
      if (pastState != null) {
        nodeMap.putAll(pastState.getNodeMap());
      } else {
        logger().log(Level.WARNING, "No past state for the cluster, skipping state restoration");
      }
    }
  }

  protected abstract void bootstrapClusterInfo(Map<String, Object> config) throws PluginConfigurationException;

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
   * @return the nodeMap
   */
  public Map<String, Node> getNodeMap() {
    return nodeMap;
  }

  /**
   * @param nodeMap the nodeMap to set
   */
  public void setNodeMap(ConcurrentMap<String, Node> nodeMap) {
    this.nodeMap = nodeMap;
  }

  public NodeCmd updateNodeFromAgentHeartbeat(AgentHeartbeat heartbeat) throws IOException {
    NodeInfo nodeInfo = heartbeat.getNodeInfo();
    Node node = getNodeMap().get(nodeInfo.getNodeId());
    if (node == null) {
      synchronized (getNodeMap()) {
        node = getNodeMap().get(nodeInfo.getNodeId());
        if (node == null) {
          node = getNodeInstance(nodeInfo);
          getNodeMap().put(nodeInfo.getNodeId(), node);
        }
      }
      logger().info("Registered node:" + nodeInfo.getNodeId());
    }
    try {
      node.setAgentNodeInfo(nodeInfo);
      node.getAgentNodeInfo().setTimestamp(System.currentTimeMillis());
      if(heartbeat.isContainsMetrics()){
        node.setCurrentNodeMetrics(heartbeat.getMetrics());
      }
      node.setAgentStatus(heartbeat.getAgentStatus());
      node.setServiceStatus(heartbeat.getServiceStatus());
      CmdResult currentCmdResult = heartbeat.getCurrentCmdResult();
      if (currentCmdResult != null) {
        if (!node.getCmdQueue().isEmpty()
            && node.getCmdQueue().peek().getUuid().equalsIgnoreCase(currentCmdResult.getUuid())) {
          CmdResult result = node.getCmdQueue().peek().getResult();
          result.setErr(currentCmdResult.getErr());
          result.setOut(currentCmdResult.getOut());
          result.setExitCode(currentCmdResult.getExitCode());
          result.setState(currentCmdResult.getState());
        }
      }
    } catch (Exception e) {
      logger().log(Level.SEVERE, "Node connection failed:" + heartbeat, e);
    }
    if (heartbeat.isReadOnly()) {
      if (!node.getCmdQueue().isEmpty()) {
        CmdResult result = node.getCmdQueue().peek().getResult();
        result.setErr("Agent is in read-only mode.");
        result.setExitCode((short) -1);
        result.setState(CmdResult.CmdState.COMPLETED);
      }
      return null;
    } else {
      return node.getCmdQueue().isEmpty() ? null : node.getCmdQueue().peek();
    }
  }

  protected abstract Node getNodeInstance(NodeInfo info);

  public abstract void addNodeWithoutAgent(NodeInfo info);

  public ActionEngine getActionEngine() {
    return actionEngine;
  }

  public AutomationEngine getAutomationEngine() {
    return automationEngine;
  }

  public boolean isHealthy() {
    return true;
  }

  @JsonIgnore
  public boolean clusterHealthy() {
    return true;
  }

  public abstract Logger logger();

  public void checkpointState() {
    stateSink.serialize(this);
  }

  @JsonIgnore
  public abstract Map<String, Utilization> getUtilizationMap();

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Cluster [clusterId=" + clusterId + ", type=" + type + ", nodeMap=" + nodeMap
        + ", actionEngine=" + actionEngine + "]";
  }

  @Override
  public String getName() {
    return clusterId;
  }

  public boolean isUnderMaintenance() {
    return maintenance.get();
  }

  public void setMaintenance(boolean maintenance) {
    this.maintenance.set(maintenance);
  }

  @JsonIgnore
  public ClusterCost getCostMap() {
    return new ClusterCost();
  }
}
