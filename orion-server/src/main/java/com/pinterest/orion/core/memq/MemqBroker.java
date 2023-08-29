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
package com.pinterest.orion.core.memq;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.core.Attribute;
import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.Node;
import com.pinterest.orion.core.automation.sensor.memq.Broker;
import com.pinterest.orion.core.automation.sensor.memq.MemqClusterSensor;
import com.pinterest.orion.core.automation.sensor.memq.TopicConfig;

public class MemqBroker extends Node {

  private static final long serialVersionUID = 1L;

  public MemqBroker(Cluster cluster, NodeInfo currentNodeInfo, Properties connectionProps) {
    super(cluster, currentNodeInfo, connectionProps);
  }

  public Collection<TopicConfig> getTopicPartitionsForNode() {
    MemqCluster cluster = ((MemqCluster) getCluster());

    if (cluster.containsAttribute(MemqClusterSensor.RAW_BROKER_INFO)) {
      Attribute attribute = cluster.getAttribute(MemqClusterSensor.RAW_BROKER_INFO);
      Map<String, Broker> rawBrokerMap = attribute.getValue();
      Broker broker = rawBrokerMap.get(currentNodeInfo.getNodeId());
      if (broker != null) {
        setNodeStatus(NodeStatus.COMMISSIONED);
        return broker.getAssignedTopics();
      } else {
        setNodeStatus(NodeStatus.DECOMMISSIONED);
      }
    }
    return new HashSet<>();
  }
}
