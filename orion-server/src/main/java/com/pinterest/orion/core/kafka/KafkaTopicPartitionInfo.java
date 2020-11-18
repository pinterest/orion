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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.requests.DescribeLogDirsResponse.ReplicaInfo;

public class KafkaTopicPartitionInfo {

  private final int partition;
  private final Node leader;
  private final List<Node> replicas;
  private final List<Node> isr;
  private double mbin;
  private double mbout;
  private Map<String, ReplicaInfo> replicaInfo;

  public KafkaTopicPartitionInfo(TopicPartitionInfo info) {
    this.partition = info.partition();
    this.leader = info.leader();
    this.replicas = Collections.unmodifiableList(info.replicas());
    this.isr = Collections.unmodifiableList(info.isr());
    this.replicaInfo = new HashMap<>();
  }

  /**
   * @return the partition
   */
  public int getPartition() {
    return partition;
  }

  /**
   * @return the leader
   */
  public Node getLeader() {
    return leader;
  }

  /**
   * @return the replicas
   */
  public List<Node> getReplicas() {
    return replicas;
  }

  /**
   * @return the isr
   */
  public List<Node> getIsrs() {
    return isr;
  }

  /**
   * @return the mbin
   */
  public double getMbin() {
    return mbin;
  }

  /**
   * @param mbin the mbin to set
   */
  public void setMbin(double mbin) {
    this.mbin = mbin;
  }

  /**
   * @return the mbout
   */
  public double getMbout() {
    return mbout;
  }

  /**
   * @param mbout the mbout to set
   */
  public void setMbout(double mbout) {
    this.mbout = mbout;
  }
  
  public Map<String, ReplicaInfo> getReplicaInfo() {
    return replicaInfo;
  }
}
