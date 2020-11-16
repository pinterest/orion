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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.ActionEngine;
import com.pinterest.orion.core.actions.alert.AlertMessage;
import com.pinterest.orion.core.automation.sensor.kafka.KafkaStuckConsumerGroupSensor;

public class KafkaStuckConsumerGroupRecoveryAction extends Action {
  private static final String CONF_STABLE_TIMEOUT_SECONDS_KEY = "stableTimeoutSeconds";
  private static final String CONF_DRY_RUN_KEY = "dry_run";
  private long stableTimeoutSeconds = 600; // 10 minutes based on empirical results
  private boolean isDryRun = false;

  public static final String ATTR_GROUP_IDS_KEY = "groupIds";
  public static final String ATTR_PARTITION_ASSIGNMENT_KEY = "assignment";
  private static final String[] requiredAttrs = new String[]{ATTR_GROUP_IDS_KEY, ATTR_PARTITION_ASSIGNMENT_KEY};

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    super.initialize(config);
    if(config.containsKey(CONF_STABLE_TIMEOUT_SECONDS_KEY)) {
      stableTimeoutSeconds = Long.parseLong(config.get(CONF_STABLE_TIMEOUT_SECONDS_KEY).toString());
    }
    if(config.containsKey(CONF_DRY_RUN_KEY)) {
      isDryRun = Boolean.parseBoolean(config.get(CONF_DRY_RUN_KEY).toString());
    }
  }

  @Override
  public void runAction() throws Exception {
    for (String requiredAttr : requiredAttrs) {
      if (!containsAttribute(requiredAttr)) {
        markFailed("Missing attribute " + requiredAttr);
        return;
      }
    }

    Set<String> groupIds = getAttribute(ATTR_GROUP_IDS_KEY).getValue();
    Map<String, Map<Integer, List<Integer>>> originalAssignment = getAttribute(ATTR_PARTITION_ASSIGNMENT_KEY).getValue();
    String topic = originalAssignment.keySet().iterator().next();
    Set<Integer> partitions = originalAssignment.get(topic).keySet();

    Cluster cluster = getEngine().getCluster();
    String clusterId = cluster.getClusterId();

    getEngine().alert(ActionEngine.AlertLevel.MEDIUM,
        new AlertMessage(
            "Consumer group(s) on " + clusterId + " stuck in REBALANCING",
            "Group(s) "+ groupIds + " are stuck. " +
                (isDryRun ? "Dry run mode, please handle manually." : "Swapping leaders of partition(s): " + partitions + " of " + topic + " on " + clusterId),
            "orion"
        ));

    if (isDryRun) {
      markSucceeded();
      return;
    }

    try {
      this.getEngine().getCluster().setMaintenance(true);

      Map<Integer, List<Integer>> swappedAssignmentsMap = new HashMap<>();
      for(Map.Entry<Integer, List<Integer>> entry : originalAssignment.get(topic).entrySet()) {
        List<Integer> swappedAssignment = new ArrayList<>(entry.getValue());
        int prevLeader = swappedAssignment.remove(0);
        swappedAssignment.add(prevLeader);
        swappedAssignmentsMap.put(entry.getKey(), swappedAssignment);
      }

      Action swapLeaderAction = new ReassignmentAction();
      Action restoreLeaderAction = new ReassignmentAction();
      this.getChildren().add(swapLeaderAction);
      this.getChildren().add(restoreLeaderAction);

      swapLeaderAction.setAttribute(
          ReassignmentAction.ATTR_REASSIGNMENT_KEY,
          Collections.singletonMap(topic, swappedAssignmentsMap)
      );

      restoreLeaderAction.setAttribute(
          ReassignmentAction.ATTR_REASSIGNMENT_KEY,
          originalAssignment
      );

      getResult().appendOut("Swapping leader to bump coordinator...");
      getEngine().dispatchChild(this, swapLeaderAction);
      swapLeaderAction.get();

      if (!swapLeaderAction.isSuccess()) {
        markFailed("Leader swap failed");
        return;
      }
      getResult().appendOut("Waiting for consumer group to leave stuck state");

      // check if the CG state is unstuck
      Set<String> stuckGroupIds = checkIfGroupsAreUnstuck(cluster, groupIds);

      if (!stuckGroupIds.isEmpty()) {
        getResult().appendOut("Failed to stablize the consumer group to steady state after " + stableTimeoutSeconds + " seconds, restoring leader swap");
        getEngine().alert(ActionEngine.AlertLevel.MEDIUM, new AlertMessage("Failed to stablize the " + clusterId + " consumer group(s) " + stuckGroupIds + " after " + stableTimeoutSeconds + " seconds",
            "Restoring leader swap on __consumer_offsets back to previous assignment, please check the cluster for any degradation.",
            "orion"));
      }

      getEngine().dispatchChild(this, restoreLeaderAction);
      restoreLeaderAction.get();

      if (!restoreLeaderAction.isSuccess()) {
        markFailed("Failed to restore leader");
        return;
      }

      stuckGroupIds = checkIfGroupsAreUnstuck(cluster, stuckGroupIds);

      if (stuckGroupIds.isEmpty()) {
        markSucceeded();
      } else {
        markFailed("Consumer are still be in a stuck state after swapping leaders back. Please check the cluster/application");
      }
      return;
    } finally {
      this.getEngine().getCluster().setMaintenance(false);
    }

  }

  protected Set<String> checkIfGroupsAreUnstuck(Cluster cluster, Set<String> groupIds) throws InterruptedException {
    long stableTimeoutStart = System.currentTimeMillis();
    Set<String> ret = new HashSet<>(groupIds);
    while(System.currentTimeMillis() - stableTimeoutStart < stableTimeoutSeconds * 1000) {
      if (cluster.containsAttribute(KafkaStuckConsumerGroupSensor.ATTR_STUCK_CONSUMER_GROUPS_ID_KEY)) {
        Set<String>
            newGroupIds = cluster.getAttribute(KafkaStuckConsumerGroupSensor.ATTR_STUCK_CONSUMER_GROUPS_ID_KEY).getValue();
        Iterator<String> itr = ret.iterator();
        while (itr.hasNext()) {
          if(!newGroupIds.contains(itr.next())) {
            // group is unstuck, swap back
            getResult().appendOut("Consumer group is unstuck, restoring leader swap");
            itr.remove();
          }
        }
        if (ret.isEmpty()) {
          return ret;
        }
      } else {
        logger().warning("Missing cluster attribute" + KafkaStuckConsumerGroupSensor.ATTR_STUCK_CONSUMER_GROUPS_ID_KEY);
        getResult().appendOut("Missing cluster attribute " + KafkaStuckConsumerGroupSensor.ATTR_STUCK_CONSUMER_GROUPS_ID_KEY);
      }
      Thread.sleep(10_000);
    }
    return ret;
  }

  @Override
  public Type getActionType() {
    return Type.CLUSTER;
  }

  @Override
  public String getName() {
    return "StuckConsumerGroupRecovery" + (isDryRun ? " - Dry Run" : "");
  }

}
