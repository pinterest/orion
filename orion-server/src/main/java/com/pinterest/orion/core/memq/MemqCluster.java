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
package com.pinterest.orion.core.memq;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.apache.curator.framework.CuratorFramework;
import org.apache.kafka.clients.admin.AdminClient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.ClusterStateSink;
import com.pinterest.orion.core.CostCalculator;
import com.pinterest.orion.core.Node;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.Utilization;
import com.pinterest.orion.core.actions.ActionFactory;
import com.pinterest.orion.core.actions.alert.AlertFactory;
import com.pinterest.orion.core.actions.audit.ActionAuditor;
import com.pinterest.orion.core.automation.operator.Operator;
import com.pinterest.orion.core.automation.sensor.Sensor;

public class MemqCluster extends Cluster {

  public static final String CLUSTER_INFO_DIR = "clusterInfoDir";
  public static final String SERVERSET_PATH = "serversetPath";
  public static final String ZK_CONNECTION_STRING = "zkConnectionString";
  private static final long serialVersionUID = 1L;
  private static final Logger logger = Logger.getLogger(MemqCluster.class.getName());
  private Map<String, Object> config;
  @JsonIgnore
  private transient CuratorFramework zkClient;
  @JsonIgnore
  private transient Map<String, AdminClient> readClusterClientMap = new ConcurrentHashMap<>();
  public static final String NOTIFICATION_BROKERSET = "notificationBrokerset";
  public static final String NOTIFICATION_STRIDE = "notificationStride";
  public static final String NOTIFICATION_CLUSTER_CONFIG = "notificationClusterConfig";

  public MemqCluster(String id,
                     String name,
                     List<Sensor> monitors,
                     List<Operator> operators,
                     ActionFactory actionFactory,
                     AlertFactory alertFactory,
                     ActionAuditor auditSink,
                     ClusterStateSink stateSink,
                     CostCalculator costCalculator) {
    super(id, name, "MemQ", monitors, operators, actionFactory, alertFactory, auditSink, stateSink,
        costCalculator);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void bootstrapClusterInfo(Map<String, Object> config) throws PluginConfigurationException {
    this.config = config;
    setAttribute(ZK_CONNECTION_STRING, config.get(ZK_CONNECTION_STRING));
    setAttribute(SERVERSET_PATH, config.get(SERVERSET_PATH));
    setAttribute(CLUSTER_INFO_DIR, config.get(CLUSTER_INFO_DIR));
    Map<String, String> notificationClusterConfig = (Map<String, String>) config
        .get(MemqCluster.NOTIFICATION_CLUSTER_CONFIG);
    setAttribute(NOTIFICATION_CLUSTER_CONFIG, notificationClusterConfig);
  }

  @Override
  protected Node getNodeInstance(NodeInfo info) {
    return new MemqBroker(this, info, new Properties());
  }

  @Override
  public void addNodeWithoutAgent(NodeInfo info) {
    getNodeMap().put(info.getNodeId(), getNodeInstance(info));
  }

  @Override
  public Map<String, Utilization> getUtilizationMap() {
    return new HashMap<>();
  }

  public Map<String, Object> getConfig() {
    return config;
  }

  public CuratorFramework getZkClient() {
    return zkClient;
  }

  public void setZkClient(CuratorFramework zkClient) {
    this.zkClient = zkClient;
  }

  public Map<String, AdminClient> getReadClusterClientMap() {
    return readClusterClientMap;
  }

  public void setReadClusterClientMap(Map<String, AdminClient> readClusterClientMap) {
    this.readClusterClientMap = readClusterClientMap;
  }

  @Override
  public Logger logger() {
    return logger;
  }

}
