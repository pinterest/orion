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

import org.apache.kafka.common.Node;

import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.kafka.KafkaStuckConsumerGroupRecoveryAction;
import com.pinterest.orion.core.automation.sensor.kafka.KafkaStuckConsumerGroupSensor;
import com.pinterest.orion.core.automation.sensor.kafka.KafkaTopicSensor;
import com.pinterest.orion.core.kafka.KafkaCluster;
import com.pinterest.orion.core.kafka.KafkaTopicDescription;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class StuckConsumerGroupOperator extends KafkaOperator {
  public static final String CONSUMER_OFFSETS_TOPIC_NAME = "__consumer_offsets";

  private static final Logger logger = Logger.getLogger(StuckConsumerGroupOperator.class.getName());

  @Override
  public void operate(KafkaCluster cluster) throws Exception {
    if(!cluster.containsAttribute(KafkaStuckConsumerGroupSensor.ATTR_STUCK_CONSUMER_GROUPS_ID_KEY)) {
      return;
    }

    Set<String> groupIds = cluster.getAttribute(KafkaStuckConsumerGroupSensor.ATTR_STUCK_CONSUMER_GROUPS_ID_KEY).getValue();

    if(groupIds.isEmpty()) {
      return;
    }
    logger.info(groupIds.size() + " consumers stuck on " + cluster.getClusterId() + ", swapping leaders to bump coordinator");

    Map<String, Map<Integer, List<Integer>>> assignment = getConsumerOffsetsPartitionAssignment(cluster, groupIds);
    Action action = new KafkaStuckConsumerGroupRecoveryAction();
    action.setAttribute(KafkaStuckConsumerGroupRecoveryAction.ATTR_PARTITION_ASSIGNMENT_KEY, assignment);
    action.setAttribute(KafkaStuckConsumerGroupRecoveryAction.ATTR_GROUP_IDS_KEY, groupIds);

    dispatch(action);
  }

  protected Map<String, Map<Integer, List<Integer>>> getConsumerOffsetsPartitionAssignment(KafkaCluster cluster, Set<String> groupIds) {
    Map<String, KafkaTopicDescription>
        topicDescriptionMap = cluster.getAttribute(KafkaTopicSensor.ATTR_TOPICINFO_MAP_KEY).getValue();
    KafkaTopicDescription consumerOffsetDesc = topicDescriptionMap.get(CONSUMER_OFFSETS_TOPIC_NAME);
    int partitionCount = 50; // default 50
    if (consumerOffsetDesc != null) {
      partitionCount = consumerOffsetDesc.getPartitions().size();
    }
    Map<Integer, List<Integer>> partitionAssignments = new HashMap<>();
    for(String groupId : groupIds) {
      int partition = Math.abs(groupId.hashCode()) % partitionCount;
      partitionAssignments.putIfAbsent(
          partition,
          consumerOffsetDesc
          .partitionMap()
          .get(partition).getReplicas().stream()
          .map(Node::id)
          .collect(Collectors.toList())
      );
    }
    return Collections.singletonMap(CONSUMER_OFFSETS_TOPIC_NAME, partitionAssignments);
  }

  @Override
  public String getName() {
    return "StuckConsumerGroupOperator";
  }

}
