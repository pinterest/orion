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
package com.pinterest.orion.core.actions.kafka;

import com.pinterest.orion.core.Attribute;
import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.generic.NotifyNimbusOwnerAction;
import com.pinterest.orion.utils.OrionConstants;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreatePartitionsResult;
import org.apache.kafka.clients.admin.NewPartitions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class AssignmentExpandKafkaTopicAction extends AbstractKafkaAction {

  public static final String ATTR_TOPIC_NAME_KEY = "topic";
  public static final String ATTR_REPLICAS_ASSIGNMENTS_KEY = "replicas_assignments";
  public static final String ATTR_NEW_PARTITION_INDEX_KEY = "new_partition_idx";

  private static Logger logger = Logger.getLogger(AssignmentExpandKafkaTopicAction.class.getName());
  private static final String[]
      REQUIRED_ARG_KEYS =
      new String[]{ATTR_TOPIC_NAME_KEY, ATTR_REPLICAS_ASSIGNMENTS_KEY,
                   ATTR_NEW_PARTITION_INDEX_KEY};

  @Override
  public void run(String zkUrl, AdminClient adminClient) {
    for (String arg : REQUIRED_ARG_KEYS) {
      if (!containsAttribute(arg)) {
        markFailed("Missing " + arg);
        return;
      }
    }
    String topicName = getAttribute(this, ATTR_TOPIC_NAME_KEY).getValue().toString();
    Attribute attributeReplicaAssignments = getAttribute(this, ATTR_REPLICAS_ASSIGNMENTS_KEY);
    int newPartitionIdx = getAttribute(this, ATTR_NEW_PARTITION_INDEX_KEY).getValue();
    Map<Integer, List<Integer>> replicasAssignments = attributeReplicaAssignments.getValue();
    List<List<Integer>> newPartitionAssignments = new ArrayList<>();
    for (int i = newPartitionIdx; i < replicasAssignments.size(); i++) {
      newPartitionAssignments.add(replicasAssignments.get(i));
    }
    NewPartitions newPartitions =
        NewPartitions.increaseTo(replicasAssignments.size(), newPartitionAssignments);
    this.getResult().appendOut("Increasing topic " + topicName + " to " + replicasAssignments.size() + " partitions.");
    CreatePartitionsResult result = adminClient.createPartitions(Collections.singletonMap(topicName, newPartitions));

    try {
      result.all().get();
    } catch (Exception e) {
      markFailed("Failed when expanding partition count for topic " + topicName + ":" + e);
      return;
    }
    Action action = new NotifyNimbusOwnerAction();
    action.copyAttributeFrom(this, OrionConstants.PROJECT);
    action.setAttribute(NotifyNimbusOwnerAction.ATTR_SUBJECT_KEY, "[Logging Team] Kafka topic " + topicName + " has been expanded");
    action.setAttribute(NotifyNimbusOwnerAction.ATTR_MESSAGE_KEY,
        "The Kafka topic " + topicName +
            " has been expanded to " + replicasAssignments.size() +
            " partitions on cluster " + getEngine().getCluster().getClusterId()
    );
    this.getChildren().add(action);
    try {
      this.getEngine().dispatchChild(this, action);
      action.get();
    } catch (Exception e) {
      logger.warning("Failed to notify Nimbus owner: " + e);
    }

    markSucceeded();
  }

  @Override
  public String getName() {
    return "Expand Kafka Topic " + getAttribute(this, ATTR_TOPIC_NAME_KEY).getValue().toString();
  }
}
