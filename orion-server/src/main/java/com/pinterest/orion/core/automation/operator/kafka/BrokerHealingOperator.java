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

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.pinterest.orion.server.OrionServer;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Sets;
import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.core.Attribute;
import com.pinterest.orion.core.Node;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.alert.AlertLevel;
import com.pinterest.orion.core.actions.alert.AlertMessage;
import com.pinterest.orion.core.actions.kafka.BrokerRecoveryAction;
import com.pinterest.orion.core.automation.sensor.kafka.KafkaTopicSensor;
import com.pinterest.orion.core.kafka.KafkaCluster;
import com.pinterest.orion.core.kafka.KafkaTopicDescription;
import com.pinterest.orion.core.kafka.KafkaTopicPartitionInfo;
import com.pinterest.orion.utils.OrionConstants;

public class BrokerHealingOperator extends KafkaOperator {
  private static final Logger logger = Logger.getLogger(BrokerHealingOperator.class.getCanonicalName());
  private static final String CONF_DEAD_BROKER_THRESHOLD_SECONDS_KEY = "deadBrokerThresholdSeconds";
  private static final String CONF_MAX_NUM_STALE_SENSOR_INTERVALS_KEY = "maxNumStaleSensorIntervals";
  private static final String
      CONF_UNHEALTHY_BROKER_URP_RATIO_THRESHOLD_KEY = "unhealthyBrokerURPRatioThreshold";
  private static final String CONF_UNHEALTHY_ALERT_FAIL_THRESHOLD_KEY = "unhealthyAlertFailThreshold";
  private long configDeadBrokerThresholdMillis = 300_000; // default 5 minutes
  private long maxNumStaleIntervals = 2; // default 2 times
  private double unhealthyBrokerURPRatioThreshold = 0; // default no URPs allowed
  private int unhealthyAlertFailThreshold = 3;
  private static final long cooldownMilliseconds = 43200_000L;

  private Map<String, Integer> unhealthyAgentBrokersWithoutURPs = new HashMap<>();
  private Map<String, Integer> unhealthyBrokersWithoutURPs = new HashMap<>();

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    super.initialize(config);
    if (config.containsKey(CONF_DEAD_BROKER_THRESHOLD_SECONDS_KEY)) {
      configDeadBrokerThresholdMillis = 1000L
          * Long.parseLong((String) config.get(CONF_DEAD_BROKER_THRESHOLD_SECONDS_KEY));
    }
    if (config.containsKey(CONF_MAX_NUM_STALE_SENSOR_INTERVALS_KEY)) {
      maxNumStaleIntervals = Integer.parseInt(config.get(CONF_MAX_NUM_STALE_SENSOR_INTERVALS_KEY).toString());
    }
    if (config.containsKey(CONF_UNHEALTHY_BROKER_URP_RATIO_THRESHOLD_KEY)) {
      unhealthyBrokerURPRatioThreshold = Double.parseDouble(config.get(
          CONF_UNHEALTHY_BROKER_URP_RATIO_THRESHOLD_KEY).toString());
      if (unhealthyBrokerURPRatioThreshold > 1 || unhealthyBrokerURPRatioThreshold < 0) {
        throw new PluginConfigurationException("URP ratio threshold should be within range [0,1], is set to " + unhealthyBrokerURPRatioThreshold);
      }
    }
    if (config.containsKey(CONF_UNHEALTHY_ALERT_FAIL_THRESHOLD_KEY)) {
      unhealthyAlertFailThreshold = Integer.parseInt(config.get(CONF_UNHEALTHY_ALERT_FAIL_THRESHOLD_KEY).toString());
    }
  }

  @Override
  public void operate(KafkaCluster cluster) throws Exception {
    // There are 4 cases:
    // 1. Kafka: healthy  , Agent: healthy    -> happy path
    // 2. Kafka: unhealthy, Agent: healthy    -> maybe try restart the service
    //    If the agent is reporting the service is dead, try restart the service before restart

    // 3. Kafka: healthy  , Agent: unhealthy  -> agent is down / agent not deployed (alert?)
    // 4. Kafka: unhealthy, Agent: unhealthy  -> node probably dead, replace the node
    //
    // We only handle case 4 now

    // We should only handle the issue if there is only one host to replace.
    // We probably don't want to auto heal if more than one hosts needs replacement

    // first we check nodes from Kafka-reported ISR info
    // Brokers are marked as candidates if they are not in ISRs in all it's partitions
    if (!cluster.containsAttribute(KafkaTopicSensor.ATTR_TOPICINFO_MAP_KEY)) {
      logger.info("TopicInfo map is unavailable.");
      return;
    }

    Attribute topicInfoAttr = cluster.getAttribute(KafkaTopicSensor.ATTR_TOPICINFO_MAP_KEY);
    long maxStalePeriod = -1L;
    for(String sensorKey : topicInfoAttr.getPublishingSensors()) {
      maxStalePeriod = Long.max(
          cluster.getAutomationEngine().getSensorMap().get(sensorKey).getSensorInterval(), 
          maxStalePeriod
      );
    }
    // bail out if the data is stale
    if( maxStalePeriod != -1 &&
        System.currentTimeMillis() - topicInfoAttr.getUpdateTimestamp() > maxNumStaleIntervals * maxStalePeriod * 1000L
    ) {
      cluster.getActionEngine().alert(AlertLevel.MEDIUM, new AlertMessage(
          "Stale topic info data on " + cluster.getClusterId(),
          "Broker Healing Operator has stale data from the topic sensors, please check if Kafka is stable",
          "orion"
      ));
      return;
    }

    // get unhealthy kafka brokers based on URP ratio thresholds
    Set<String> sensorSet = new HashSet<>(topicInfoAttr.getPublishingSensors());
    Map<String, KafkaTopicDescription> kafkaPartitionMap = topicInfoAttr.getValue();
    Set<String> unhealthyKafkaBrokers = getUnhealthyKafkaBrokers(kafkaPartitionMap);

    // then we check nodes from agent health info
    Set<String> unhealthyAgentNodes = getDeadAgents(cluster);

    // set of nodes that have healthy agent but unhealthy service:
    Set<String> unhealthyServiceNodes = getMaybeDeadAgents(cluster);

    // brokers that are most likely dead (reporting failure from both sources)
    Set<String> deadBrokers = Sets.intersection(unhealthyKafkaBrokers, unhealthyAgentNodes);
    Set<String> nonExistentBrokers = Sets.difference(unhealthyKafkaBrokers, cluster.getNodeMap().keySet());
    deadBrokers = Sets.union(deadBrokers, nonExistentBrokers);

    // hosts that are unhealthy (service is down) but might be recoverable, try restarting the service and wait for a while before replacing
    Set<String> maybeDeadBrokers = Sets.intersection(unhealthyKafkaBrokers, unhealthyServiceNodes);
    
    // alert on brokers where agents are down but there are no URPs if they show up for 3 consecutive times
    Set<String> currentUnhealthyAgentBrokersWithoutURPs = Sets.difference(unhealthyAgentNodes, deadBrokers);
    Set<String> alertableUnhealthyAgentBrokersWithoutURPs = new HashSet<>();
    for (String b : Sets.difference(unhealthyBrokersWithoutURPs.keySet(), currentUnhealthyAgentBrokersWithoutURPs)) {
      unhealthyAgentBrokersWithoutURPs.remove(b);
    }
    for (String b : currentUnhealthyAgentBrokersWithoutURPs) {
      int count = unhealthyAgentBrokersWithoutURPs.compute(b, (k, v) -> v == null ? 1 : v + 1);
      if (count >= unhealthyAlertFailThreshold) {
        alertableUnhealthyAgentBrokersWithoutURPs.add(b);
        unhealthyAgentBrokersWithoutURPs.remove(b);
      }
    }
    if (!alertableUnhealthyAgentBrokersWithoutURPs.isEmpty()) {
      cluster.getActionEngine().alert(AlertLevel.HIGH, new AlertMessage(
          "Orion agents on " + cluster.getClusterId() + " are unhealthy, no URPs on the cluster",
          "Orion agents on " + cluster.getClusterId() + " are unhealthy, no URPs on the cluster: " + alertableUnhealthyAgentBrokersWithoutURPs,
          "orion"
      ));
      OrionServer.metricsGaugeOne(
              "broker.agent.unhealthy",
              new HashMap<String, String>() {{
                put("clusterId", cluster.getClusterId());
              }}
      );
    }
    
    // alert on brokers where broker service is unhealthy but there are no URPs if they show up for 3 consecutive times
    Set<String> currentUnhealthyBrokersWithoutURPs = Sets.difference(unhealthyServiceNodes, deadBrokers);
    Set<String> alertableUnhealthyBrokersWithoutURPs = new HashSet<>();
    for (String b : Sets.difference(unhealthyBrokersWithoutURPs.keySet(), currentUnhealthyBrokersWithoutURPs)) {
      unhealthyBrokersWithoutURPs.remove(b);
    }
    for (String b : currentUnhealthyBrokersWithoutURPs) {
      int count = unhealthyBrokersWithoutURPs.compute(b, (k, v) -> v == null ? 1 : v + 1);
      if (count >= unhealthyAlertFailThreshold) {
        alertableUnhealthyBrokersWithoutURPs.add(b);
        unhealthyBrokersWithoutURPs.remove(b);
      }
    }
    if (!alertableUnhealthyBrokersWithoutURPs.isEmpty()) {
      cluster.getActionEngine().alert(AlertLevel.HIGH, new AlertMessage(
          "Kafka service on " + cluster.getClusterId() + " are unhealthy, no URPs on the cluster",
          "Kafka service on " + cluster.getClusterId() + " are unhealthy, no URPs on the cluster: " + alertableUnhealthyBrokersWithoutURPs,
          "orion"
      ));
      OrionServer.metricsGaugeOne(
              "broker.service.unhealthy",
              new HashMap<String, String>() {{
                put("clusterId", cluster.getClusterId());
              }}
      );
    }

    setMessage("offline brokers: " + unhealthyKafkaBrokers + "\nunhealthy agent orion nodes: " + unhealthyAgentNodes +
        "\nunhealthy service orion nodes: " + maybeDeadBrokers + "\nnon-existent Brokers: "+ nonExistentBrokers);
    Set<String> candidates = Sets.union(deadBrokers, maybeDeadBrokers);
    if (candidates.size() == 1) {
      // Check if the cluster has other brokers replaced within cooldownMilliseconds and resume if not
      if (cluster.containsAttribute(BrokerRecoveryAction.ATTR_LAST_REPLACED_NODE_ID_KEY)) {
        Attribute lastReplacedAttr = cluster.getAttribute(BrokerRecoveryAction.ATTR_LAST_REPLACED_NODE_ID_KEY);
        if (System.currentTimeMillis() - lastReplacedAttr.getUpdateTimestamp() < cooldownMilliseconds) {
          logger.warning("In cooldown phase: Last replacement was at " + new Date(lastReplacedAttr.getUpdateTimestamp()) + " on the node " + lastReplacedAttr.getValue());
          return;
        }
      }

      String deadBrokerId = candidates.iterator().next();
      // This will trigger an action that will attempt to replace the broker ( and first try to restart if agent is still online but Kafka process is down)
      Action brokerRecoveryAction = newBrokerRecoveryAction();
      brokerRecoveryAction.setAttribute(OrionConstants.NODE_ID, deadBrokerId, sensorSet);

      if (nonExistentBrokers.size() == 1) {
        Node existingNode = cluster.getNodeMap().values().iterator().next();
        String extractedName = deriveNonexistentHostname(
            existingNode.getCurrentNodeInfo().getHostname(),
            existingNode.getCurrentNodeInfo().getNodeId(),
            deadBrokerId
        );
        // setting these attributes to indicate that the node doesn't exist in cluster map, and should skip any node-related checks
        brokerRecoveryAction.setAttribute(BrokerRecoveryAction.ATTR_NODE_EXISTS_KEY, false);
        brokerRecoveryAction.setAttribute(BrokerRecoveryAction.ATTR_NONEXISTENT_HOST_KEY, extractedName);
      }

      if (maybeDeadBrokers.size() == 1) {
        // Setting this flag in the action will restart the broker before replacing the broker
        brokerRecoveryAction.setAttribute(BrokerRecoveryAction.ATTR_TRY_TO_RESTART_KEY, true);
        logger.info("Will try to restart node " + deadBrokerId + " before replacing");
      }
      logger.info( "Dispatching BrokerRecoveryAction on " + cluster.getClusterId() + " for node: " +  deadBrokerId);
      dispatch(brokerRecoveryAction);
    } else if (candidates.size() > 1){
      cluster.getActionEngine().alert(AlertLevel.HIGH, new AlertMessage(
          candidates.size() + " brokers on " + cluster.getClusterId() + " are unhealthy",
          "Brokers " + candidates + " are unhealthy",
          "orion"
      ));
      OrionServer.metricsGaugeOne(
              "broker.services.unhealthy",
              new HashMap<String, String>() {{
                put("clusterId", cluster.getClusterId());
              }}
      );
      // more than 1 brokers are dead... better alert and have human intervention
      logger.severe("More than one broker is in bad state - dead: " + deadBrokers + " service down: " + maybeDeadBrokers);
      return;
    }

    if (!unhealthyKafkaBrokers.isEmpty()) {
      logger.warning(
          "Exists unhealthy Kafka brokers: " + unhealthyKafkaBrokers);
    }
    if (!nonExistentBrokers.isEmpty()) {
      logger.warning(
          "Exists offline Kafka brokers that are non-existent in Orion: " + unhealthyKafkaBrokers);
    }
    if (!unhealthyAgentNodes.isEmpty()) {
      logger.warning("Exists unhealthy Orion agents: " + unhealthyAgentNodes);
    }
    if (!unhealthyServiceNodes.isEmpty()) {
      logger.warning("Exists unhealthy Kafka services reported by Orion agent: " + unhealthyServiceNodes);
    }
  }


  /**
   * An unhealthy broker is flagged when the number of URPs on the broker exceeds a configurable threshold,
   * which is further defined as the following:
   *    [# of replicas assigned to the broker] > [# of in-sync replicas]
   * and,
   *    [# of URPs of the broker] / [# of replicas assigned to the broker] >= unhealthyBrokerURPRatioThreshold
   *
   * Note that the threshold is inclusive, i.e. if the URP ratio == threshold, the broker is marked as unhealthy
   *
   * @param kafkaPartitionMap current TopicPartitionMap of the cluster
   * @return a set of broker id strings that are unhealthy
   */
  protected Set<String> getUnhealthyKafkaBrokers(Map<String, KafkaTopicDescription> kafkaPartitionMap) {
    List<KafkaTopicPartitionInfo> partitionInfoList = kafkaPartitionMap.values().stream()
        .flatMap(td -> td.getPartitions().stream())
        .collect(Collectors.toList());

    Map<String, Long> brokerReplicaCount = partitionInfoList.stream()
        .flatMap(tpi -> tpi.getReplicas().stream())
        .map(n -> n.idString())
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

    Map<String, Long> brokerISRCount = partitionInfoList.stream()
        .flatMap(tpi -> tpi.getIsrs().stream())
        .map( n -> n.idString())
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

    Set<String> unhealthyKafkaBrokers = new HashSet<>();
    for(Map.Entry<String, Long> entry : brokerReplicaCount.entrySet()) {
      String brokerId = entry.getKey();
      long replicaCount = entry.getValue();
      long isrCount = brokerISRCount.getOrDefault(entry.getKey(), 0L);
      // if replica count is larger than isr count, there are URPs
      // so we check if the number of URPs on that broker exceeds the threshold
      // if the threshold is 0, but there are no URPs, the condition will not be true since the
      // isr count == replica count
      if (replicaCount > isrCount && (replicaCount - isrCount) >= (unhealthyBrokerURPRatioThreshold
                                                                       * replicaCount)) {
        unhealthyKafkaBrokers.add(brokerId);
      }
    }

    return unhealthyKafkaBrokers;
  }

  protected Set<String> getDeadAgents(KafkaCluster cluster) {
    return cluster.getNodeMap().values().stream()
        .filter(n -> !n.isAgentHealthy() && !n.isUnderMaintenance())
        .map(Node::getCurrentNodeInfo)
        .map(NodeInfo::getNodeId)
        .collect(Collectors.toSet());
  }

  protected Set<String> getMaybeDeadAgents(KafkaCluster cluster) {
    long serviceDeadThresholdTime = System.currentTimeMillis() - configDeadBrokerThresholdMillis;
    return cluster.getNodeMap().values().stream()
        .filter(Node::isAgentHealthy)
        .filter(n -> !n.isServiceHealthy(serviceDeadThresholdTime) && !n.isUnderMaintenance())
        .map(Node::getCurrentNodeInfo)
        .map(NodeInfo::getNodeId)
        .collect(Collectors.toSet());
  }

  protected String deriveNonexistentHostname(String existingHostname, String existingId, String nonExistingId) {
    existingHostname = existingHostname.split("\\.", 2)[0]; // sanitize potential suffixes
    int diff = nonExistingId.length() - existingId.length();
    if ( diff > 0 ) {
      existingId = StringUtils.leftPad(existingId, diff, '0');
    } else if (diff < 0) {
      nonExistingId = StringUtils.leftPad(nonExistingId, -diff, '0');
    }

    String ret = existingHostname.replace(existingId, nonExistingId);
    if (ret.equals(existingHostname)) {
      return null;
    }
    return ret;
  }

  @Override
  public String getName() {
    return "Broker Healing Operator";
  }

  protected BrokerRecoveryAction newBrokerRecoveryAction() {
    return new BrokerRecoveryAction();
  }
}
