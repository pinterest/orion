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

import java.io.Serializable;
import java.util.Map;

public class NodeInfo implements Serializable {

  private static final long serialVersionUID = 1L;
  private long timestamp;
  private String nodeId;
  private String hostname;
  private String ip;
  private String clusterId;
  private int servicePort;
  private long localtime;
  private String rack;
  private String nodeType;
  private Map<String, String> serviceInfo;
  private Map<String, String> agentSettings;
  private Map<String, String> environment;
  /**
   * @return the timestamp
   */
  public long getTimestamp() {
    return timestamp;
  }
  /**
   * @param timestamp the timestamp to set
   */
  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }
  /**
   * @return the nodeId
   */
  public String getNodeId() {
    return nodeId;
  }
  /**
   * @param nodeId the nodeId to set
   */
  public void setNodeId(String nodeId) {
    this.nodeId = nodeId;
  }
  /**
   * @return the hostname
   */
  public String getHostname() {
    return hostname;
  }
  /**
   * @param hostname the hostname to set
   */
  public void setHostname(String hostname) {
    this.hostname = hostname;
  }
  /**
   * @return the ip
   */
  public String getIp() {
    return ip;
  }
  /**
   * @param ip the ip to set
   */
  public void setIp(String ip) {
    this.ip = ip;
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
   * @return the servicePort
   */
  public int getServicePort() {
    return servicePort;
  }
  /**
   * @param servicePort the servicePort to set
   */
  public void setServicePort(int servicePort) {
    this.servicePort = servicePort;
  }
  /**
   * @return the localtime
   */
  public long getLocaltime() {
    return localtime;
  }
  /**
   * @param localtime the localtime to set
   */
  public void setLocaltime(long localtime) {
    this.localtime = localtime;
  }
  /**
   * @return the rack
   */
  public String getRack() {
    return rack;
  }
  /**
   * @param rack the rack to set
   */
  public void setRack(String rack) {
    this.rack = rack;
  }
  /**
   * @return the serviceInfo
   */
  public Map<String, String> getServiceInfo() {
    return serviceInfo;
  }
  /**
   * @param serviceInfo the serviceInfo to set
   */
  public void setServiceInfo(Map<String, String> serviceInfo) {
    this.serviceInfo = serviceInfo;
  }
  /**
   * @return the agentSettings
   */
  public Map<String, String> getAgentSettings() {
    return agentSettings;
  }
  /**
   * @param agentSettings the agentSettings to set
   */
  public void setAgentSettings(Map<String, String> agentSettings) {
    this.agentSettings = agentSettings;
  }
  /**
   * @return the environment
   */
  public Map<String, String> getEnvironment() {
    return environment;
  }
  /**
   * @param environment the environment to set
   */
  public void setEnvironment(Map<String, String> environment) {
    this.environment = environment;
  }

  public String getNodeType() {
    return nodeType;
  }

  public void setNodeType(String nodeType) {
    this.nodeType = nodeType;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "NodeInfo [nodeId=" + nodeId + ", hostname=" + hostname + ", ip=" + ip + ", clusterId="
        + clusterId + ", servicePort=" + servicePort + ", localtime="
        + localtime + ", rack=" + rack + ", serviceInfo=" + serviceInfo + ", agentSettings="
        + agentSettings + ", environment=" + environment + "]";
  }

}
