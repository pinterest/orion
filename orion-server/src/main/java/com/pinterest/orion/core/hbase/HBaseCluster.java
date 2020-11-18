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
package com.pinterest.orion.core.hbase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.hadoop.hbase.client.Connection;

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

public class HBaseCluster extends Cluster {

  public static final String HBASE_REGIONSERVER_PORT = "hbase.regionserver.port";
  public static final String HBASE_MASTER_PORT = "hbase.master.port";
  public static final String ZK_CONNECTION_STRING = "zkConnectionString";
  private static final long serialVersionUID = 1L;
  private static final Logger logger = Logger.getLogger(HBaseCluster.class.getName());
  private Map<String, Object> config;
  @JsonIgnore
  private transient Connection connection;

  public HBaseCluster(String id,
                      String name,
                      List<Sensor> monitors,
                      List<Operator> operators,
                      ActionFactory actionFactory,
                      AlertFactory alertFactory,
                      ActionAuditor auditSink,
                      ClusterStateSink stateSink,
                      CostCalculator costCalculator) {
    super(id, name, "HBase", monitors, operators, actionFactory, alertFactory, auditSink, stateSink, costCalculator);
  }

  @Override
  protected void bootstrapClusterInfo(Map<String, Object> config) throws PluginConfigurationException {
    this.config = config;
    setAttribute(ZK_CONNECTION_STRING, config.get(ZK_CONNECTION_STRING));
    setAttribute(HBASE_MASTER_PORT, config.get(HBASE_MASTER_PORT));
    setAttribute(HBASE_REGIONSERVER_PORT, config.get(HBASE_REGIONSERVER_PORT));
  }

  @Override
  protected Node getNodeInstance(NodeInfo info) {
    return new RegionServerNode(this, info, new Properties());
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

  @Override
  public Logger logger() {
    return logger;
  }
  
  public Connection getConnection() {
    return connection;
  }
  
  public void setConnection(Connection connection) {
    this.connection = connection;
  }

}
