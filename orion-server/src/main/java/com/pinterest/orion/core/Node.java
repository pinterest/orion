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

import java.io.Serializable;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pinterest.orion.common.Metrics;
import com.pinterest.orion.common.NodeCmd;
import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.common.StatusInfo;
import com.pinterest.orion.common.StatusType;

public abstract class Node implements Serializable {

  public static enum NodeStatus {
                                 COMMISSIONED,
                                 MAINTENANCE,
                                 DECOMMISSIONED
  }

  private static final Logger logger = Logger.getLogger(Node.class.getCanonicalName());
  private static final long serialVersionUID = 1L;

  protected volatile NodeInfo currentNodeInfo;
  protected volatile NodeInfo agentNodeInfo;
  protected volatile Metrics currentNodeMetrics;
  protected NodeStatus nodeStatus;
  private StatusInfo agentStatus;
  private StatusInfo serviceStatus;
  private volatile boolean maintenance = false;
  @JsonIgnore
  private Cluster cluster;
  private BlockingQueue<NodeCmd> cmdQueue;

  public Node(Cluster cluster, NodeInfo currentNodeInfo, Properties connectionProps) {
    this.cluster = cluster;
    this.currentNodeInfo = currentNodeInfo;
    this.cmdQueue = new ArrayBlockingQueue<>(1);
  }

  public Cluster getCluster() {
    return cluster;
  }

  public String getVersion() {
    return "N/A";
  }

  /**
   * @return the currentNodeInfo
   */
  public NodeInfo getCurrentNodeInfo() {
    return currentNodeInfo;
  }

  /**
   * @return
   */
  public NodeInfo getAgentNodeInfo() {
    return agentNodeInfo;
  }

  /**
   * @return the currentNodeMetrics
   */
  @JsonIgnore
  public Metrics getCurrentNodeMetrics() {
    return currentNodeMetrics;
  }

  public void setCurrentNodeInfo(NodeInfo currentNodeInfo) {
    this.currentNodeInfo = currentNodeInfo;
  }

  public void setAgentNodeInfo(NodeInfo agentNodeInfo) {
    this.agentNodeInfo = agentNodeInfo;
  }

  public StatusInfo serviceStatus() {
    return serviceStatus;
  }

  public StatusInfo agentStatus() {
    return agentStatus;
  }

  public NodeCmd runGenericCommand(String cmdUuid, String cmd, long waitTimeout) throws Exception {
    logger.info("Triggered new command on node:" + currentNodeInfo.getNodeId() + " " + cmd);
    NodeCmd cmdObj = new NodeCmd(cmdUuid, cmd);
    cmdQueue.offer(cmdObj, waitTimeout, TimeUnit.SECONDS);
    return cmdObj;
  }

  public NodeCmd restartService(String cmdUuid, long timeout) throws Exception {
    return runGenericCommand(cmdUuid, NodeCmd.RESTART_CMD, timeout);
  }

  public NodeCmd startService(String cmdUuid, long timeout) throws Exception {
    return runGenericCommand(cmdUuid, NodeCmd.START_CMD, timeout);
  }

  public NodeCmd stopService(String cmdUuid, long timeout) throws Exception {
    return runGenericCommand(cmdUuid, NodeCmd.STOP_CMD, timeout);
  }

  public NodeCmd updateConfigs(String cmdUuid, long timeout) throws Exception {
    return runGenericCommand(cmdUuid, NodeCmd.UPDATE_CONFIG_CMD, timeout);
  }

  public void clearActionQueue() throws InterruptedException {
    if (!cmdQueue.isEmpty()) {
      logger.info("Removed cmd from queue:" + currentNodeInfo.getNodeId());
      cmdQueue.take();
    }
  }

  public BlockingQueue<NodeCmd> getCmdQueue() {
    return cmdQueue;
  }

  /**
   * @return the nodeStatus
   */
  public NodeStatus getNodeStatus() {
    return nodeStatus;
  }

  /**
   * @param nodeStatus the nodeStatus to set
   */
  public void setNodeStatus(NodeStatus nodeStatus) {
    this.nodeStatus = nodeStatus;
  }

  /**
   * @param currentNodeMetrics the currentNodeMetrics to set
   */
  public void setCurrentNodeMetrics(Metrics currentNodeMetrics) {
    this.currentNodeMetrics = currentNodeMetrics;
  }

  public boolean isAgentPresent() {
    return getAgentNodeInfo() != null
        && System.currentTimeMillis() - getAgentNodeInfo().getLocaltime() < 30000;
  }

  @JsonIgnore
  public boolean isAgentHealthy() {
    return isAgentPresent() &&
        (agentStatus.getStatusType() == StatusType.OK ||
         agentStatus.getStatusType() == StatusType.UPGRADE
        );
  }

  public boolean isServiceHealthy(long lastTimestamp) {
    return serviceStatus != null && agentNodeInfo.getTimestamp() >= lastTimestamp
        && serviceStatus.getStatusType() == StatusType.OK;
  }

  @Override
  public String toString() {
    return cluster.getName() + ":" + currentNodeInfo.getHostname();
  }

  /**
   * @return the agentStatus
   */
  public StatusInfo getAgentStatus() {
    return agentStatus;
  }

  /**
   * @param agentStatus the agentStatus to set
   */
  public void setAgentStatus(StatusInfo agentStatus) {
    this.agentStatus = agentStatus;
  }

  /**
   * @return the serviceStatus
   */
  public StatusInfo getServiceStatus() {
    return serviceStatus;
  }

  /**
   * @param serviceStatus the serviceStatus to set
   */
  public void setServiceStatus(StatusInfo serviceStatus) {
    this.serviceStatus = serviceStatus;
  }

  public boolean isUnderMaintenance() {
    return maintenance;
  }

  public void setMaintenance(boolean maintenance) {
    this.maintenance = maintenance;
  }
}
