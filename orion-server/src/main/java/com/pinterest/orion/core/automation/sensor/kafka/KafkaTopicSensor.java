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

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.pinterest.orion.server.OrionServer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.DescribeConfigsOptions;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.ConfigResource.Type;
import org.apache.kafka.common.requests.DescribeLogDirsResponse.LogDirInfo;
import org.apache.kafka.common.requests.DescribeLogDirsResponse.ReplicaInfo;

import com.pinterest.orion.core.kafka.KafkaCluster;
import com.pinterest.orion.core.kafka.KafkaTopicDescription;
import com.pinterest.orion.core.kafka.KafkaTopicPartitionInfo;
import com.pinterest.orion.core.kafka.TopicAssignment;

public class KafkaTopicSensor extends KafkaSensor {
  public static final String ATTR_TOPICINFO_MAP_KEY = "topicinfo";
  private static int kafkaAdminClientClusterRequestTimeoutMs = -1; // -1 means using default value.

  @Override
  public String getName() {
    return "KafkaTopicSensor";
  }

  @Override
  public void sense(KafkaCluster cluster) throws Exception {
    AdminClient adminClient = cluster.getAdminClient();
    if (adminClient == null) {
      return;
    }

    List<TopicAssignment> assignments;
    if (cluster.containsAttribute(KafkaClusterInfoSensor.ATTR_TOPIC_ASSIGNMENTS_KEY)) {
      assignments = cluster.getAttribute(KafkaClusterInfoSensor.ATTR_TOPIC_ASSIGNMENTS_KEY).getValue();
    } else {
      assignments = new ArrayList<>();
    }
    kafkaAdminClientClusterRequestTimeoutMs = cluster.getKafkaAdminClientClusterRequestTimeoutMilliseconds();
    Map<String, KafkaTopicDescription> topicDescriptionMap = getTopicDescriptionFromKafka(cluster);
    try {
      populateTopicBrokersetInfo(assignments, topicDescriptionMap);
      populateTopicLogDirectoryInfo(cluster, topicDescriptionMap);
      populateTopicConfigInfo(adminClient, topicDescriptionMap);
      populateTopicMetrics(cluster, topicDescriptionMap);

      setAttribute(cluster, ATTR_TOPICINFO_MAP_KEY, topicDescriptionMap);

      logger.info(() -> "Updated topic info for cluster: " + cluster.getName());
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to populate Kafka topic info", e);
    }
  }

  public static void populateTopicConfigInfo(AdminClient adminClient,
                                         Map<String, KafkaTopicDescription> topicDescriptionMap) throws ExecutionException, InterruptedException {
    List<ConfigResource> request = topicDescriptionMap.keySet().stream()
        .map(m -> new ConfigResource(Type.TOPIC, m)).collect(Collectors.toList());
    DescribeConfigsOptions describeConfigsOptions = new DescribeConfigsOptions();
    if (kafkaAdminClientClusterRequestTimeoutMs > 0) {
      describeConfigsOptions.timeoutMs(kafkaAdminClientClusterRequestTimeoutMs);
    }
    DescribeConfigsResult describeConfigs = adminClient.describeConfigs(request, describeConfigsOptions);
    Map<ConfigResource, Config> map = describeConfigs.all().get();
    map.entrySet().stream().forEach(e -> {
      KafkaTopicDescription kafkaTopicDescription = topicDescriptionMap.get(e.getKey().name());
      if (kafkaTopicDescription != null) {
        Map<String, String> value = new HashMap<>();
        Set<String> overrideConfigs = new HashSet<>();
        for (ConfigEntry configEntry : e.getValue().entries()) {
          if (configEntry.source().equals(ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG)
                  && !configEntry.isDefault()) {
            overrideConfigs.add(configEntry.name());
          }
          value.put(configEntry.name(), configEntry.value());
        }
        kafkaTopicDescription.setTopicConfigs(value);
        kafkaTopicDescription.setOverrideConfigs(overrideConfigs);
      }
    });
  }

  public static void populateTopicBrokersetInfo(List<TopicAssignment> assignments,
                                            Map<String, KafkaTopicDescription> topicDescriptionMap) {
    assignments.forEach(assignment -> {
      if (topicDescriptionMap.containsKey(assignment.getTopicName())) {
        topicDescriptionMap.get(assignment.getTopicName()).setBrokersetAlias(assignment.getBrokerset());
      }
    });
  }

  public static void populateTopicLogDirectoryInfo(KafkaCluster cluster,
                                               Map<String, KafkaTopicDescription> value) {
    if (cluster.containsAttribute(KafkaLogDirectorySensor.ATTR_BROKER_LOG_DIRS_KEY)) {
      Map<Integer, Map<String, LogDirInfo>> map = cluster
          .getAttribute(KafkaLogDirectorySensor.ATTR_BROKER_LOG_DIRS_KEY).getValue();
      for (Entry<Integer, Map<String, LogDirInfo>> entry : map.entrySet()) {
        for (Entry<String, LogDirInfo> entry2 : entry.getValue().entrySet()) {
          Map<TopicPartition, ReplicaInfo> replicaInfos = entry2.getValue().replicaInfos;
          for (Entry<TopicPartition, ReplicaInfo> entry3 : replicaInfos.entrySet()) {
            KafkaTopicDescription kafkaTopicDescription = value.get(entry3.getKey().topic());
            if (kafkaTopicDescription == null) {
              continue;
            }
            KafkaTopicPartitionInfo tpInfo = kafkaTopicDescription.partitionMap()
                .get(entry3.getKey().partition());
            if (tpInfo == null) {
              continue;
            }
            tpInfo.getReplicaInfo().put(String.valueOf(entry.getKey()), entry3.getValue());
          }
        }
      }
    }
  }

  private Map<String, KafkaTopicDescription> getTopicDescriptionFromKafka(KafkaCluster cluster)
          throws InterruptedException, ExecutionException, TimeoutException {
    return cluster.getTopicDescriptionFromKafka();
  }

  public void populateTopicMetrics(KafkaCluster cluster, Map<String, KafkaTopicDescription> topicDescriptionMap) {
    // Publish kafka topic metrics including topic size in byte, number of partitions and retention period in ms.
    for (KafkaTopicDescription topicDescription : topicDescriptionMap.values()) {
      Map<String, String> metricsTags = new HashMap<String, String>() {{
        put("topicName", topicDescription.getName());
        put("clusterId", cluster.getClusterId());
      }};
      // Topic size
      double topicSize = getTopicSizeByteFromTopicDescription(topicDescription);
      OrionServer.metricsGaugeNum(
              "kafkatopic.size.byte",
              topicSize,
              metricsTags
      );
      // Number of partitions
      double numPartition = (double) topicDescription.getPartitions().size();
      OrionServer.metricsGaugeNum(
              "kafkatopic.partition.num",
              numPartition,
              metricsTags
      );
      // Retention
      double retentionMs= Double.parseDouble(
              topicDescription.getTopicConfigs().getOrDefault("retention.ms", "0.0"));
      OrionServer.metricsGaugeNum(
              "kafkatopic.retention.millisecond",
              retentionMs,
              metricsTags
      );
    }
  }

  public double getTopicSizeByteFromTopicDescription(KafkaTopicDescription topicDescription) {
    // Calculate topic size by looping through all topic partitions and sum up the size of all replicas
    double topicSize = 0;
    for (KafkaTopicPartitionInfo partition : topicDescription.getPartitions()) {
      for (ReplicaInfo replicaInfo : partition.getReplicaInfo().values()) {
        topicSize += (double) replicaInfo.size; // Shouldn't reach Double.MAX_VALUE and value overflow is OK.
      }
    }
    return topicSize;
  }
}
