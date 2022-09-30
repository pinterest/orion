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

import com.google.common.collect.Lists;
import com.pinterest.orion.core.kafka.KafkaCluster;
import com.pinterest.orion.core.kafka.KafkaTopicDescription;
import com.pinterest.orion.core.kafka.KafkaTopicPartitionInfo;
import com.pinterest.orion.core.kafka.TopicPartitionOffsets;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListOffsetsOptions;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.internals.Topic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class KafkaTopicOffsetSensor extends KafkaSensor {
  public static final String ATTR_TOPIC_OFFSET_KEY = "topicOffsets";

  private static final int OFFSET_API_BATCH_SIZE = 1_000;
  private static int kafkaAdminClientTopicRequestTimeoutMs = -1; // -1 means using default value.

  @Override
  public void sense(KafkaCluster cluster) throws Exception {
    AdminClient adminClient = cluster.getAdminClient();
    if (adminClient == null) {
      return;
    }

    if(!cluster.containsAttribute(KafkaTopicSensor.ATTR_TOPICINFO_MAP_KEY))  {
      logger.info("Missing topic info map, skip fetching topic partition offsets");
      return;
    }

    Map<String, KafkaTopicDescription> topicDescriptionMap = cluster.getAttribute(KafkaTopicSensor.ATTR_TOPICINFO_MAP_KEY).getValue();
    kafkaAdminClientTopicRequestTimeoutMs = cluster.getKafkaAdminClientTopicRequestTimeoutMilliseconds();

    try {
      Map<TopicPartition, TopicPartitionOffsets> topicPartitionOffsetMap = getTopicOffsets(adminClient, topicDescriptionMap.values());
      setHiddenAttribute(cluster, ATTR_TOPIC_OFFSET_KEY, topicPartitionOffsetMap);
    } catch (InterruptedException | ExecutionException e) {
      logger.log(Level.WARNING, "Failed to populate topic offset info", e);
    }
  }

  private Map<TopicPartition, TopicPartitionOffsets> getTopicOffsets(
      AdminClient adminClient, Collection<KafkaTopicDescription> kafkaTopicDescriptions) throws
                                                                                         ExecutionException, InterruptedException {
    long startMs = System.currentTimeMillis();
    Map<TopicPartition, OffsetSpec> partitionsBeginningOffsetsReq = new HashMap<>();
    Map<TopicPartition, OffsetSpec> partitionsEndOffsetsReq = new HashMap<>();
    List<Map<TopicPartition, OffsetSpec>> partitionsBeginningOffsetsReqBatches = new ArrayList<>();
    List<Map<TopicPartition, OffsetSpec>> partitionsEndOffsetsReqBatches = new ArrayList<>();

    int currentBatchSize = 0;
    int partitionCount = 0;
    for (KafkaTopicDescription kafkaTopicDescription: kafkaTopicDescriptions) {
      for (KafkaTopicPartitionInfo kafkaTopicPartitionInfo : kafkaTopicDescription.getPartitions()) {
        TopicPartition topicPartition = new TopicPartition(kafkaTopicDescription.getName(), kafkaTopicPartitionInfo.getPartition());
        partitionsBeginningOffsetsReq.put(topicPartition, OffsetSpec.earliest());
        partitionsEndOffsetsReq.put(topicPartition, OffsetSpec.latest());
        ++currentBatchSize;
        ++partitionCount;
        if (currentBatchSize == OFFSET_API_BATCH_SIZE) {
          partitionsBeginningOffsetsReqBatches.add(partitionsBeginningOffsetsReq);
          partitionsBeginningOffsetsReq = new HashMap<>();
          partitionsEndOffsetsReqBatches.add(partitionsEndOffsetsReq);
          partitionsEndOffsetsReq = new HashMap<>();
          currentBatchSize = 0;
        }
      }
    }
    if (currentBatchSize > 0) {
      partitionsBeginningOffsetsReqBatches.add(partitionsBeginningOffsetsReq);
      partitionsEndOffsetsReqBatches.add(partitionsEndOffsetsReq);
    }

    int batchCount = partitionsBeginningOffsetsReqBatches.size();
    Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> beginningOffsets = new HashMap<>();
    Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets = new HashMap<>();
    List<KafkaFuture<Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo>>> beginningOffsetsFutures = new ArrayList<>();
    List<KafkaFuture<Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo>>> endOffsetsFutures = new ArrayList<>();
    ListOffsetsOptions listOffsetsOptions = new ListOffsetsOptions();
    if (kafkaAdminClientTopicRequestTimeoutMs > 0) {
      listOffsetsOptions.timeoutMs(kafkaAdminClientTopicRequestTimeoutMs);
    }
    for (int i = 0; i < batchCount; ++i) {
      beginningOffsetsFutures.add(adminClient.listOffsets(partitionsBeginningOffsetsReqBatches.get(i), listOffsetsOptions).all());
      endOffsetsFutures.add(adminClient.listOffsets(partitionsEndOffsetsReqBatches.get(i), listOffsetsOptions).all());
    }

    for (KafkaFuture<Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo>> f: beginningOffsetsFutures) {
      beginningOffsets.putAll(f.get());
    }

    for (KafkaFuture<Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo>> f: endOffsetsFutures) {
      endOffsets.putAll(f.get());
    }

    Map<TopicPartition, TopicPartitionOffsets> topicOffsets = new HashMap<>();
    partitionsBeginningOffsetsReqBatches.stream().flatMap(m -> m.keySet().stream()).forEach(tp -> {
      topicOffsets.put(tp,
          new TopicPartitionOffsets(
              beginningOffsets.containsKey(tp) ? beginningOffsets.get(tp).offset() : -1,
              endOffsets.containsKey(tp) ? endOffsets.get(tp).offset() : -1
          ));
    });

    logger.info(String.format("Spent %d ms on fetching offsets of %d partitions in %d batches.",
        System.currentTimeMillis() - startMs, partitionCount, batchCount));
    return topicOffsets;
  }
  @Override
  public String getName() {
    return "KafkaTopicOffsetSensor";
  }
}
