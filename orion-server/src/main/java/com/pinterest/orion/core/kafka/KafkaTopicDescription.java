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

import java.util.*;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.TopicDescription;

import com.pinterest.orion.core.Context;

public class KafkaTopicDescription {

  private final String name;
  private final boolean internal;
  private final Map<Integer, KafkaTopicPartitionInfo> partitions;
  private Map<String, String> topicConfigs;
  private String brokersetAlias;
  private Set<String> overrideConfigs;

  public KafkaTopicDescription(TopicDescription description) {
    this.name = description.name();
    this.internal = description.isInternal();
    this.partitions = description.partitions().stream().map(p -> new KafkaTopicPartitionInfo(p))
        .collect(Collectors.toMap(tp -> tp.getPartition(), tp -> tp));
    this.overrideConfigs = new HashSet<>();
  }

  public KafkaTopicDescription(String name,
                               boolean internal,
                               Map<Integer, KafkaTopicPartitionInfo> partitions) {
    this.name = name;
    this.internal = internal;
    this.partitions = partitions;
  }

  /**
   * @return the sampledReplicationFactor (-1 if mismatch)
   */
  public int getSampledReplicationFactor() {
    // randomly sample replicas from 2 partitions
    List<Integer> partitionsList = new ArrayList<>(partitions.keySet());
    Collections.shuffle(partitionsList);  // randomly shuffle partitions
    int sample0 = partitions.get(partitionsList.get(0)).getReplicas().size();
    if (partitionsList.size() <= 1) {
      // return first sample if there is only one partition
      return sample0;
    } else {
      int sample1 = partitions.get(partitionsList.get(1)).getReplicas().size();
      if (sample0 != sample1) {
        // weird assignment, need to alert. This case needs to be handled whenever this method is called
        return -1;
      }
      // samples equal, return sample
      return sample0;
    }
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the internal
   */
  public boolean isInternal() {
    return internal;
  }

  /**
   * @return the partitions
   */
  public Collection<KafkaTopicPartitionInfo> getPartitions() {
    return partitions.values();
  }
  
  public Map<Integer,KafkaTopicPartitionInfo> partitionMap(){
    return partitions;
  }

  /**
   * @param topicConfigs the topicConfigs to set
   */
  public void setTopicConfigs(Map<String, String> topicConfigs) {
    this.topicConfigs = topicConfigs;
  }

  /**
   * @param brokersetAlias the brokersetAlias to set
   */
  public void setBrokersetAlias(String brokersetAlias) {
    this.brokersetAlias = brokersetAlias;
  }

  /**
   * @return the topicSettings
   */
  public Map<String, String> getTopicConfigs() {
    return topicConfigs;
  }

  /**
   * @return the brokersetAlias
   */
  public String getBrokersetAlias() {
    return brokersetAlias;
  }

  /**
   * @return the overrideConfigs
   */
  public Set<String> getOverrideConfigs() {
    return overrideConfigs;
  }

  /**
   * @param overrideConfigs the overrideConfigs to set
   */
  public void setOverrideConfigs(Set<String> overrideConfigs) {
    this.overrideConfigs = overrideConfigs;
  }
}
