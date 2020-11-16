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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import kafka.Kafka;
import org.apache.curator.framework.CuratorFramework;
import org.apache.kafka.clients.admin.AdminClient;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pinterest.orion.core.Attribute;
import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.kafka.KafkaCluster;
import com.pinterest.orion.core.kafka.KafkaTopicDescription;
import com.pinterest.orion.core.kafka.KafkaTopicPartitionInfo;
import com.pinterest.orion.core.utils.kafka.CuratorClient;

import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;

public class ReassignmentAction extends AbstractKafkaAction {

  private static final Logger logger = Logger
      .getLogger(ReassignmentAction.class.getCanonicalName());
  public static final String REASSIGNMENT_PATH = "/admin/reassign_partitions";
  public static final String ATTR_REASSIGNMENT_KEY = "reassignment";

  @SuppressWarnings("unchecked")
  @Override
  public void run(String zkString, AdminClient adminClient) {
    Boolean waitForPrevious = false;
    Attribute attribute = getAttribute(ATTR_REASSIGNMENT_KEY);
    KafkaCluster kakfaCluster = (KafkaCluster) getEngine().getCluster();
    if (containsAttribute("wait_for_previous")) {
      waitForPrevious = getAttribute(this, "wait_for_previous").getValue();
    }
    if (attribute != null) {
      Map<String, Map<Integer, List<Integer>>> assignmentMap = attribute.getValue();
      // convert assignment map to json
      String assignmentJson = assignmentMapToJson(assignmentMap);
      // trigger reassignments
      try (CuratorFramework zkClient = CuratorClient.buildAndGetZkClient(zkString)) {
        zkClient.start();

        if (!zkClient.blockUntilConnected(10, TimeUnit.SECONDS)) {
          markFailed("Could not connect to zookeeper: " + zkString);
          return;
        }

        if (zkClient.checkExists().forPath(REASSIGNMENT_PATH) != null && !waitForPrevious) {
          markFailed("Reassignment already running");
          return;
        } else {
          CuratorClient.waitForZnodeToBeDeleted(zkClient, REASSIGNMENT_PATH);
        }

        zkClient.create().forPath(REASSIGNMENT_PATH, assignmentJson.getBytes());
        waitForISRsToMatchAssignments(kakfaCluster, assignmentMap, 15_000);
        waitForURPsToBeResolved(kakfaCluster, 15_000);
        CuratorClient.waitForZnodeToBeDeleted(zkClient, REASSIGNMENT_PATH);
        markSucceeded();
      } catch (Exception e) {
        markFailed(e);
      }
    }
  }

  private void waitForISRsToMatchAssignments(KafkaCluster cluster,
                                             Map<String, Map<Integer, List<Integer>>> assignments,
                                             long checkFrequencyMillis)
      throws Exception {
    while (!doesISRsMatchAssignments(cluster, assignments)) {
      logger.info("Waiting for ISRs to match assignments");
      Thread.sleep(checkFrequencyMillis);
    }
  }

  private boolean doesISRsMatchAssignments(KafkaCluster cluster,
                                           Map<String, Map<Integer, List<Integer>>> assignments)
      throws Exception {
    Map<String, Map<Integer, Set<Integer>>> isrs =
        cluster.getTopicDescriptionFromKafka().values().stream().collect(Collectors.toMap(
            KafkaTopicDescription::getName,
            td -> td.getPartitions().stream().collect(Collectors.toMap(
                KafkaTopicPartitionInfo::getPartition,
                tpi -> tpi.getIsrs().stream().map(Node::id).collect(Collectors.toSet())
            )
    )));
    for(Map.Entry<String, Map<Integer, List<Integer>>> entry : assignments.entrySet()) {
      Map<Integer, Set<Integer>> topicISRs = isrs.get(entry.getKey());
      if (topicISRs == null) {
        throw new IllegalArgumentException("Topic " + entry.getKey() + " does not exist in cluster");
      }
      Map<Integer, List<Integer>> topicAssignments = entry.getValue();
      for (Map.Entry<Integer, List<Integer>> pEntry : topicAssignments.entrySet()) {
        Set<Integer> pIsrs = topicISRs.get(pEntry.getKey());
        if(!(pIsrs.size() == pEntry.getValue().size() && pIsrs.containsAll(pEntry.getValue()))) {
          getResult().appendOut("Waiting for topic: "+ entry.getKey() + " Partition " + pEntry.getKey() + " ISR to match with assignment\n");
          return false;
        }
      }
    }
    return true;
  }

  public void waitForURPsToBeResolved(KafkaCluster cluster,
                                      int checkFrequencyMillis) throws InterruptedException {
    while (!cluster.clusterHealthy()) {
      logger.info("Waiting for URPs to be resolved during reassignment");
      Thread.sleep(checkFrequencyMillis);
    }
  }

  public static String assignmentMapToJson(Map<String, Map<Integer, List<Integer>>> assignmentMap) {
    Gson gson = new Gson();
    JsonObject obj = new JsonObject();
    JsonArray assignments = new JsonArray();
    obj.add("partitions", assignments);
    for (Entry<String, Map<Integer, List<Integer>>> entry : assignmentMap.entrySet()) {
      String topic = entry.getKey();
      Map<Integer, List<Integer>> td = entry.getValue();
      for (Entry<Integer, List<Integer>> topicPartitionInfo : td.entrySet()) {
        JsonObject assignmentEntry = new JsonObject();
        assignmentEntry.addProperty("topic", topic);
        assignmentEntry.addProperty("partition", topicPartitionInfo.getKey());
        JsonArray assignmentArray = new JsonArray();
        for (Integer brokerId : topicPartitionInfo.getValue()) {
          assignmentArray.add(brokerId);
        }
        assignmentEntry.add("replicas", assignmentArray);
        assignments.add(assignmentEntry);
      }
    }
    return gson.toJson(obj);
  }

  @Override
  public String getName() {
    return "Partition Reassignment";
  }

}