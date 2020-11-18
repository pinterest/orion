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
package com.pinterest.orion.core.kafka;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.core.Node;
import com.pinterest.orion.core.automation.sensor.kafka.KafkaTopicSensor;
import com.fasterxml.jackson.annotation.JsonProperty;

public class KafkaBroker extends Node {

  private static final long serialVersionUID = 1L;
  
  public KafkaBroker(KafkaCluster cluster, NodeInfo info, Properties connectionProps) {
    super(cluster, info, connectionProps);
  }

  public Collection<KafkaBrokerTopicSummary> getTopicPartitionsForNode() {
    KafkaCluster cluster = ((KafkaCluster) getCluster());
    if (cluster.containsAttribute(KafkaTopicSensor.ATTR_TOPICINFO_MAP_KEY)) {
      Map<String, KafkaTopicDescription> clusterMD = cluster
          .getAttribute(KafkaTopicSensor.ATTR_TOPICINFO_MAP_KEY).getValue();
      Map<String, KafkaBrokerTopicSummary> topicDescriptions = new HashMap<>();
      for (Entry<String, KafkaTopicDescription> entry : clusterMD.entrySet()) {
        KafkaTopicDescription originalDescription = entry.getValue();
        for (KafkaTopicPartitionInfo topicPartitionInfo : originalDescription.getPartitions()) {
          if (topicPartitionInfo.getReplicas().stream().anyMatch(
              n -> String.valueOf(n.id()).equalsIgnoreCase(currentNodeInfo.getNodeId()))) {

            topicDescriptions
                .computeIfAbsent(entry.getKey(), KafkaBrokerTopicSummary::new)
                .addPartition(topicPartitionInfo.getPartition());
          }
        }
      }
      return topicDescriptions.values();
    }
    return Collections.emptyList();
  }

  @Override
  public String getVersion() {
    return currentNodeInfo != null ? currentNodeInfo.getServiceInfo().get("kafka.version")
        : super.getVersion();
  }

  private static class KafkaBrokerTopicSummary {
    @JsonProperty
    String topic;
    @JsonProperty
    List<Integer> partitions = new ArrayList<>();

    public KafkaBrokerTopicSummary(String topic) {
      this.topic = topic;
    }

    public void addPartition(int partition) {
      partitions.add(partition);
    }
  }

}