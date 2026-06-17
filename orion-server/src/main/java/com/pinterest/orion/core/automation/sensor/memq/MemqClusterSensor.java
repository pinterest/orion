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

import com.pinterest.orion.core.utils.memq.zookeeper.MemqZookeeperClient;

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
    sense(cluster, new MemqZookeeperClient(cluster));
  }

  /**
   * Reads cluster state from ZooKeeper and publishes it as cluster attributes.
   *
   * Reads are defensive: the cluster's attributes and node map are only mutated once a complete,
   * trustworthy snapshot has been read. If the client is unhealthy or any read fails partway
   * through, this method leaves the previously published (known-good) state untouched instead of
   * overwriting it with empty, partial, or cross-cluster data.
   */
  void sense(MemqCluster cluster, MemqZookeeperClient memqZookeeperClient) throws Exception {
    if (!memqZookeeperClient.isConnected()) {
      logger.warning("ZooKeeper client for cluster " + cluster.getClusterId()
          + " is not connected; skipping update to avoid showing incorrect data.");
      return;
    }

    List<String> brokerNames;
    try {
      brokerNames = memqZookeeperClient.getBrokerNames();
    } catch (Exception e) {
      logger.warning("Failed to read broker list for cluster " + cluster.getClusterId()
          + "; skipping update to avoid showing incorrect data: " + e);
      return;
    }

    Gson gson = new Gson();
    Map<String, List<String>> writeBrokerAssignments = new HashMap<>();
    Map<String, Broker> rawBrokerMap = new HashMap<>();
    Set<String> brokersInZookeeper = new HashSet<>();
    List<NodeInfo> discoveredNodes = new ArrayList<>();
    Set<String> removedBrokers = new HashSet<>();

    for (String brokerName : brokerNames) {
      String brokerDataJsonString;
      try {
        brokerDataJsonString = memqZookeeperClient.getBrokerData(brokerName);
      } catch (KeeperException.NoNodeException e) {
        removedBrokers.add(brokerName);
        logger.info(
            "Broker data of " + brokerName + " is not available in zookeeper. The broker might be removed.");
        continue;
      } catch (Exception e) {
        // A transient read failure means we have an incomplete snapshot. Bail out rather than
        // silently dropping brokers and reporting NO BROKER / partial assignments.
        logger.warning("Failed to read broker data for " + brokerName + " in cluster "
            + cluster.getClusterId() + "; skipping update to avoid showing incorrect data: " + e);
        return;
      }
      Broker broker = gson.fromJson(brokerDataJsonString, Broker.class);
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
      discoveredNodes.add(info);

      rawBrokerMap.put(broker.getBrokerIP(), broker);
      for (TopicConfig topicConfig : broker.getAssignedTopics()) {
        writeBrokerAssignments.computeIfAbsent(topicConfig.getTopic(), k -> new ArrayList<>())
            .add(hostname);
      }
      brokersInZookeeper.add(broker.getBrokerIP());
    }

    Map<String, TopicConfig> topicConfigMap = new HashMap<>();
    try {
      for (String topicName : memqZookeeperClient.getTopics()) {
        String topicDataJsonString = memqZookeeperClient.getTopicData(topicName);
        topicConfigMap.put(topicName, gson.fromJson(topicDataJsonString, TopicConfig.class));
      }
    } catch (Exception e) {
      logger.warning("Failed to read topics for cluster " + cluster.getClusterId()
          + "; skipping update to avoid showing incorrect data: " + e);
      return;
    }

    // Snapshot is complete and trustworthy: apply it to the cluster.
    for (NodeInfo info : discoveredNodes) {
      cluster.addNodeWithoutAgent(info);
    }
    for (String removed : removedBrokers) {
      cluster.getNodeMap().remove(removed);
    }

    String clusterContext;
    if (brokersInZookeeper.isEmpty()) {
      logger.warning("No broker found in zookeeper for cluster " + cluster.getClusterId());
      clusterContext = "NO BROKER";
    } else {
      // Prune stale nodes only when we have a confirmed, non-empty broker list.
      cluster.getNodeMap().keySet().removeIf(nodeId -> !brokersInZookeeper.contains(nodeId));
      String governorIp = memqZookeeperClient.getGovernorIp();
      clusterContext = governorIp != null ? "Governor: " + governorIp + "\n" : "";
    }

    setAttribute(cluster, TOPIC_CONFIG, topicConfigMap);
    setAttribute(cluster, RAW_BROKER_INFO, rawBrokerMap);
    setAttribute(cluster, WRITE_ASSIGNMENTS, writeBrokerAssignments);
    setAttribute(cluster, CLUSTER_CONTEXT, clusterContext);
  }

}
