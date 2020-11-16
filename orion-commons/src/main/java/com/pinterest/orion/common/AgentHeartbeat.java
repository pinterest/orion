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
package com.pinterest.orion.common;

public class AgentHeartbeat {
  
  private NodeInfo nodeInfo;
  private StatusInfo agentStatus;
  private StatusInfo serviceStatus;
  private CmdResult currentCmdResult;
  private Metrics metrics;
  private boolean containsMetrics = false;
  private boolean readOnly;
  
  /**
   * @return the nodeInfo
   */
  public NodeInfo getNodeInfo() {
    return nodeInfo;
  }
  /**
   * @param nodeInfo the nodeInfo to set
   */
  public void setNodeInfo(NodeInfo nodeInfo) {
    this.nodeInfo = nodeInfo;
  }
  /**
   * @return the currentCmdResult
   */
  public CmdResult getCurrentCmdResult() {
    return currentCmdResult;
  }
  /**
   * @param currentCmdResult the currentCmdResult to set
   */
  public void setCurrentCmdResult(CmdResult currentCmdResult) {
    this.currentCmdResult = currentCmdResult;
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
  /**
   * @return the metrics
   */
  public Metrics getMetrics() {
    return metrics;
  }
  /**
   * @param metrics the metrics to set
   */
  public void setMetrics(Metrics metrics) {
    this.metrics = metrics;
    setContainsMetrics(true);
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public void setReadOnly(boolean readOnly) {
    this.readOnly = readOnly;
  }

  public boolean isContainsMetrics() {
    return containsMetrics;
  }

  public void setContainsMetrics(boolean containsMetrics) {
    this.containsMetrics = containsMetrics;
  }
}