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

import org.apache.kafka.clients.admin.AdminClient;

import com.pinterest.orion.core.Attribute;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.kafka.MinIsrRfConflictResolutionAction;
import com.pinterest.orion.core.automation.conflicts.MinIsrRfConflict;
import com.pinterest.orion.core.automation.sensor.kafka.KafkaClusterInfoSensor;
import com.pinterest.orion.core.automation.sensor.kafka.KafkaTopicSensor;
import com.pinterest.orion.core.kafka.KafkaCluster;
import com.pinterest.orion.core.kafka.KafkaTopicDescription;
import com.pinterest.orion.core.kafka.TopicAssignment;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ConfigConflictOperator extends KafkaOperator {

    private static final Logger logger = Logger
            .getLogger(ConfigConflictOperator.class.getCanonicalName());
    private static final String MINISR_RF_CONFLICT_KEY = "minIsrRfConflict";

    @Override
    public void initialize(Map<String, Object> config) throws PluginConfigurationException {
        super.initialize(config);
    }

    @Override
    public void operate(KafkaCluster cluster) throws Exception {
        AdminClient adminClient = cluster.getAdminClient();
        if (adminClient == null) {
            return;
        } else if (!cluster.containsAttribute(KafkaTopicSensor.ATTR_TOPICINFO_MAP_KEY)
                || !cluster.containsAttribute(KafkaClusterInfoSensor.ATTR_TOPIC_ASSIGNMENTS_KEY)) {
            return;
        }
        Attribute topicAssignmentsMapAttr = cluster
                .getAttribute(KafkaClusterInfoSensor.ATTR_TOPIC_ASSIGNMENTS_KEY);
        List<TopicAssignment> topicAssignments = topicAssignmentsMapAttr.getValue();
        Map<String, TopicAssignment> topicAssignmentsMap = topicAssignments.stream().collect(
                Collectors.toMap(TopicAssignment::getTopicName, Function.identity()));
        Attribute topicDescriptionMapAttr = cluster
                .getAttribute(KafkaTopicSensor.ATTR_TOPICINFO_MAP_KEY);
        Map<String, KafkaTopicDescription> topicDescriptionMap = topicDescriptionMapAttr.getValue();
        logger.info("Starting conflict detection for cluster " + cluster.getClusterId());
        for (Map.Entry<String, KafkaTopicDescription> topicDescriptionEntry : topicDescriptionMap.entrySet()) {
            String topicName = topicDescriptionEntry.getKey();
            KafkaTopicDescription actualTopicDescription = topicDescriptionEntry.getValue();
            TopicAssignment topicAssignment = topicAssignmentsMap.get(topicName);
            Action minIsrRfConflictResolutionAction = getActionForConflict(MINISR_RF_CONFLICT_KEY,
                    actualTopicDescription, cluster, topicAssignment);
            if (minIsrRfConflictResolutionAction != null) {
                logger.info("Detected MinIsrRfConflict for topic " + topicName + " on cluster " + cluster.getClusterId() +
                        ". Dispatching action to resolve.");
                dispatch(minIsrRfConflictResolutionAction);
                return;
            }
        }
        logger.info("No config conflicts detected for cluster " + cluster.getClusterId());
    }

    private Action getActionForConflict(String conflictRuleKey,
                                        KafkaTopicDescription actualTopicDescription,
                                        KafkaCluster cluster,
                                        TopicAssignment topicAssignment) {
        switch (conflictRuleKey) {
            case MINISR_RF_CONFLICT_KEY:
                MinIsrRfConflict conflict = getMinIsrRfConflict(actualTopicDescription, cluster, topicAssignment);
                return conflict.detectConflict() ? getMinIsrRfConflictResolutionAction(conflict) : null;
            default:
                return null;
        }
    }

    private Action getMinIsrRfConflictResolutionAction(MinIsrRfConflict conflict) {
        Action action = new MinIsrRfConflictResolutionAction();
        action.setAttribute(MinIsrRfConflictResolutionAction.ATTR_CONFLICT_OBJ_KEY, conflict);
        return action;
    }

    private MinIsrRfConflict getMinIsrRfConflict(KafkaTopicDescription actualTopicDescription,
                                                    KafkaCluster cluster,
                                                    TopicAssignment topicAssignment) {
        MinIsrRfConflict conflict = new MinIsrRfConflict();
        int sampledReplicationFactor = actualTopicDescription.getSampledReplicationFactor();
        int currMinIsr = Integer.parseInt(actualTopicDescription.getTopicConfigs().get("min.insync.replicas"));
        conflict.setTopicName(actualTopicDescription.getName());
        conflict.setTopicInLCONF(topicAssignment != null);
        conflict.setCurrRf(sampledReplicationFactor);
        conflict.setCurrMinIsr(currMinIsr);
        conflict.setClusterDefaultRf(cluster.getClusterDefaultReplicationFactor());
        return conflict;
    }

    @Override
    public String getName() {
        return "ConfigConflictOperator";
    }
}
