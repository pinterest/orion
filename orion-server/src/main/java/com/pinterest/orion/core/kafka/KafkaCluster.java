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
package com.pinterest.orion.core.kafka;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.pinterest.orion.core.Attribute;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.requests.DescribeLogDirsResponse.ReplicaInfo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.ClusterCost;
import com.pinterest.orion.core.ClusterCost.EntityCost;
import com.pinterest.orion.core.ClusterStateSink;
import com.pinterest.orion.core.CostCalculator;
import com.pinterest.orion.core.Node;
import com.pinterest.orion.core.Utilization;
import com.pinterest.orion.core.actions.ActionFactory;
import com.pinterest.orion.core.actions.alert.AlertFactory;
import com.pinterest.orion.core.actions.audit.ActionAuditor;
import com.pinterest.orion.core.automation.operator.Operator;
import com.pinterest.orion.core.automation.sensor.Sensor;
import com.pinterest.orion.core.automation.sensor.kafka.KafkaTopicSensor;


public class KafkaCluster extends Cluster {

  // These cluster attributes are used to define the timeout values of the Kafka AdminClient API calls.
  // If cluster does not have required attributes, the AdminClient call will use default timeout values.
  //    The get methods will return -1.
  // KafkaAdminClientClusterRequestTimeoutMs is used for AdminClient cluster/broker config APIs.
  //    Ex: describeConfigs and describeCluster
  // KafkaAdminClientTopicRequestTimeoutMs is used for AdminClient topic/partition related APIs.
  //    Ex: describeLogDirs and listOffsets
  // KafkaAdminClientConsumerGroupRequestTimeoutMs is used for AdminClient consumer group related APIs.
  //    Ex: listConsumerGroups, describeConsumerGroups and listConsumerGroupOffsets
  protected static final String ATTR_KAFKA_ADMIN_CLIENT_CLUSTER_REQUEST_TIMEOUT_MILLISECONDS_KEY =
          "kafkaAdminClientClusterRequestTimeoutMs";
  protected static final String ATTR_KAFKA_ADMIN_CLIENT_TOPIC_REQUEST_TIMEOUT_MILLISECONDS_KEY =
          "kafkaAdminClientTopicRequestTimeoutMs";
  protected static final String ATTR_KAFKA_ADMIN_CLIENT_CONSUMER_GROUP_REQUEST_TIMEOUT_MILLISECONDS_KEY =
          "kafkaAdminClientConsumerGroupRequestTimeoutMs";

  private static final long serialVersionUID = 1L;
  private static final Logger logger = Logger.getLogger(KafkaCluster.class.getCanonicalName());
  public static final String ZOOKEEPER_CONNECT = "zookeeper.connect";
  public static final long DEFAULT_METADATA_TIMEOUT_MS = 30_000L;
  private transient Properties props;
  private AdminClient adminClient;
  private KafkaConsumer<byte[], byte[]> kafkaConsumer;
  public static final String ATTR_EBS_VOLUME_SIZE_KEY = "ebs_volume_size";

  public KafkaCluster(String clusterId,
                      String name,
                      List<Sensor> monitors,
                      List<Operator> operators,
                      ActionFactory actionFactory,
                      AlertFactory alertFactory,
                      ActionAuditor historySink,
                      ClusterStateSink stateSink,
                      CostCalculator costCalculator) {
    super(clusterId, name, "Kafka", monitors, operators, actionFactory, alertFactory, historySink,
        stateSink, costCalculator);
  }

  @Override
  public boolean clusterHealthy() {
    try {
      Map<String, KafkaTopicDescription> urps = getURPFromClusters();
      return urps.isEmpty();
    } catch (Exception e) {
      logger().log(Level.SEVERE, "Failed to fetch Kafka URP status", e);
      return false;
    }
  }

  public boolean brokerHealthy(String brokerId) {
    try {
      return getURPFromClusters().values().stream().flatMap(ktd -> ktd.getPartitions().stream())
          .filter(ktpi -> ktpi.getReplicas().stream().anyMatch(n -> n.idString().equals(brokerId)))
          .allMatch(ktpi -> ktpi.getIsrs().stream().anyMatch(n -> n.idString().equals(brokerId)));
    } catch (Exception e) {
      logger().log(Level.SEVERE, "Failed to fetch broker URP status", e);
      return false;
    }
  }

  @JsonIgnore
  public int getClusterDefaultReplicationFactor() {
    int defaultRf = -1;
    try {
      DescribeClusterOptions describeClusterOptions = new DescribeClusterOptions();
      int kafkaAdminClientClusterRequestTimeoutMs = getKafkaAdminClientClusterRequestTimeoutMilliseconds();
      if (kafkaAdminClientClusterRequestTimeoutMs > 0) {
        describeClusterOptions.timeoutMs(kafkaAdminClientClusterRequestTimeoutMs);
      }
      Set<Integer> nodeIds = adminClient.describeCluster(describeClusterOptions).nodes().get().stream()
          .map(org.apache.kafka.common.Node::id).collect(Collectors.toSet());
      ConfigResource brokerConfigResource = new ConfigResource(ConfigResource.Type.BROKER,
          nodeIds.iterator().next().toString());
      DescribeConfigsResult defaultRfConfig = adminClient
          .describeConfigs(Collections.singletonList(brokerConfigResource));
      defaultRf = Integer.parseInt(defaultRfConfig.all().get().get(brokerConfigResource)
          .get("default.replication.factor").value());
    } catch (Exception e) {
    }
    return defaultRf;
  }

  @Override
  public void bootstrapClusterInfo(Map<String, Object> config) {
    props = new Properties();
  }

  public void addBrokerset(Brokerset brokerset) {
    Map<String, Brokerset> map = getAttribute("brokersets").getValue();
    if (!map.containsKey(brokerset.getBrokersetAlias())) {
      map.put(brokerset.getBrokersetAlias(), brokerset);
    } else {
      throw new IllegalArgumentException("Brokerset with this alias already exists");
    }
  }

  @Override
  protected Node getNodeInstance(NodeInfo info) {
    return new KafkaBroker(this, info, props);
  }

  @Override
  public void addNodeWithoutAgent(NodeInfo info) {
    getNodeMap().putIfAbsent(info.getNodeId(), new KafkaBroker(this, info, props));
  }

  private Map<String, KafkaTopicDescription> getURP(Map<String, KafkaTopicDescription> clusterMD) {
    Map<String, KafkaTopicDescription> topicDescriptions = new HashMap<>();
    for (Entry<String, KafkaTopicDescription> entry : clusterMD.entrySet()) {
      KafkaTopicDescription originalDescription = entry.getValue();
      for (KafkaTopicPartitionInfo topicPartitionInfo : originalDescription.getPartitions()) {
        if (topicPartitionInfo.getIsrs().size() != topicPartitionInfo.getReplicas().size()) {
          KafkaTopicDescription topicDescription = topicDescriptions.get(entry.getKey());
          if (topicDescription == null) {
            topicDescription = new KafkaTopicDescription(originalDescription.getName(),
                originalDescription.isInternal(), new HashMap<>());
            topicDescriptions.put(entry.getKey(), topicDescription);
          }
          topicDescription.partitionMap().put(topicPartitionInfo.getPartition(),
              topicPartitionInfo);
        }
      }
    }
    return topicDescriptions;
  }

  public Map<String, KafkaTopicDescription> getURP() {
    if (!containsAttribute(KafkaTopicSensor.ATTR_TOPICINFO_MAP_KEY)) {
      return null;
    }
    Map<String, KafkaTopicDescription> attribute = getAttribute(
        KafkaTopicSensor.ATTR_TOPICINFO_MAP_KEY).getValue();
    return getURP(attribute);
  }

  @JsonIgnore
  public Map<String, KafkaTopicDescription> getTopicDescriptionFromKafka() throws InterruptedException,
                                                                                  ExecutionException, TimeoutException {
    int kafkaAdminClientTopicRequestTimeoutMs = getKafkaAdminClientTopicRequestTimeoutMilliseconds();
    if (kafkaAdminClientTopicRequestTimeoutMs > 0) {
      return getTopicDescriptionFromKafka(kafkaAdminClientTopicRequestTimeoutMs);
    }
    return getTopicDescriptionFromKafka(DEFAULT_METADATA_TIMEOUT_MS);
  }

  @JsonIgnore
  public Map<String, KafkaTopicDescription> getTopicDescriptionFromKafka(long metadataFetchTimeoutMs)
          throws ExecutionException, InterruptedException, TimeoutException {
    AdminClient adminClient = getAdminClient();
    Map<String, KafkaTopicDescription> cachedTopicMap = containsAttribute(KafkaTopicSensor.ATTR_TOPICINFO_MAP_KEY)
            ? getAttribute(KafkaTopicSensor.ATTR_TOPICINFO_MAP_KEY).getValue()
            : null;
    Map<String, KafkaTopicDescription> ret = getTopicDescriptions(adminClient, logger(),
                                                                  cachedTopicMap,
                                                                  clusterId,
                                                                  metadataFetchTimeoutMs);
    // Set the topic cache if the cache has not been created by KafkaTopicSensor.
    // It will be refreshed by KafkaTopicSensor.
    if (!containsAttribute(KafkaTopicSensor.ATTR_TOPICINFO_MAP_KEY) && ret != null) {
      setAttribute(KafkaTopicSensor.ATTR_TOPICINFO_MAP_KEY, ret);
    }
    return ret;
  }

  public static Map<String, KafkaTopicDescription> getTopicDescriptions(AdminClient adminClient,
                                                                        Logger logger,
                                                                        Map<String, KafkaTopicDescription> cachedTopicMap,
                                                                        String clusterId,
                                                                        long metadataFetchTimeoutMs) throws InterruptedException,
                                                                                          ExecutionException, TimeoutException {
    long start = System.currentTimeMillis();
    Map<String, KafkaTopicDescription> ret = new HashMap<>();
    Set<String> topics;
    try {
      topics = adminClient.listTopics(new ListTopicsOptions().listInternal(true)).names().get(metadataFetchTimeoutMs, TimeUnit.MILLISECONDS);
    } catch (ExecutionException e) {
      if (cachedTopicMap != null) {
        logger.log(Level.WARNING,
            "Failed to list topics for " + clusterId + ", fallback to previous topic data.", e);
        topics = cachedTopicMap.keySet();
      } else {
        throw e;
      }
    }
    long list = System.currentTimeMillis();
    if (topics.isEmpty()) {
      return ret;
    }
    List<Future<Map<String, TopicDescription>>> futures = Lists
        .partition(new ArrayList<>(topics), Integer.min(topics.size(), 10)).stream()
        .map(subset -> adminClient.describeTopics(subset).all()).collect(Collectors.toList());
    for (Future<Map<String, TopicDescription>> f : futures) {
      ret.putAll(f.get(metadataFetchTimeoutMs, TimeUnit.MILLISECONDS).entrySet().stream().collect(
          Collectors.toMap(Entry::getKey, entry -> new KafkaTopicDescription(entry.getValue()))));
    }
    long desc = System.currentTimeMillis();
    logger.info(
        String.format("%s:getTopicDescriptionFromKafka: ListTopics %d ms, DescribeTopics %d ms",
            clusterId, list - start, desc - list));
    return ret;
  }

  @JsonIgnore
  public Map<String, KafkaTopicDescription> getURPFromClusters() throws InterruptedException,
                                                                        ExecutionException, TimeoutException {
    Map<String, KafkaTopicDescription> clusterMD = getTopicDescriptionFromKafka();
    return getURP(clusterMD);
  }

  @JsonIgnore
  public AdminClient getAdminClient() {
    return adminClient;
  }

  public void setAdminClient(AdminClient adminClient) {
    this.adminClient = adminClient;
  }

  @JsonIgnore
  public KafkaConsumer<byte[], byte[]> getKafkaConsumer() {
    return kafkaConsumer;
  }

  public void setKafkaConsumer(KafkaConsumer<byte[], byte[]> kafkaConsumer) {
    this.kafkaConsumer = kafkaConsumer;
  }

  @Override
  public boolean isHealthy() {
    return getURP() != null ? getURP().isEmpty() : false;
  }

  @Override
  @JsonIgnore
  public Map<String, Utilization> getUtilizationMap() {
    Map<String, Utilization> utilizationMap = new HashMap<>();
    if (containsAttribute(KafkaTopicSensor.ATTR_TOPICINFO_MAP_KEY)) {
      Map<String, KafkaTopicDescription> topicDescriptionMap = getAttribute(
          KafkaTopicSensor.ATTR_TOPICINFO_MAP_KEY).getValue();
      for (Entry<String, KafkaTopicDescription> entry : topicDescriptionMap.entrySet()) {
        KafkaTopicDescription value = entry.getValue();
        Map<String, String> topicConfigs = value.getTopicConfigs();
        if (topicConfigs != null) {
          long retentionSeconds = Long.parseLong(topicConfigs.get("retention.ms")) / 1000;
          if (retentionSeconds <= 0) {
            retentionSeconds = 1;
          }
          long totalTopicSize = 0;
          for (KafkaTopicPartitionInfo partition : value.getPartitions()) {
            for (ReplicaInfo replicaInfo : partition.getReplicaInfo().values()) {
              totalTopicSize += replicaInfo.size;
            }
          }
          totalTopicSize = totalTopicSize / 1024 / 1024;
          double sizePerSecond = ((double) totalTopicSize) / retentionSeconds;
          double networkUtilizationInMBPerSecond = sizePerSecond;
          utilizationMap.put(entry.getKey(),
              new Utilization(networkUtilizationInMBPerSecond, totalTopicSize));
        }
      }
    }
    return utilizationMap;
  }

  @Override
  @JsonIgnore
  public ClusterCost getCostMap() {
    ClusterCost clusterCost = new ClusterCost();
    if (costCalculator != null) {
      Map<String, EntityCost> costMap = clusterCost.getEntityCostMap();
      if (containsAttribute(KafkaTopicSensor.ATTR_TOPICINFO_MAP_KEY)) {
        Map<String, KafkaTopicDescription> topicDescriptionMap = getAttribute(
            KafkaTopicSensor.ATTR_TOPICINFO_MAP_KEY).getValue();
        for (Entry<String, KafkaTopicDescription> entry : topicDescriptionMap.entrySet()) {
          KafkaTopicDescription value = entry.getValue();
          Map<String, String> topicConfigs = value.getTopicConfigs();
          if (topicConfigs != null) {
            long retentionSeconds = Long.parseLong(topicConfigs.get("retention.ms")) / 1000;
            if (retentionSeconds <= 0) {
              retentionSeconds = 1;
            }
            int replicationFactor = 0;
            long totalTopicSize = 0;
            Iterator<KafkaTopicPartitionInfo> iterator = value.getPartitions().iterator();
            double diskCapacityForNodeType = 1;
            double costForNodeType = 1;
            if (iterator.hasNext()) {
              org.apache.kafka.common.Node leader = iterator.next().getLeader();
              Node node = getNodeMap().get(String.valueOf(leader.id()));
              String nodeType = "default";
              if (node != null && node.getAgentNodeInfo() != null
                  && node.getAgentNodeInfo().getNodeType() != null) {
                nodeType = node.getAgentNodeInfo().getNodeType();
              }
              diskCapacityForNodeType = costCalculator.getDiskCapacityForNodeTypeInGB(nodeType);
              costForNodeType = costCalculator.getCostForNodeType(nodeType);
            }
            for (KafkaTopicPartitionInfo partition : value.getPartitions()) {
              replicationFactor += partition.getReplicaInfo().size();
              for (ReplicaInfo replicaInfo : partition.getReplicaInfo().values()) {
                totalTopicSize += replicaInfo.size;
              }
            }
            replicationFactor = replicationFactor / value.getPartitions().size();
            totalTopicSize = totalTopicSize / 1024 / 1024;
            double sizePerSecond = ((double) totalTopicSize) / retentionSeconds;
            double networkUtilizationInMBPerSecond = sizePerSecond;
            double mbInPerSecond = networkUtilizationInMBPerSecond / replicationFactor;

            // cost of replication + inbound
            double crossRackTrafficCost = (0.66 + replicationFactor - 1) * mbInPerSecond
                * costCalculator.getNetworkCost();
            // calculate fraction of disk used
            costMap.put(entry.getKey(), new EntityCost(crossRackTrafficCost,
                totalTopicSize * costForNodeType / 1024 / diskCapacityForNodeType));
          }
        }
      }
      clusterCost.setNodeCost(costCalculator.getNodeCostForCluster(this));
    }
    return clusterCost;
  }

  @Override
  public Logger logger() {
    return logger;
  }

  private Map<String, Object> getClusterConfMap() {
    if (containsAttribute(ATTR_CONF_KEY)) {
      Attribute confAttribute = getAttribute(Cluster.ATTR_CONF_KEY);
      if (confAttribute != null) {
        Map<String, Object> confMap = confAttribute.getValue();
        if (confMap != null) {
          return confMap;
        }
      }
    }
    return new HashMap<>();
  }

  public int getKafkaAdminClientClusterRequestTimeoutMilliseconds() {
    Map<String, Object> clusterConfMap = getClusterConfMap();
    int timeoutMs = -1;
    if (clusterConfMap.containsKey(ATTR_KAFKA_ADMIN_CLIENT_CLUSTER_REQUEST_TIMEOUT_MILLISECONDS_KEY)) {
      Object timeoutObject = clusterConfMap.get(ATTR_KAFKA_ADMIN_CLIENT_CLUSTER_REQUEST_TIMEOUT_MILLISECONDS_KEY);
      if (timeoutObject != null) {
        if (timeoutObject instanceof Integer) {
          timeoutMs = (int) timeoutObject;
          logger.log(Level.INFO,
                  "getKafkaAdminClientClusterRequestTimeoutMilliseconds returns timeout value: " + timeoutMs);
        } else {
          logger.log(Level.WARNING,
                  ATTR_KAFKA_ADMIN_CLIENT_CLUSTER_REQUEST_TIMEOUT_MILLISECONDS_KEY + " value is unacceptable type.");
        }
      } else {
        logger.log(Level.WARNING,
                ATTR_KAFKA_ADMIN_CLIENT_CLUSTER_REQUEST_TIMEOUT_MILLISECONDS_KEY + " value is null.");
      }
    }
    return timeoutMs;
  }

  public int getKafkaAdminClientTopicRequestTimeoutMilliseconds() {
    Map<String, Object> clusterConfMap = getClusterConfMap();
    int timeoutMs = -1;
    if (clusterConfMap.containsKey(ATTR_KAFKA_ADMIN_CLIENT_TOPIC_REQUEST_TIMEOUT_MILLISECONDS_KEY)) {
      Object timeoutObject = clusterConfMap.get(ATTR_KAFKA_ADMIN_CLIENT_TOPIC_REQUEST_TIMEOUT_MILLISECONDS_KEY);
      if (timeoutObject != null) {
        if (timeoutObject instanceof Integer) {
          timeoutMs = (int) timeoutObject;
          logger.log(Level.INFO,
                  "getKafkaAdminClientTopicRequestTimeoutMilliseconds returns timeout value: " + timeoutMs);
        } else {
          logger.log(Level.WARNING,
                  ATTR_KAFKA_ADMIN_CLIENT_TOPIC_REQUEST_TIMEOUT_MILLISECONDS_KEY + " value is unacceptable type.");
        }
      } else {
        logger.log(Level.WARNING,
                ATTR_KAFKA_ADMIN_CLIENT_TOPIC_REQUEST_TIMEOUT_MILLISECONDS_KEY + " value is null.");
      }
    }
    return timeoutMs;
  }

  public int getKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds() {
    Map<String, Object> clusterConfMap = getClusterConfMap();
    int timeoutMs = -1;
    if (clusterConfMap.containsKey(ATTR_KAFKA_ADMIN_CLIENT_CONSUMER_GROUP_REQUEST_TIMEOUT_MILLISECONDS_KEY)) {
      Object timeoutObject = clusterConfMap.get(ATTR_KAFKA_ADMIN_CLIENT_CONSUMER_GROUP_REQUEST_TIMEOUT_MILLISECONDS_KEY);
      if (timeoutObject != null) {
        if (timeoutObject instanceof Integer) {
          timeoutMs = (int) timeoutObject;
          logger.log(Level.INFO,
                  "getKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds returns timeout value: " + timeoutMs);
        } else {
          logger.log(Level.WARNING,
                  ATTR_KAFKA_ADMIN_CLIENT_CONSUMER_GROUP_REQUEST_TIMEOUT_MILLISECONDS_KEY + " value is unacceptable type.");
        }
      } else {
        logger.log(Level.WARNING,
                ATTR_KAFKA_ADMIN_CLIENT_CONSUMER_GROUP_REQUEST_TIMEOUT_MILLISECONDS_KEY + " value is null.");
      }
    }
    return timeoutMs;
  }

  public int getEbsVolumeSize() {
    return ebsVolumeSize;
  }

  public void setEbsVolumeSize(int ebsVolumeSize) {
    this.ebsVolumeSize = ebsVolumeSize;
  }
}
