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
package com.pinterest.orion.core.automation.sensor.memq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

import com.google.gson.Gson;
import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.memq.MemqCluster;
import com.pinterest.orion.utils.NetworkUtils;
import org.apache.zookeeper.KeeperException;

import static com.pinterest.orion.core.memq.MemqCluster.CLUSTER_CONTEXT;

public class MemqClusterSensor extends MemqSensor {

  public static final String WRITE_ASSIGNMENTS = "writeAssignments";
  public static final String TOPIC_CONFIG = "topicconfig";
  public static final String BROKERS = "/brokers";
  public static final String TOPICS = "/topics";
  public static final String GOVERNOR = "/governor";
  public static final String RAW_BROKER_INFO = "rawBrokerInfo";

  @Override
  public String getName() {
    return "Cluster Sensor";
  }

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    super.initialize(config);
  }

  @Override
  public void sense(MemqCluster cluster) throws Exception {
    try {
      if (cluster.getZkClient() == null) {
        String zkUrl = cluster.getAttribute(MemqCluster.ZK_CONNECTION_STRING).getValue();
        CuratorFramework curator = CuratorFrameworkFactory.newClient(zkUrl,
            new ExponentialBackoffRetry(1000, 3));
        curator.start();
        curator.blockUntilConnected();
        cluster.setZkClient(curator);
      }

      CuratorFramework zkClient = cluster.getZkClient();
      List<String> brokerNames = zkClient.getChildren().forPath(BROKERS);
      
      Map<String, List<String>> writeBrokerAssignments = new HashMap<>();
      
      Map<String, Broker> rawBrokerMap = new HashMap<>();

      Gson gson = new Gson();

      Set<String> brokersInZookeeper = new HashSet<>();
      for (String brokerName : brokerNames) {
        byte[] brokerData = null;
        try {
          brokerData = zkClient.getData().forPath(BROKERS + "/" + brokerName);
        } catch (KeeperException.NoNodeException e) {
          cluster.getNodeMap().remove(brokerName);
          logger.info(
              "Broker data of " + brokerName + " is not available in zookeeper. The broker might be removed.");
          continue;
        } catch (Exception e) {
          logger.severe(
              "Faced an unknown exception when getting broker data for " + brokerName +" from zookeeper:" + e);
          continue;
        }
        Broker broker = gson.fromJson(new String(brokerData), Broker.class);
        NodeInfo info = new NodeInfo();
        info.setClusterId(cluster.getClusterId());
        String hostname = NetworkUtils.getHostnameFromIpIfAvailable(broker.getBrokerIP());
        info.setHostname(hostname);
        info.setIp(broker.getBrokerIP());
        info.setNodeType(broker.getInstanceType());
        info.setNodeId(broker.getBrokerIP());
        info.setRack(broker.getLocality());
        info.setServicePort(broker.getBrokerPort());
        info.setTimestamp(System.currentTimeMillis());
        cluster.addNodeWithoutAgent(info);
        
        rawBrokerMap.put(broker.getBrokerIP(), broker);
        for (TopicConfig topicConfig : broker.getAssignedTopics()) {
          String topic = topicConfig.getTopic();
          List<String> hostnames = writeBrokerAssignments.get(topic);
          if (hostnames == null) {
            hostnames = new ArrayList<>();
            writeBrokerAssignments.put(topic, hostnames);
          }
          hostnames.add(hostname);
        }
        brokersInZookeeper.add(broker.getBrokerIP());
      }

      if (brokersInZookeeper.isEmpty()) {
        logger.warning("No broker found in zookeeper for cluster " + cluster.getClusterId());
      } else {
        // Remove brokers that are not in zookeeper from the cluster node map
        for (String nodeId : cluster.getNodeMap().keySet()) {
          if (!brokersInZookeeper.contains(nodeId)) {
            cluster.getNodeMap().remove(nodeId);
          }
        }
      }

      Map<String, TopicConfig> topicConfigMap = new HashMap<>();
      List<String> topics = zkClient.getChildren().forPath(TOPICS);
      for (String topic : topics) {
        byte[] topicData = zkClient.getData().forPath(TOPICS + "/" + topic);
        TopicConfig topicConfig = gson.fromJson(new String(topicData), TopicConfig.class);
        topicConfigMap.put(topic, topicConfig);
      }

      byte[] governorData = zkClient.getData().forPath(GOVERNOR);
      String governorIp = new String(governorData);
      String clusterContext = "Governor: " + governorIp + "\n";

      setAttribute(cluster, TOPIC_CONFIG, topicConfigMap);
      setAttribute(cluster, RAW_BROKER_INFO, rawBrokerMap);
      setAttribute(cluster, WRITE_ASSIGNMENTS, writeBrokerAssignments);
      setAttribute(cluster, CLUSTER_CONTEXT, clusterContext);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

}
