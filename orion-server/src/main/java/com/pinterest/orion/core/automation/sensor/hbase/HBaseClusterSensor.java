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
package com.pinterest.orion.core.automation.sensor.hbase;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.common.StatusInfo;
import com.pinterest.orion.common.StatusType;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.hbase.HBaseCluster;

public class HBaseClusterSensor extends HBaseSensor {

  private Map<String, Object> config;

  @Override
  public String getName() {
    return "hbaseregionserversensor";
  }

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    super.initialize(config);
  }

  @Override
  public void sense(HBaseCluster cluster) throws Exception {
    try {
      Connection connection = cluster.getConnection();
      if (connection == null) {
        connection = initializeHBaseConnect(cluster);
      }
      Admin admin = connection.getAdmin();

      Map<String, List<HRegionInfo>> serverRegionMap = new HashMap<>();
      // section covers region server fetch
      Collection<ServerName> liveRegionServers = admin.getClusterStatus().getServers();
      for (ServerName serverName : liveRegionServers) {
        NodeInfo info = new NodeInfo();
        info.setClusterId(cluster.getClusterId());
        info.setHostname(serverName.getHostname());
        info.setServicePort(serverName.getPort());
        info.setNodeId(generateHashedNodeId(serverName));
        cluster.addNodeWithoutAgent(info);
        cluster.getNodeMap().get(info.getNodeId()).setServiceStatus(new StatusInfo(StatusType.OK));
        List<HRegionInfo> onlineRegions = admin.getOnlineRegions(serverName);
        serverRegionMap.put(info.getNodeId(), onlineRegions);
      }
      cluster.setAttribute("regionserverOnlineRegions", serverRegionMap);

      for (ServerName serverName : admin.getClusterStatus().getDeadServerNames()) {
        NodeInfo info = new NodeInfo();
        info.setClusterId(cluster.getClusterId());
        info.setHostname(serverName.getHostname());
        info.setServicePort(serverName.getPort());
        info.setNodeId(generateHashedNodeId(serverName));
        cluster.addNodeWithoutAgent(info);
        cluster.getNodeMap().get(info.getNodeId())
            .setServiceStatus(new StatusInfo(StatusType.ERROR));
      }

      // table / namespace fetch
      Map<String, Map<String, HTableDescriptor>> tableDescriptorMap = new HashMap<>();
      TableName[] listTableNames = admin.listTableNames();
      for (TableName tableName : listTableNames) {
        HTableDescriptor tableDescriptor = admin.getTableDescriptor(tableName);
        Map<String, HTableDescriptor> map = tableDescriptorMap
            .get(tableName.getNamespaceAsString());
        if (map == null) {
          map = new HashMap<>();
          tableDescriptorMap.put(tableName.getNamespaceAsString(), map);
        }
        map.put(tableName.getNameAsString(), tableDescriptor);
      }
      cluster.setAttribute("namespaceTableDescriptor", tableDescriptorMap, getName());

    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  public static String generateHashedNodeId(ServerName server) {
    return String.valueOf(
        Hashing.sha256().hashString(server.getHostAndPort(), Charset.defaultCharset()).asLong());
  }

  private Connection initializeHBaseConnect(HBaseCluster cluster) throws PluginConfigurationException {
    String zkConnectionString = cluster.getAttribute(HBaseCluster.ZK_CONNECTION_STRING).getValue();
    String[] zkConnects = zkConnectionString.split(",");
    String[] zkConnect = zkConnects[0].split(":");
    String value = "/tmp/hbase+" + cluster.getClusterId();
    new File(value).mkdirs();
    System.setProperty("hadoop.home.dir", value);
    Configuration configuration = HBaseConfiguration.create();
    configuration.set("hbase.zookeeper.quorum", zkConnect[0]);
    configuration.set("hbase.zookeeper.property.clientPort", zkConnect[1]);
    if (cluster.getAttribute(HBaseCluster.HBASE_REGIONSERVER_PORT).getValue() != null) {
      configuration.set(HBaseCluster.HBASE_REGIONSERVER_PORT, String.valueOf(
          (Integer) cluster.getAttribute(HBaseCluster.HBASE_REGIONSERVER_PORT).getValue()));
    }
    if (cluster.getAttribute(HBaseCluster.HBASE_MASTER_PORT).getValue() != null) {
      configuration.set(HBaseCluster.HBASE_MASTER_PORT, String
          .valueOf((Integer) cluster.getAttribute(HBaseCluster.HBASE_MASTER_PORT).getValue()));
    }
    try {
      Connection connection = ConnectionFactory.createConnection(configuration);
      cluster.setConnection(connection);
      return connection;
    } catch (IOException e) {
      throw new PluginConfigurationException(e);
    }
  }
}
