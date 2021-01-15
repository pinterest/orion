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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;

import com.pinterest.orion.core.Attribute;

public class AssignmentCreateKafkaTopicAction extends AbstractKafkaAction {
  public static final String ATTR_TOPIC_NAME_KEY = "topic";
  public static final String ATTR_REPLICAS_ASSIGNMENTS_KEY = "replicas_assignments";

  private static final String[] REQUIRED_ARG_KEYS = new String[] { ATTR_TOPIC_NAME_KEY,
          ATTR_REPLICAS_ASSIGNMENTS_KEY };

  @Override
  public void run(String zkUrl, AdminClient adminClient) {
    checkRequiredArgs(REQUIRED_ARG_KEYS);
    String topicName = getAttribute(this, ATTR_TOPIC_NAME_KEY).getValue().toString();
    Attribute attributeReplicaAssignments = getAttribute(this, ATTR_REPLICAS_ASSIGNMENTS_KEY);
    Map<Integer, List<Integer>> replicasAssignments = attributeReplicaAssignments.getValue();
    NewTopic topic = new NewTopic(topicName, replicasAssignments);
    CreateTopicsResult result = adminClient.createTopics(Collections.singletonList(topic));

    try {
      result.all().get();
    } catch (Exception e) {
      markFailed(e);
      return;
    }
    
    try {
      onTopicCreated(topicName, replicasAssignments);
    } catch (Exception e) {
      markFailed(e);
      return;
    }
    markSucceeded();
  }

  protected void onTopicCreated(String topicName,
                                Map<Integer, List<Integer>> assignment) throws Exception {
    // e.g. some sort of notification
  }

  @Override
  public String getName() {
    return "Create Kafka Topic " + getAttribute(this, ATTR_TOPIC_NAME_KEY).getValue().toString();
  }
}
