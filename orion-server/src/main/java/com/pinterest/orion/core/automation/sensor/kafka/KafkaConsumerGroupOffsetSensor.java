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
package com.pinterest.orion.core.automation.sensor.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsOptions;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;

import com.pinterest.orion.core.kafka.KafkaCluster;
import com.pinterest.orion.core.kafka.KafkaConsumerGroupDescription;
import com.pinterest.orion.core.kafka.KafkaConsumerGroupOffsetsAndLag;
import com.pinterest.orion.core.kafka.TopicPartitionOffsets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class KafkaConsumerGroupOffsetSensor extends KafkaSensor {
  private static final Logger logger = Logger.getLogger(KafkaConsumerGroupOffsetSensor.class.getCanonicalName());
  public static final String ATTR_CONSUMER_GROUPS_KEY = "consumerGroups";

  @Override
  public String getName() {
    return "KafkaConsumerGroupOffsetSensor";
  }

  @Override
  public void sense(KafkaCluster cluster) throws Exception {
    logger.info(() -> "Started updating consumer groups for cluster: " + cluster.getName());
    AdminClient adminClient = cluster.getAdminClient();
    if (adminClient == null) {
      return;
    }

    if (!cluster.containsAttribute(KafkaTopicOffsetSensor.ATTR_TOPIC_OFFSET_KEY)) {
      logger.info("Missing topics offset map, skipping consumer group sensor");
      return;
    }

    if (!cluster.containsAttribute(KafkaConsumerGroupDescriptionSensor.ATTR_CONSUMER_GROUP_IDS_KEY)) {
      logger.info("Missing consumer group ids, skipping consumer group sensor");
      return;
    }

    if (!cluster.containsAttribute(KafkaConsumerGroupDescriptionSensor.ATTR_CONSUMER_GROUP_DESC_KEY)) {
      logger.info("Missing consumer group descriptions, skipping consumer group sensor");
      return;
    }


    Map<String, Map<TopicPartition, KafkaConsumerGroupOffsetsAndLag>> consumerGroupOffsetsMap = new HashMap<>();
    List<String> groupIds = cluster.getAttribute(KafkaConsumerGroupDescriptionSensor.ATTR_CONSUMER_GROUP_IDS_KEY).getValue();
    Map<String, ConsumerGroupDescription> consumerGroupDescriptionMap = cluster.getAttribute(KafkaConsumerGroupDescriptionSensor.ATTR_CONSUMER_GROUP_DESC_KEY).getValue();
    Map<TopicPartition, TopicPartitionOffsets> topicPartitionOffsetsMap = cluster.getAttribute(KafkaTopicOffsetSensor.ATTR_TOPIC_OFFSET_KEY).getValue();
    List<KafkaFuture<Map<TopicPartition, OffsetAndMetadata>>> offsetFutures = new ArrayList<>();
    ListConsumerGroupOffsetsOptions listConsumerGroupOffsetsOptions = new ListConsumerGroupOffsetsOptions();
    if (containsKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds(cluster)) {
      listConsumerGroupOffsetsOptions.timeoutMs(getKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds(cluster));
    }
    for (String groupId : groupIds) {
      ListConsumerGroupOffsetsResult listConsumerGroupOffsetsResult = adminClient.listConsumerGroupOffsets(
              groupId, listConsumerGroupOffsetsOptions);
      offsetFutures.add(listConsumerGroupOffsetsResult.partitionsToOffsetAndMetadata());
    }

    for( int i = 0; i < groupIds.size(); i++ ) {
      String groupId = groupIds.get(i);
      try {
        Map<TopicPartition, OffsetAndMetadata> offsets = offsetFutures.get(i).get();
        consumerGroupOffsetsMap.put(groupId, offsets.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> {
              long offset = entry.getValue().offset();
              TopicPartitionOffsets partitionOffsets = topicPartitionOffsetsMap.get(entry.getKey());
              if(partitionOffsets == null) {
                return new KafkaConsumerGroupOffsetsAndLag(-1, -1, offset, -1);
              }
              long beginningOffset = partitionOffsets.getBeginningOffset();
              long endOffset = partitionOffsets.getEndOffset();
              long lag = Math.max(endOffset - offset, 0);
              return new KafkaConsumerGroupOffsetsAndLag(beginningOffset, endOffset, offset, lag);
            }
        )));
      } catch (InterruptedException | ExecutionException e) {
        logger.log(Level.SEVERE, "Failed to extract offsets for consumer group " + groupId, e);
      }
    }

    setHiddenAttribute(cluster, ATTR_CONSUMER_GROUPS_KEY,
            groupIds.stream().map(groupId -> new KafkaConsumerGroupDescription(
                    consumerGroupDescriptionMap.get(groupId),
                    consumerGroupOffsetsMap.get(groupId))).collect(Collectors.toList()));

    logger.info(() ->"Updated consumer groups for cluster: " + cluster.getName());
  }

}
