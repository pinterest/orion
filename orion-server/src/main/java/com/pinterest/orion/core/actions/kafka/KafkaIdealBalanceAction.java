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

import com.google.common.collect.Lists;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.kafka.KafkaCluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class KafkaIdealBalanceAction extends Action {
  private static Logger logger = Logger.getLogger(KafkaIdealBalanceAction.class.getCanonicalName());

  public static final String CONF_BATCH_SIZE_KEY = "batchSize";
  public static final String CONF_METADATA_FETCH_TIMEOUT_MS_KEY = "metadataTimeoutMs";
  public static final String ATTR_ACTUAL_ASSIGNMENTS_KEY = "actualAssignments";
  public static final String ATTR_IDEAL_ASSIGNMENTS_KEY = "idealAssignments";
  public static final String ATTR_TOPIC_KEY = "topic";
  private static final String[] attrArr = new String[]{ATTR_ACTUAL_ASSIGNMENTS_KEY, ATTR_IDEAL_ASSIGNMENTS_KEY, ATTR_TOPIC_KEY};

  public int batchSize = 60;
  public long metadataTimeoutMs = KafkaCluster.DEFAULT_METADATA_TIMEOUT_MS;  // initialize as default

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    super.initialize(config);
    if(config.containsKey(CONF_BATCH_SIZE_KEY)) {
      batchSize = Integer.parseInt(config.get(CONF_BATCH_SIZE_KEY).toString());
    }
    if (config.containsKey(CONF_METADATA_FETCH_TIMEOUT_MS_KEY)) {
      metadataTimeoutMs = Long.parseLong(config.get(CONF_METADATA_FETCH_TIMEOUT_MS_KEY).toString());
    }
  }

  @Override
  public void runAction() throws Exception {
    for(String attrKey : attrArr){
      if (!containsAttribute(attrKey)) {
        markFailed("Missing attribute " + attrKey);
        return;
      }
    }

    Map<Integer, List<Integer>> actualAssignmentsSrc = getAttribute(this, ATTR_ACTUAL_ASSIGNMENTS_KEY).getValue();
    Map<Integer, List<Integer>> idealAssignments = getAttribute(this, ATTR_IDEAL_ASSIGNMENTS_KEY).getValue();
    String topic = getAttribute(this, ATTR_TOPIC_KEY).getValue();
    if (!actualAssignmentsSrc.keySet().equals(idealAssignments.keySet())) {
      markFailed("actualAssignment does not have same partitions as idealAssignment!");
      return;
    }

    if(actualAssignmentsSrc.isEmpty()){
      markSucceeded();
      return;
    }

    Map<Integer, List<Integer>> actualAssignments = actualAssignmentsSrc.entrySet().stream().collect(
        Collectors.toMap(Map.Entry::getKey, e -> new ArrayList<>(e.getValue())));

    List<Integer> partitions = new ArrayList<>(actualAssignments.keySet());
    Collections.sort(partitions);
    List<List<Integer>> batchedPartitions = Lists.partition(partitions, batchSize);
    // assume that the partitions all have same replication factor as the first ideal assignment
    int idealReplicationFactor = idealAssignments.get(partitions.get(0)).size();
    int actualReplicationFactor = actualAssignments.get(partitions.get(0)).size();
    for (int replicaIdx = 0; replicaIdx < idealReplicationFactor; replicaIdx++) {
      logger.info("Ideal balancing replica " + replicaIdx + " of topic " + topic);
      for (int batchIdx = 0; batchIdx < batchedPartitions.size(); batchIdx++) {
        // for each partition in the batch, see if shrinking is needed:
        Map<Integer, List<Integer>> shrinkReassignments = new HashMap<>();
        Map<Integer, List<Integer>> expandReassignments = new HashMap<>();
        for (int partition : batchedPartitions.get(batchIdx)) {
          List<Integer> actualPartitionAssignment = actualAssignments.get(partition);
          List<Integer> idealPartitionAssignment = idealAssignments.get(partition);
          boolean disableShrink = false;
          if (actualPartitionAssignment.size() < idealReplicationFactor) {
            // if replica is missing (# of replicas in assignment < replication factor)
            // we don't want the shrink phase to happen because we don't want to drop partitions
            disableShrink = true;
          }
          if (actualPartitionAssignment.size() > replicaIdx &&
              actualPartitionAssignment.get(replicaIdx)
              .equals(idealPartitionAssignment.get(replicaIdx))) {
            // check if current replica exists and is correct, do nothing
            continue;
          }
          int idealReplicaIdx = actualPartitionAssignment.indexOf(idealPartitionAssignment.get(replicaIdx));
          if (idealReplicaIdx != -1) {
            // replica is already in the assignment, but position is off. don't shrink, swap on expansion
            // swap is safe to happen even if replication factor is not restored yet. (because swap doesn't drop replicas)
            List<Integer> expandPartitionAssignment = new ArrayList<>(actualPartitionAssignment);
            Collections.swap(expandPartitionAssignment, idealReplicaIdx, replicaIdx);
            expandReassignments.put(partition, expandPartitionAssignment);
          } else {
            // ideal replica assignment is NOT in actual assignment. remove actual in shrink phase
            // and replace it with the ideal one in expand phase
            // if replication factor is lower than ideal, don't shrink
            List<Integer> partitionAssignment = new ArrayList<>(actualPartitionAssignment);
            if( !disableShrink ) {
              partitionAssignment.remove(replicaIdx);
              shrinkReassignments.put(partition, partitionAssignment);
              partitionAssignment = new ArrayList<>(partitionAssignment);
            }

            // if the shrink phase happen, we are essentially replacing the original actual replica
            // otherwise, we put the right broker here and move the original actual replica backwards
            partitionAssignment.add(replicaIdx, idealPartitionAssignment.get(replicaIdx));
            expandReassignments.put(partition, partitionAssignment);
          }
        }
        Map<String, Map<Integer, List<Integer>>> kafkaReassignment = null;
        Action shrinkReassignmentAction = null;
        Action expandReassignmentAction = null;
        boolean skipShrink = shrinkReassignments.isEmpty();
        boolean skipExpand = expandReassignments.isEmpty();
        if (!skipShrink){
          shrinkReassignmentAction = new ShrinkReassignmentAction(replicaIdx, batchIdx + 1, batchedPartitions.size());
          kafkaReassignment = newKafkaReassignmentFromTopic(topic, shrinkReassignments);
          shrinkReassignmentAction.setAttribute(ReassignmentAction.ATTR_REASSIGNMENT_KEY, kafkaReassignment);
          shrinkReassignmentAction.setAttribute(CONF_METADATA_FETCH_TIMEOUT_MS_KEY, metadataTimeoutMs);
          this.getChildren().add(shrinkReassignmentAction);
        }

        if (!skipExpand){
          expandReassignmentAction = new ExpandReassignmentAction(replicaIdx, batchIdx + 1, batchedPartitions.size());
          kafkaReassignment = newKafkaReassignmentFromTopic(topic, expandReassignments);
          expandReassignmentAction.setAttribute(ReassignmentAction.ATTR_REASSIGNMENT_KEY, kafkaReassignment);
          expandReassignmentAction.setAttribute(CONF_METADATA_FETCH_TIMEOUT_MS_KEY, metadataTimeoutMs);
          this.getChildren().add(expandReassignmentAction);
        }

        if(!skipShrink){
          // shrink phase:
          getEngine().dispatchChild(this, shrinkReassignmentAction);
          logger.info("Waiting for ideal balancing shrink phase");
          shrinkReassignmentAction.get();
          if(!shrinkReassignmentAction.isSuccess()){
            markFailed("Shrink reassignment of topic: " + topic + " partitions: " + shrinkReassignments.keySet() + " failed.");
            return;
          }
        }

        if(!skipExpand){
          // expand phase:
          getEngine().dispatchChild(this, expandReassignmentAction);
          logger.info("Waiting for ideal balancing expansion phase");
          expandReassignmentAction.get();
          if(!expandReassignmentAction.isSuccess()){
            markFailed("Expand reassignment of topic: " + topic + " partitions: " + shrinkReassignments.keySet() + " failed.");
            return;
          }
          // if everything is good, expandReassignments are now the actual assignments so we updated
          // actualAssignments for the next iteration
          actualAssignments.putAll(expandReassignments);
        }
      }
    }

    // handle replication factor shrinking
    if (actualReplicationFactor > idealReplicationFactor) {
      Action reassignmentAction = new ShrinkReplicationFactorAction(actualReplicationFactor, idealReplicationFactor);
      Map<String, Map<Integer, List<Integer>>> kafkaReassignment = newKafkaReassignmentFromTopic(topic, idealAssignments);
      reassignmentAction.setAttribute(ReassignmentAction.ATTR_REASSIGNMENT_KEY, kafkaReassignment);
      reassignmentAction.setAttribute(CONF_METADATA_FETCH_TIMEOUT_MS_KEY, metadataTimeoutMs);
      this.getChildren().add(reassignmentAction);
      getEngine().dispatchChild(this, reassignmentAction);
      logger.info("Shrinking replication factor");
      reassignmentAction.get();
      if(!reassignmentAction.isSuccess()) {
        markFailed("Failed to shrink replication factor of topic " + topic);
        return;
      }
    }
    markSucceeded();
  }

  private static Map<String, Map<Integer, List<Integer>>> newKafkaReassignmentFromTopic(String topic, Map<Integer, List<Integer>> topicReassignment) {
    return Collections.singletonMap(topic, topicReassignment);
  }

  @Override
  public String getName() {
    return "Ideal Balance Topic " + getAttribute(this, ATTR_TOPIC_KEY).getValue();
  }

  private static class ShrinkReassignmentAction extends ReassignmentAction {

    private final String subtitle;

    public ShrinkReassignmentAction (int replicaIdx, int currentBatch, int totalBatch) {
      this.subtitle = "Replica " + replicaIdx + " Batch: " + currentBatch + "/" + totalBatch;
    }

    @Override
    public String getName() {
      return "Shrinking Reassignment - " + subtitle;
    }
  }

  private static class ExpandReassignmentAction extends ReassignmentAction {

    private final String subtitle;

    public ExpandReassignmentAction (int replicaIdx, int currentBatch, int totalBatch) {
      this.subtitle = "Replica " + replicaIdx + " Batch: " + currentBatch + "/" + totalBatch;
    }

    @Override
    public String getName() {
      return "Expanding Reassignment - " + subtitle;
    }
  }

  private static class ShrinkReplicationFactorAction extends ReassignmentAction {

    private final String subtitle;

    public ShrinkReplicationFactorAction (int oldRF, int newRF) {
      this.subtitle = oldRF + " -> " + newRF;
    }

    @Override
    public String getName() {
      return "Shrinking replication factor - " + subtitle;
    }
  }
}
