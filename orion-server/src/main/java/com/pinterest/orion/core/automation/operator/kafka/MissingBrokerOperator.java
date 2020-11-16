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
package com.pinterest.orion.core.automation.operator.kafka;

import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.core.automation.sensor.kafka.KafkaBrokerSensor;
import com.pinterest.orion.core.kafka.KafkaCluster;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MissingBrokerOperator extends KafkaOperator {

  @Override
  @SuppressWarnings("unchecked")
  public void operate(KafkaCluster cluster) throws Exception {
    if (!cluster.containsAttribute(KafkaBrokerSensor.ATTR_BROKERS_KEY)) {
      // can't do anything since the attribute is not populated
      return;
    }
    List<NodeInfo> kafkaBrokers = (List<NodeInfo>) cluster
        .getAttribute(KafkaBrokerSensor.ATTR_BROKERS_KEY).getValue();
    Set<String> kafkaBrokersSet = kafkaBrokers.stream().map(NodeInfo::getNodeId)
        .collect(Collectors.toSet());
    Set<String> nodeIdSet = cluster.getNodeMap().keySet();

    for (String missingOrionNode : Sets.difference(kafkaBrokersSet, nodeIdSet)) {
      // node doesn't exist in Orion, but exists in Kafka metadata

    }

    for (String missingKafkaBroker : Sets.difference(nodeIdSet, kafkaBrokersSet)) {
      // broker is not in the Kafka cluster, but agent exists in Orion

    }
  }

  @Override
  public String getName() {
    return "Missing Broker Operator";
  }
}
