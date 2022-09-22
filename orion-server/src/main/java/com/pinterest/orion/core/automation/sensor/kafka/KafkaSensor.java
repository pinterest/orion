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

import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.automation.sensor.Sensor;
import com.pinterest.orion.core.kafka.KafkaCluster;

public abstract class KafkaSensor extends Sensor {

  // These cluster attributes are used to define the timeout values of the Kafka AdminClient API calls.
  // If cluster does not have required attributes, the AdminClient call will use default timeout values.
  // KafkaAdminClientClusterRequestTimeoutMs is used for AdminClient cluster/broker config APIs.
  //    Ex: describeConfigs and describeCluster
  // KafkaAdminClientTopicRequestTimeoutMs is used for AdminClient topic/partition related APIs.
  //    Ex: describeLogDirs and listOffsets
  // KafkaAdminClientConsumerGroupRequestTimeoutMs is used for AdminClient consumer group related APIs.
  //    Ex: listConsumerGroups, describeConsumerGroups and listConsumerGroupOffsets
  protected static final String ATTR_KAFKA_ADMIN_CLIENT_CLUSTER_REQUEST_TIMEOUT_MILLISECONDS_KEY =
          "KafkaAdminClientClusterRequestTimeoutMs";
  protected static final String ATTR_KAFKA_ADMIN_CLIENT_TOPIC_REQUEST_TIMEOUT_MILLISECONDS_KEY =
          "KafkaAdminClientTopicRequestTimeoutMs";
  protected static final String ATTR_KAFKA_ADMIN_CLIENT_CONSUMER_GROUP_REQUEST_TIMEOUT_MILLISECONDS_KEY =
          "KafkaAdminClientConsumerGroupRequestTimeoutMs";

  @Override
  public final void observe(Cluster cluster) throws Exception {
    if (logger == null) {
      logger = getLogger(cluster);
    }
    if(cluster instanceof KafkaCluster){
      sense((KafkaCluster) cluster);
    }
  }

  public abstract void sense(KafkaCluster cluster) throws Exception;

  protected static boolean containsKafkaAdminClientClusterRequestTimeoutMilliseconds(Cluster cluster) {
    return cluster.containsAttribute(ATTR_KAFKA_ADMIN_CLIENT_CLUSTER_REQUEST_TIMEOUT_MILLISECONDS_KEY);
  }

  protected static int getKafkaAdminClientClusterRequestTimeoutMilliseconds(Cluster cluster) {
    return Integer.valueOf(
              cluster.getAttribute(ATTR_KAFKA_ADMIN_CLIENT_CLUSTER_REQUEST_TIMEOUT_MILLISECONDS_KEY).getValue());
  }

  protected static boolean containsKafkaAdminClientTopicRequestTimeoutMilliseconds(Cluster cluster) {
    return cluster.containsAttribute(ATTR_KAFKA_ADMIN_CLIENT_TOPIC_REQUEST_TIMEOUT_MILLISECONDS_KEY);
  }

  protected static int getKafkaAdminClientTopicRequestTimeoutMilliseconds(Cluster cluster) {
    return Integer.valueOf(
              cluster.getAttribute(ATTR_KAFKA_ADMIN_CLIENT_TOPIC_REQUEST_TIMEOUT_MILLISECONDS_KEY).getValue());
  }

  protected static boolean containsKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds(Cluster cluster) {
    return cluster.containsAttribute(ATTR_KAFKA_ADMIN_CLIENT_CONSUMER_GROUP_REQUEST_TIMEOUT_MILLISECONDS_KEY);
  }

  protected static int getKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds(Cluster cluster) {
    return Integer.valueOf(
              cluster.getAttribute(ATTR_KAFKA_ADMIN_CLIENT_CONSUMER_GROUP_REQUEST_TIMEOUT_MILLISECONDS_KEY).getValue());
  }
}
