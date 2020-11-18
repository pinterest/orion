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
package com.pinterest.orion.core.actions.kafka;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.kafka.common.Node;

import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.actions.generic.ConcurrentAction;
import com.pinterest.orion.core.kafka.KafkaCluster;

public abstract class ConcurrentKafkaAction extends ConcurrentAction {

  public ConcurrentKafkaAction(String actionName) {
    super(actionName);
  }

  /**
   * Generate the dependency graph by adding an edge between nodes if the two nodes are replicas of the same topic partition
   * @param cluster the current cluster
   * @param actionNodes the nodes that have to execute the action
   * @return a adjacency list of each node in actionNodes
   * @throws Exception
   */
  @Override
  protected Map<String, Set<String>> generateDependencyGraph(Cluster cluster,
                                                             Collection<String> actionNodes) throws Exception {
    if (cluster instanceof KafkaCluster) {
      Set<String> actionNodeSet = new HashSet<>(actionNodes);
      Map<String, Set<String>> coverMap = new HashMap<>();
      actionNodes.forEach(node -> coverMap.compute(node, (k, v) -> new HashSet<>()).add(node));
      KafkaCluster kafkaCluster = (KafkaCluster) cluster;
      kafkaCluster.getTopicDescriptionFromKafka().values().stream()
          .flatMap(ktd -> ktd.getPartitions().stream())
          .forEach(ktpi ->  {
            Set<String> replicaSet = ktpi.getReplicas().stream()
                .map(Node::idString)
                .collect(Collectors.toSet());
            replicaSet.stream()
                .filter(actionNodeSet::contains)
                .forEach(replica ->
                    coverMap
                        .computeIfAbsent(replica, r -> new HashSet<>())
                        .addAll(replicaSet)
                );
          });
      return coverMap;
    }
    return null;
  }
}
