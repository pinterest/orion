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
package com.pinterest.orion.core.automation.operator.kafka;

import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.core.Attribute;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.alert.AlertLevel;
import com.pinterest.orion.core.actions.alert.AlertMessage;
import com.pinterest.orion.core.actions.kafka.AssignmentCreateKafkaTopicAction;
import com.pinterest.orion.core.actions.kafka.AssignmentExpandKafkaTopicAction;
import com.pinterest.orion.core.actions.kafka.KafkaIdealBalanceAction;
import com.pinterest.orion.core.actions.kafka.ReassignmentAction;
import com.pinterest.orion.core.actions.kafka.AssignmentDeleteKafkaTopicAction;
import com.pinterest.orion.core.automation.sensor.kafka.KafkaClusterInfoSensor;
import com.pinterest.orion.core.automation.sensor.kafka.KafkaTopicSensor;
import com.pinterest.orion.core.kafka.Brokerset;
import com.pinterest.orion.core.kafka.KafkaCluster;
import com.pinterest.orion.core.kafka.KafkaTopicDescription;
import com.pinterest.orion.core.kafka.KafkaTopicPartitionInfo;
import com.pinterest.orion.core.kafka.TopicAssignment;
import com.pinterest.orion.core.utils.kafka.CuratorClient;
import com.pinterest.orion.server.OrionServer;
import com.pinterest.orion.utils.OrionConstants;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import io.dropwizard.metrics5.MetricName;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.shaded.com.google.common.collect.Sets;
import org.apache.curator.shaded.com.google.common.collect.Sets.SetView;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.common.Node;
import org.apache.zookeeper.data.Stat;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BrokersetTopicOperator extends KafkaOperator {

  private static final Logger logger = Logger
      .getLogger(BrokersetTopicOperator.class.getCanonicalName());

  private static final String CONF_STEP_SIZE_KEY = "stepSize";
  private static final String CONF_REPLICATION_FACTOR = "replicationFactor";
  public static final String ATTR_TOPIC_DELETION_ENABLED = "enableTopicDeletion";

  private int stepSize = 3;
  private int zookeeperCheckTimeoutSeconds = 5;
  private static final String CONF_MAX_NUM_STALE_SENSOR_INTERVALS_KEY = "maxNumStaleSensorIntervals";
  private long maxNumStaleIntervals = 2; // default 2 times
  private boolean enableTopicDeletion = false;

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    super.initialize(config);
    stepSize = (int) config.getOrDefault(CONF_STEP_SIZE_KEY, stepSize);
    if (config.containsKey(CONF_MAX_NUM_STALE_SENSOR_INTERVALS_KEY)) {
      maxNumStaleIntervals = Integer
          .parseInt(config.get(CONF_MAX_NUM_STALE_SENSOR_INTERVALS_KEY).toString());
    }
    if (config.containsKey(ATTR_TOPIC_DELETION_ENABLED)) {
      enableTopicDeletion = Boolean.parseBoolean(config.get(ATTR_TOPIC_DELETION_ENABLED).toString());
    }
  }

  @Override
  public void operate(KafkaCluster cluster) throws Exception {
    MetricName metric = MetricName.build("pinterest_brokerset_topic_operator").tagged("cluster", cluster.getName());
    AdminClient adminClient = cluster.getAdminClient();
    if (adminClient == null) {
      return;
    } else if (!cluster.containsAttribute(KafkaClusterInfoSensor.ATTR_BROKERSET_KEY)
        || !cluster.containsAttribute(KafkaClusterInfoSensor.ATTR_TOPIC_ASSIGNMENTS_KEY)
        || !cluster.containsAttribute(KafkaTopicSensor.ATTR_TOPICINFO_MAP_KEY)) {
      return;
    }

    Set<String> sensorSet = new HashSet<>();

    Attribute brokersetMapAttr = cluster.getAttribute(KafkaClusterInfoSensor.ATTR_BROKERSET_KEY);
    Map<String, Brokerset> brokersetMap = brokersetMapAttr.getValue();
    sensorSet.addAll(brokersetMapAttr.getPublishingSensors());

    Attribute topicAssignmentsAttr = cluster
        .getAttribute(KafkaClusterInfoSensor.ATTR_TOPIC_ASSIGNMENTS_KEY);
    List<TopicAssignment> topicAssignments = topicAssignmentsAttr.getValue();
    sensorSet.addAll(topicAssignmentsAttr.getPublishingSensors());

    Attribute topicDescriptionMapAttr = cluster
        .getAttribute(KafkaTopicSensor.ATTR_TOPICINFO_MAP_KEY);
    Map<String, KafkaTopicDescription> topicDescriptionMap = topicDescriptionMapAttr.getValue();
    long maxStalePeriod = -1L;
    for (String sensorKey : topicDescriptionMapAttr.getPublishingSensors()) {
      maxStalePeriod = Long.max(
          cluster.getAutomationEngine().getSensorMap().get(sensorKey).getSensorInterval(),
          maxStalePeriod);
    }
    // bail out if the data is stale
    if (maxStalePeriod != -1 && System.currentTimeMillis() - topicDescriptionMapAttr
        .getUpdateTimestamp() > maxNumStaleIntervals * maxStalePeriod * 1000L) {
      cluster.getActionEngine().alert(AlertLevel.MEDIUM, new AlertMessage(
          "Stale topic info data on " + cluster.getClusterId(),
          "BrokersetTopicOperator has stale data from the topic sensors, please check if Kafka is stable",
          "orion"));
      return;
    }
    sensorSet.addAll(topicDescriptionMapAttr.getPublishingSensors());

    logger.info("Traversing through topicassignments to find deltas between ideal"
        + " and actual for " + topicAssignments.size() + " topics");
    for (TopicAssignment topicAssignment : topicAssignments) {

      String brokersetAlias = topicAssignment.getBrokerset();
      String topicName = topicAssignment.getTopicName();
      metric.tagged("topic", topicName);
      KafkaTopicDescription actualTopicDescription = topicDescriptionMap.get(topicName);
      Brokerset brokerset = brokersetMap.get(brokersetAlias);

      if (brokerset == null) {
        logger.warning("Topic(" + topicName + ") Attempted to use Brokerset(" + brokersetAlias + ") which doesn't exist.");
        OrionServer.METRICS.counter(metric.resolve("non_existent_brokerset")).inc();
        continue;
      }

      if (topicAssignment.isDelete()) {
        if (enableTopicDeletion) {
          if (actualTopicDescription == null) {
            logger.info("Topic(" + topicName + ") exists and is marked for deletion. However deletion is not enabled in the server config file.");
          } else {
            Action action = createDeleteTopicAction(topicAssignment.getTopicName(), sensorSet);
            logger.info("Topic(" + topicName + ") exists and is marked for deletion. Starting deletion.");
            dispatch(action);
          }
        }
        continue;
      }

      // create topic if topic exists in assignment file but is absent in the cluster
      if (actualTopicDescription == null) {
        Action action = createIdealBalancedTopicAction(brokerset, topicAssignment, sensorSet, stepSize);
        logger.info("Topic(" + topicName + ") doesn't exist planned creation");
        dispatch(action);
        return;
      }

      int idealPartitionCount = getPartitionsFromBrokersetOrTopicAssignment(brokerset,
          topicAssignment);
      int actualPartitionCount = actualTopicDescription.getPartitions().size();

      int idealRepFactor = topicAssignment.getReplicationFactor();
      int actualRepFactor = actualTopicDescription.getSampledReplicationFactor();

      if (idealPartitionCount > actualPartitionCount) {
        // expansion
        // reject expasion if topic has compact in cleanup policy
        if (topicAssignment.getConfig() != null &&
                topicAssignment.getConfig().containsKey("cleanup.policy")) {
          List<String> policy = Arrays.asList(topicAssignment.getConfig().get("cleanup.policy").split(","));
          if (policy.stream().anyMatch(s -> s.equals("compact"))) {
            logger.warning(
                    "Compacted topic " + topicName + " partition count can't be increased, rejecting expansion - Actual: "
                            + actualPartitionCount + " Ideal: " + idealPartitionCount);
            return;
          }
        }
        logger.warning(
            "Topic " + topicName + " partition count is not ideal, expanding topic - Actual: "
                + actualPartitionCount + " Ideal: " + idealPartitionCount);
        TopicAssignment ta = topicAssignment;
        if (idealRepFactor != actualRepFactor) {
          // Make ta copy and override ideal rep factor
          ta = new TopicAssignment(topicAssignment);
          ta.setReplicationFactor(actualRepFactor);
        }
        Action action = expandIdealBalancedTopicAction(brokerset, ta,
                actualPartitionCount, sensorSet);
        dispatch(action);
        return;
      } else if (idealPartitionCount < actualPartitionCount) {
        // invalid: Kafka topics cannot be shrunk
        AlertMessage moreThanIdealPartitionsAlert = new AlertMessage(
            cluster.getClusterId() + " topic " + topicName + " has more than ideal partitions",
            "Ideal count: " + idealPartitionCount + ", actual count: " + actualPartitionCount,
            "orion");
        cluster.getActionEngine().alert(AlertLevel.MEDIUM, moreThanIdealPartitionsAlert);
        logger
            .warning("Topic " + topicName + " ideal partition count is less than actual - Actual: "
                + actualPartitionCount + " Ideal: " + idealPartitionCount);
        continue;
      }

      // the topic's assignment is off from ideal
      Map<Integer, List<Integer>> idealAssignment = generateAssignmentForTopic(brokerset,
          topicAssignment, stepSize);
      Map<Integer, List<Integer>> actualAssignment = getAssignmentFromTopicDescription(
          actualTopicDescription);
      Set<Integer> nonIdealPartitions = getTopicNonIdealPartitions(idealAssignment,
          actualAssignment);
      // skip if the topic is ideal
      if (nonIdealPartitions.isEmpty()) {
        continue;
      }
      // check if there are existing reassignments
      if (!cluster.getNodeMap().isEmpty()) {
        NodeInfo nodeInfo = cluster.getNodeMap().values().iterator().next().getCurrentNodeInfo();
        String zkUrl = nodeInfo.getServiceInfo().get(KafkaCluster.ZOOKEEPER_CONNECT);
        if (isClusterInReassignmentState(zkUrl)) {
          return;
        }
      }

      for (int pIdx = 0; pIdx < idealAssignment.size(); pIdx++) {
        if (!nonIdealPartitions.contains(pIdx)) {
          actualAssignment.remove(pIdx);
          idealAssignment.remove(pIdx);
        }
      }
      logger.info("Topic(" + topicName + ") requires ideal rebalance");

      // rebalance topics in the topic assignment file if they are off
      Action rebalanceAction = new KafkaIdealBalanceAction();
      rebalanceAction.setAttribute(KafkaIdealBalanceAction.ATTR_TOPIC_KEY, topicName);
      rebalanceAction.setAttribute(KafkaIdealBalanceAction.ATTR_ACTUAL_ASSIGNMENTS_KEY,
          actualAssignment, sensorSet);
      rebalanceAction.setAttribute(KafkaIdealBalanceAction.ATTR_IDEAL_ASSIGNMENTS_KEY,
          idealAssignment, sensorSet);

      dispatch(rebalanceAction);
      // very important to exit out of the loop here to prevent multiple reassignments
      // from being triggered
      return;
    }
    logger.info("All brokersets/topicassignments are ideal");
  }

  private boolean isClusterInReassignmentState(String zkUrl) throws Exception {
    try (CuratorFramework client = CuratorClient.buildAndGetZkClient(zkUrl)) {
      client.start();
      if (!client.blockUntilConnected(zookeeperCheckTimeoutSeconds, TimeUnit.SECONDS)) {
        logger.severe("Could not connect to zookeeper " + zkUrl);
        return true;
      }
      Stat stat = client.checkExists().forPath(ReassignmentAction.REASSIGNMENT_PATH);
      if (stat != null) {
        logger.warning("Active reassignment already exists. Rebalancing action aborted.");
        return true;
      }
    }
    return false;
  }

  public static AssignmentCreateKafkaTopicAction createIdealBalancedTopicAction(Brokerset brokerset,
                                                TopicAssignment topicAssignment,
                                                Set<String> sensorSet,
                                                int stepSize) throws Exception {
    Map<Integer, List<Integer>> assignments = generateAssignmentForTopic(brokerset,
        topicAssignment, stepSize);
    AssignmentCreateKafkaTopicAction action = new AssignmentCreateKafkaTopicAction();
    action.setAttribute(AssignmentCreateKafkaTopicAction.ATTR_TOPIC_NAME_KEY,
        topicAssignment.getTopicName(), sensorSet);
    action.setAttribute(OrionConstants.PROJECT, topicAssignment.getProject());
    action.setAttribute(AssignmentCreateKafkaTopicAction.ATTR_REPLICAS_ASSIGNMENTS_KEY, assignments,
        sensorSet);
    return action;
  }

  public static AssignmentDeleteKafkaTopicAction createDeleteTopicAction(String topicName,
                                                                         Set<String> sensorSet) throws Exception {
    AssignmentDeleteKafkaTopicAction action = new AssignmentDeleteKafkaTopicAction();
    action.setAttribute(AssignmentDeleteKafkaTopicAction.ATTR_TOPIC_NAME_KEY, topicName);
    return action;
  }

  private Action expandIdealBalancedTopicAction(Brokerset brokerset,
                                                TopicAssignment topicAssignment,
                                                int newPartitionStartIdx,
                                                Set<String> sensorSet) throws Exception {
    Map<Integer, List<Integer>> assignments = generateAssignmentForTopic(brokerset,
        topicAssignment, stepSize);
    Action action = new AssignmentExpandKafkaTopicAction();
    action.setAttribute(AssignmentExpandKafkaTopicAction.ATTR_TOPIC_NAME_KEY,
        topicAssignment.getTopicName(), sensorSet);
    action.setAttribute(OrionConstants.PROJECT, topicAssignment.getProject());
    action.setAttribute(AssignmentExpandKafkaTopicAction.ATTR_REPLICAS_ASSIGNMENTS_KEY, assignments,
        sensorSet);
    action.setAttribute(AssignmentExpandKafkaTopicAction.ATTR_NEW_PARTITION_INDEX_KEY,
        newPartitionStartIdx);
    return action;

  }

  private static Map<Integer, List<Integer>> getAssignmentFromTopicDescription(KafkaTopicDescription kafkaTopicDescription) {
    return kafkaTopicDescription.partitionMap().values().stream()
        .collect(Collectors.toMap(KafkaTopicPartitionInfo::getPartition,
            pInfo -> pInfo.getReplicas().stream().map(Node::id).collect(Collectors.toList())));
  }

  private static Set<Integer> getTopicNonIdealPartitions(Map<Integer, List<Integer>> idealAssignment,
                                                         Map<Integer, List<Integer>> actualAssignment) {
    Set<Integer> nonIdealPartitions = new HashSet<>();
    for (int pIdx = 0; pIdx < idealAssignment.size(); pIdx++) {
      List<Integer> idealReplicas = idealAssignment.get(pIdx);
      List<Integer> actualReplicas = actualAssignment.get(pIdx);
      if (!idealReplicas.equals(actualReplicas)) {
        nonIdealPartitions.add(pIdx);
      }
    }
    return nonIdealPartitions;
  }

  public static Map<Integer, List<Integer>> generateAssignmentForTopic(Brokerset brokerset,
                                                                       TopicAssignment topicAssignment,
                                                                       int stepSize) {
    int stride = topicAssignment.getStride();
    int partitions = getPartitionsFromBrokersetOrTopicAssignment(brokerset, topicAssignment);
    int topicReplicationFactor = topicAssignment.getReplicationFactor();
    Map<Integer, List<Integer>> assignmentMap = new HashMap<>();
    for (int i = 0; i < partitions; i++) {
      assignmentMap.put(i, new ArrayList<>(topicReplicationFactor));
    }

    for (int replicaId = 0; replicaId < topicReplicationFactor; replicaId++) {
      Iterator<Integer> brokerSequence = Iterables
          .skip(Iterables.cycle(brokerset), replicaId * (stride * stepSize + 1)).iterator();
      for (int i = 0; i < partitions; i++) {
        assignmentMap.get(i).add(brokerSequence.next());
      }
    }
    return assignmentMap;
  }

  public static int getPartitionsFromBrokersetOrTopicAssignment(Brokerset bs, TopicAssignment ta) {
    int partitions = bs.getPartitions();
    if (!(partitions > 0)) {
      partitions = ta.getPartitions();
    }
    return partitions;
  }

  @Override
  public String getName() {
    return "BrokersetTopicOperator";
  }

  public static void main(String[] args) throws Exception {
    if (args.length < 3 || args.length > 5) {
      System.out.println(
          "Usage: BrokersetTopicOperator <bootstrapServer> <topic assignment filename> <brokerset"
              + " filename> [<replication factor = 3> [step size = 3]]");
      System.exit(1);
    }
    String bootstrapServers = args[0];
    String topicassignmentFilename = args[1];
    String brokersetsFilename = args[2];

    BrokersetTopicOperator optr = new BrokersetTopicOperator();

    Map<String, Object> config = new HashMap<>();

    if (args.length > 3) {
      config.put(CONF_REPLICATION_FACTOR, Integer.parseInt(args[3]));
      if (args.length > 4) {
        config.put(CONF_STEP_SIZE_KEY, Integer.parseInt(args[4]));
      }
    }

    optr.initialize(config);

    // get ideal state from cluster info dir
    List<Brokerset> brokersetList = KafkaClusterInfoSensor
        .loadBrokersetsFromFile(new File(brokersetsFilename));
    Map<String, Brokerset> brokersetMap = brokersetList.stream()
        .collect(Collectors.toMap(Brokerset::getBrokersetAlias, b -> b));

    List<TopicAssignment> idealTopicAssignments = KafkaClusterInfoSensor
        .loadTopicAssignmentsFromFile(new File(topicassignmentFilename));
    Map<TopicAssignment, Brokerset> topicToBrokersetMap = idealTopicAssignments.stream()
        .map(topic -> Maps.immutableEntry(topic, brokersetMap.get(topic.getBrokerset())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // get actual state from kafka
    KafkaCluster cluster = new KafkaCluster("", "", Collections.emptyList(),
        Collections.emptyList(), null, null, null, null, null);
    Map<String, Object> adminClientConfigs = new HashMap<>();
    adminClientConfigs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    cluster.setAdminClient(KafkaAdminClient.create(adminClientConfigs));
    Map<String, KafkaTopicDescription> actualTopicMap = cluster.getTopicDescriptionFromKafka();

    for (Map.Entry<TopicAssignment, Brokerset> e : topicToBrokersetMap.entrySet()) {
      TopicAssignment topicAssignment = e.getKey();
      if (topicAssignment.getDescription() == null || topicAssignment.getDescription().isEmpty()) {
        logger.info(
            "WARNING: Missing required field 'description' in " + topicAssignment.getTopicName());
      }
      String topicName = topicAssignment.getTopicName();
      if (actualTopicMap.containsKey(topicName)) {
        Brokerset brokerset = e.getValue();
        Map<Integer, List<Integer>> idealAssignment = generateAssignmentForTopic(brokerset,
            topicAssignment, 3);
        KafkaTopicDescription actualTopicDescription = actualTopicMap.get(topicName);
        int actualSize = actualTopicDescription.getPartitions().size();
        int idealSize = idealAssignment.size();
        if (idealAssignment.size() != actualTopicDescription.getPartitions().size()) {
          logger.info("Topic " + topicName + " partition count has conflicts: Actual = "
              + actualSize + " Ideal = " + idealSize);
          continue;
        }
        Map<Integer, List<Integer>> actualAssignment = getAssignmentFromTopicDescription(
            actualTopicDescription);
        boolean topicIsIdeal = true;
        for (int pIdx = 0; pIdx < idealSize; pIdx++) {
          List<Integer> idealReplicas = idealAssignment.get(pIdx);
          List<Integer> actualReplicas = actualAssignment.get(pIdx);
          if (idealReplicas.size() != actualReplicas.size()) {
            topicIsIdeal = false;
            logger.info("Topic " + topicName + " partition: " + pIdx
                + " mismatch in replica size: Actual = " + actualReplicas.size() + " Ideal = "
                + idealReplicas.size());
          } else if (!new HashSet<>(idealReplicas).containsAll(actualReplicas)) {
            topicIsIdeal = false;
            if (!new HashSet<>(idealReplicas).containsAll(actualReplicas)) {
              logger.info("Topic " + topicName + " partition: " + pIdx
                  + " has different replicas: Actual = " + actualReplicas + " Ideal = "
                  + idealReplicas);
            }
          }
        }
        if (topicIsIdeal) {
          logger.info("Topic " + topicName + " is ideal.");
        }
      }
    }
    Set<String> currentTopics = new HashSet<>(actualTopicMap.keySet());
    Set<String> assignedTopics = topicToBrokersetMap.keySet().stream().map(t -> t.getTopicName())
        .collect(Collectors.toSet());
    SetView<String> missingTopics = Sets.difference(currentTopics, assignedTopics);
    if (!missingTopics.isEmpty()) {
      logger.info("Following topics do not have brokerset assignments:" + missingTopics);
    }
  }
}
