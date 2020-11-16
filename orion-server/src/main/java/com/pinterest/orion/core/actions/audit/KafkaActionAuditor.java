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
package com.pinterest.orion.core.actions.audit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.ClusterManager;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.actions.Action;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

public class KafkaActionAuditor implements ActionAuditor {

  private static final Logger logger = Logger.getLogger(KafkaActionAuditor.class.getName());
  private static final String CONF_SERVERSET_PATH_KEY = "serversetPath";
  public static final String CONF_HISTORY_TOPIC_KEY = "historyTopic";
  public static final String CONF_BACKFILL_SECONDS_KEY = "backfillSeconds";
  private static Duration clientTimeout = Duration.ofSeconds(10);
  private static ObjectMapper mapper = new ObjectMapper();

  private KafkaProducer<String, String> kafkaProducer;
  private String topicName;
  private String bootstrapBrokers;
  private int backfillSeconds = 86400;

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    if (!config.containsKey(CONF_SERVERSET_PATH_KEY)) {
      throw new PluginConfigurationException("Missing config: " + CONF_SERVERSET_PATH_KEY);
    }

    if (!config.containsKey(CONF_HISTORY_TOPIC_KEY)) {
      throw new PluginConfigurationException("Missing config: " + CONF_HISTORY_TOPIC_KEY);
    }

    String serversetPath = config.get(CONF_SERVERSET_PATH_KEY).toString();

    try (Stream<String> stream = Files.lines(new File(serversetPath).toPath())) {
      bootstrapBrokers = stream.collect(Collectors.joining(","));
    } catch (IOException e) {
      throw new PluginConfigurationException("Failed to read serverset file " + serversetPath, e);
    }
    topicName = config.get(CONF_HISTORY_TOPIC_KEY).toString();

    if (config.containsKey(CONF_BACKFILL_SECONDS_KEY)) {
      backfillSeconds = Integer.parseInt(config.get(CONF_BACKFILL_SECONDS_KEY).toString());
    }

    Map<String, Object> producerConfigs = new HashMap<>();
    producerConfigs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapBrokers);
    producerConfigs
        .put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    producerConfigs
        .put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    kafkaProducer = new KafkaProducer<>(producerConfigs);
  }

  @Override
  public String getName() {
    return "KafkaActionAuditor";
  }

  @Override
  public void logAction(Cluster cluster, Action action) {
    try {
      String json = mapper.writeValueAsString(new HistoricAction(action));
      kafkaProducer
          .send(new ProducerRecord<>(topicName, cluster.getClusterId(), json)).get();
      logger.info("action " + action.getUuidString() + " logged to Kafka topic " + topicName);
    } catch (InterruptedException | ExecutionException e) {
      logger.log(Level.SEVERE, "Failed to log action " + action, e);
    } catch (JsonProcessingException jpe) {
      logger.log(Level.SEVERE, "Failed to serialize action " + action + " to JSON", jpe);
    }
  }

  @Override
  public void loadActions(ClusterManager mgr) {
    logger.info("Loading previous actions from " + backfillSeconds + " seconds ago.");
    Map<String, Object> consumerConfigs = new HashMap<>();
    consumerConfigs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapBrokers);
    consumerConfigs
        .put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    consumerConfigs
        .put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    consumerConfigs.put(ConsumerConfig.GROUP_ID_CONFIG, "orion-kafka-audit-" + UUID.randomUUID());
    consumerConfigs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerConfigs)) {
      if(!rewindConsumer(consumer)) {
        logger.warning("Topic doesn't exist or no valid topic partitions to rewind for topic " + topicName);
        return;
      }
      ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));

      Map<String, Cluster> clusters = mgr.getClusters();

      int recordCount = 0;
      while (!records.isEmpty()) {
        for(ConsumerRecord<String, String> record : records) {
          if(clusters.containsKey(record.key())){
            HistoricAction action = mapper.readValue(record.value(), HistoricAction.class);
            clusters.get(record.key()).getActionEngine().getTrackedActionsMap()
                .put(action.getUuid(), action);
          }
        }
        recordCount += records.count();
        records = consumer.poll(Duration.ofSeconds(1));
      }
      logger.info("Backfilled " + recordCount + " actions.");
    } catch (Exception e) {
      logger.log(Level.SEVERE, " failed to load actions: " , e);
    }
  }


  /**
   * rewinds the consumer to backfill seconds ago on the audit topic
   * @param consumer the consumer to rewind
   * @return whether there are any partitions assigned to the consumer
   */
  protected boolean rewindConsumer(KafkaConsumer<?,?> consumer) {
    List<PartitionInfo> partitions = consumer.partitionsFor(topicName, clientTimeout);
    long now = System.currentTimeMillis();
    long startTime = now - backfillSeconds * 1000;
    if (partitions == null) {
      return false;
    }
    Map<TopicPartition, Long> startTimes = partitions.stream()
        .collect(
            Collectors.toMap(
                p -> new TopicPartition(p.topic(),p.partition()),
                p -> startTime
            )
        );
    Map<TopicPartition, OffsetAndTimestamp> offsets = consumer.offsetsForTimes(startTimes, clientTimeout);
    List<TopicPartition> validPartitions = new ArrayList<>();
    for (Map.Entry<TopicPartition, OffsetAndTimestamp> entry : offsets.entrySet()) {
      if (entry.getValue() != null) {
        validPartitions.add(entry.getKey());
      }
    }

    if(validPartitions.isEmpty()) {
      return false;
    }

    consumer.assign(validPartitions);

    for (TopicPartition tp : validPartitions) {
      consumer.seek(tp, offsets.get(tp).offset());
    }
    return true;
  }
}
