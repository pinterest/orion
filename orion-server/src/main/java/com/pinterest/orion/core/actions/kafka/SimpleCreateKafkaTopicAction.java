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

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class SimpleCreateKafkaTopicAction extends AbstractKafkaAction {

  public static String ATTR_TOPIC_NAME_KEY = "topic";
  public static String ATTR_PARTITION_COUNT_KEY = "partition_count";
  public static String ATTR_REPLICATION_FACTOR_KEY = "replication_factor";
  public static String ATTR_TOPIC_CONFIGS_KEY = "configs";

  @Override
  @SuppressWarnings("unchecked")
  public void run(String zkUrl, AdminClient adminClient) {
    if (!containsAttribute(ATTR_TOPIC_NAME_KEY)) {
      markFailed("Missing topic name");
      return;
    } else if (!containsAttribute(ATTR_PARTITION_COUNT_KEY)) {
      markFailed("Missing partition count");
      return;
    } else if (!containsAttribute(ATTR_REPLICATION_FACTOR_KEY)) {
      markFailed("Missing replication factor");
      return;
    }
    String topicName = getAttribute(ATTR_TOPIC_NAME_KEY).toString();
    int numPartitions = getAttribute(this, ATTR_REPLICATION_FACTOR_KEY).getValue();
    short replicationFactor = ((Integer) getAttribute(this, ATTR_REPLICATION_FACTOR_KEY).getValue())
        .shortValue();
    Map<String, String> topicConfigs = getAttribute(ATTR_TOPIC_CONFIGS_KEY).getValue();
    NewTopic topic = new NewTopic(topicName, numPartitions, replicationFactor);
    topic.configs(topicConfigs);
    CreateTopicsResult result = adminClient.createTopics(Collections.singletonList(topic));
    try {
      result.all().get();
    } catch (InterruptedException | ExecutionException e) {
      markFailed(e);
    }
    markSucceeded();
  }

  @Override
  public String getName() {
    return "Create Kafka Topic";
  }
}
