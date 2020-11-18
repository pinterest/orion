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
import com.pinterest.orion.core.actions.kafka.KafkaTopicConfigUpdateAction;
import com.pinterest.orion.core.automation.sensor.kafka.KafkaClusterInfoSensor;
import com.pinterest.orion.core.automation.sensor.kafka.KafkaTopicSensor;
import com.pinterest.orion.core.kafka.KafkaCluster;
import com.pinterest.orion.core.kafka.KafkaTopicDescription;
import com.pinterest.orion.core.kafka.TopicAssignment;

import java.util.*;
import java.util.logging.Logger;

public class KafkaTopicConfigOperator extends KafkaOperator {

    private static final Logger logger = Logger
            .getLogger(KafkaTopicConfigOperator.class.getCanonicalName());
    private int maxSensorLagSeconds;

    @Override
    public void initialize(Map<String, Object> config) throws PluginConfigurationException {
        super.initialize(config);
        maxSensorLagSeconds = (int) config.getOrDefault("maxSensorLagSeconds", 90);
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

        Set<String> sensorSet = new HashSet<>();

        Attribute topicDescriptionMapAttr = cluster
                .getAttribute(KafkaTopicSensor.ATTR_TOPICINFO_MAP_KEY);
        Map<String, KafkaTopicDescription> topicDescriptionMap = topicDescriptionMapAttr.getValue();
        sensorSet.addAll(topicDescriptionMapAttr.getPublishingSensors());

        Attribute topicAssignmentsMapAttr = cluster
                .getAttribute(KafkaClusterInfoSensor.ATTR_TOPIC_ASSIGNMENTS_KEY);
        List<TopicAssignment> topicAssignments = topicAssignmentsMapAttr.getValue();
        sensorSet.addAll(topicAssignmentsMapAttr.getPublishingSensors());

        if (System.currentTimeMillis() - topicDescriptionMapAttr.getUpdateTimestamp() > maxSensorLagSeconds * 1000) {
            logger.warning("KafkaTopicSensor publish time is more than " + maxSensorLagSeconds + " seconds" +
                    " ago, performing a no-op");
            return;
        }

        logger.info("Traversing through topicassignments configs to find deltas between ideal" +
                " and actual for " + topicAssignments.size() + " topics");
        for (TopicAssignment topicAssignment: topicAssignments) {
            String topicName = topicAssignment.getTopicName();
            Map<String, String> topicIdealConfig = topicAssignment.getConfig();
            KafkaTopicDescription actualTopicDescription = topicDescriptionMap.get(topicName);
            if (actualTopicDescription == null) {
               logger.warning("Topic " + topicName + " doesn't exist yet on the cluster, skipping config diff");
               continue;
            }
            Map<String, String> actualTopicConfig = actualTopicDescription.getTopicConfigs();
            if (!topicConfigIsIdeal(actualTopicDescription, topicIdealConfig)) {
                // topic configs unideal, need to fire action
                Action topicConfigUpdateAction = createTopicConfigUpdateAction(topicName,
                        topicIdealConfig, actualTopicConfig, sensorSet);
                logger.info("Topic(" + topicName + ") configs are not ideal. Dispatching action.");
                dispatch(topicConfigUpdateAction);
                return;
            }
        }
        logger.info("Configs for all topics are ideal");
    }

    private boolean topicConfigIsIdeal(KafkaTopicDescription topicDescription,
                                       Map<String, String> topicIdealConfig) {
        Set<String> overrideConfigs = topicDescription.getOverrideConfigs();
        Map<String, String> actualTopicConfig = topicDescription.getTopicConfigs();
        if (overrideConfigs.isEmpty() && topicIdealConfig.isEmpty()) {
            // nothing is currently overridden and nothing to override: no action needed
            return true;
        } else if (overrideConfigs.isEmpty() && !topicIdealConfig.isEmpty()) {
            // nothing is currently overridden and user specified override: topic is unideal
            return false;
        } else if (!overrideConfigs.isEmpty() && !topicIdealConfig.isEmpty()) {
            // currently overriding configs and user specified override: need to check diff
            for (Map.Entry<String, String> actualConfigEntry: actualTopicConfig.entrySet()) {
                String actualConfigKey = actualConfigEntry.getKey();
                String actualConfigValue = actualConfigEntry.getValue();
                String idealConfigValue = topicIdealConfig.get(actualConfigKey);
                if (idealConfigValue == null && overrideConfigs.contains(actualConfigKey)) {
                    // currently overriding this config but need to reset to default
                    return false;
                } else if (idealConfigValue != null && !actualConfigValue.equals(idealConfigValue)) {
                    // ideal is not actual, requires action
                    return false;
                }
            }
            return true;
        } else {
            // currently overriding configs but no user specified override: need to revert to default configs
            return false;
        }
    }

    private Action createTopicConfigUpdateAction(String topicName,
                                                 Map<String, String> idealConfigs,
                                                 Map<String, String> actualConfigs,
                                                 Set<String> sensorSet) {
        Action topicConfigUpdateAction = new KafkaTopicConfigUpdateAction();
        topicConfigUpdateAction.setAttribute(KafkaTopicConfigUpdateAction.ATTR_TOPIC_NAME_KEY,
                topicName, sensorSet);
        topicConfigUpdateAction.setAttribute(KafkaTopicConfigUpdateAction.ATTR_TOPIC_IDEAL_CONFIG_KEY,
                idealConfigs, sensorSet);
        topicConfigUpdateAction.setAttribute(KafkaTopicConfigUpdateAction.ATTR_TOPIC_ACTUAL_CONFIG_KEY,
                actualConfigs, sensorSet);
        return topicConfigUpdateAction;
    }

    @Override
    public String getName() {
        return "TopicConfigOperator";
    }
}
