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

import com.google.common.collect.Sets;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.kafka.ConsumerInfo;
import com.pinterest.orion.core.kafka.KafkaCluster;

import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.common.ConsumerGroupState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class KafkaStuckConsumerGroupSensor extends KafkaSensor {
  private static final String CONF_STUCK_THRESHOLD_SECONDS_KEY = "stuckThresholdSeconds";
  public static final String ATTR_STUCK_CONSUMER_GROUPS_ID_KEY = "stuckConsumerGroups";

  private static final Set<ConsumerGroupState> stuckStates = Sets.newHashSet(
      ConsumerGroupState.PREPARING_REBALANCE, ConsumerGroupState.COMPLETING_REBALANCE
  );
  private long stuckThresholdSeconds = 900; // 15 minutes
  private Map<String, StuckConsumerGroup> groupStateMap = new HashMap<>();

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    super.initialize(config);
    if (config.containsKey(CONF_STUCK_THRESHOLD_SECONDS_KEY)) {
      stuckThresholdSeconds = Long.parseLong(config.get(CONF_STUCK_THRESHOLD_SECONDS_KEY).toString());
    }
  }

  @Override
  public void sense(KafkaCluster cluster) throws Exception {
    if (!cluster.containsAttribute(KafkaClusterInfoSensor.ATTR_CONSUMER_INFO_KEY) ||
        !cluster.containsAttribute(KafkaConsumerGroupDescriptionSensor.ATTR_CONSUMER_GROUP_DESC_KEY)||
        !cluster.containsAttribute(KafkaTopicSensor.ATTR_TOPICINFO_MAP_KEY)
    ) {
      return;
    }

    Map<String, ConsumerInfo> consumerInfo = cluster.getAttribute(KafkaClusterInfoSensor.ATTR_CONSUMER_INFO_KEY).getValue();
    Map<String, ConsumerGroupDescription> consumerGroupDescriptionMap = cluster.getAttribute(KafkaConsumerGroupDescriptionSensor.ATTR_CONSUMER_GROUP_DESC_KEY).getValue();

    Set<String> p0GroupIds = consumerInfo.values().stream()
        .filter(c -> c.getTier() == ConsumerInfo.Tier.P0)
        .map(ConsumerInfo::getName)
        .collect(Collectors.toSet());

    Set<String> stuckConsumerGroupIds = new HashSet<>();

    for(String groupId : p0GroupIds) {
      /*
      0. consumer is not in the map, and consumer doesn't exist in the description map => cg doesn't exist, skip for now
      1. consumer is not in the map, and current state is stuck => add new entry to the map
      2. consumer is in the map, and current state is stuck => check if the entry timestamp difference exceeds the threshold, if true then alert and reset timestamp to now for alert cooldown
      3. consumer is in the map, but is not stuck => remove the entry
      */

      // 0. consumer is not in the map, and consumer doesn't exist in the description map
      ConsumerGroupDescription cgDesc = consumerGroupDescriptionMap.get(groupId);
      if(cgDesc == null) {
        continue;
      }

      boolean isStuck = stuckStates.contains(cgDesc.state());
      // 1. consumer is not in the map, and current state is stuck
      if(!groupStateMap.containsKey(groupId)) {
        if (isStuck) {
          StuckConsumerGroup
              scg = new StuckConsumerGroup();
          groupStateMap.put(groupId, scg);
          logger.info("consumer group " + groupId + " first record in stuck state at " + scg.getTimestamp());
        }
      } else {
        if (isStuck) {
          // 2. consumer is in the map, and current state is stuck
          StuckConsumerGroup scg = groupStateMap.get(groupId);

          scg.incrementCount();
          logger.info("consumer group " + groupId + " in stuck state for " + scg.getCount() + " iterations");

          if (System.currentTimeMillis() - scg.getTimestamp() > stuckThresholdSeconds * 1000) {
            logger.warning("Consumer group " + groupId + " has been stuck for more than " + stuckThresholdSeconds + " seconds");
            stuckConsumerGroupIds.add(groupId);
          }
        } else {
          // 3. consumer is in the map, but is not stuck => remove the entry
          groupStateMap.remove(groupId);
          logger.info("consumer group " + groupId + " out of stuck state (" + cgDesc.state() + ")");
        }
      }
    }
    cluster.setHiddenAttribute(ATTR_STUCK_CONSUMER_GROUPS_ID_KEY, stuckConsumerGroupIds);
    //4. consumer is in the map, and consumer doesn't exist in the description map => CG was deleted? need some sort of purging (LRU?), ignore for now

  }

  @Override
  public String getName() {
    return "StuckConsumerGroupSensor";
  }

  private static class StuckConsumerGroup {
    private long timestamp;
    private int count;

    public StuckConsumerGroup() {
      this.timestamp = System.currentTimeMillis();
      this.count = 0;
    }

    public void resetTimestamp() {
      this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
      return timestamp;
    }

    public int getCount() {
      return count;
    }

    public void incrementCount() {
      count++;
    }
  }
}
