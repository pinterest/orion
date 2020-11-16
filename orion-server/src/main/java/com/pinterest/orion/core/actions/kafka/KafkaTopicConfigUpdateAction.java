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

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;

import com.pinterest.orion.core.Attribute;

import java.util.*;

public class KafkaTopicConfigUpdateAction extends AbstractKafkaAction {

    public static String ATTR_TOPIC_NAME_KEY = "topic";
    public static String ATTR_TOPIC_IDEAL_CONFIG_KEY = "idealConfig";
    public static String ATTR_TOPIC_ACTUAL_CONFIG_KEY = "actualConfig";

    @Override
    public void run(String zkUrl, AdminClient adminClient) {
        if (!containsAttribute(ATTR_TOPIC_NAME_KEY) || !containsAttribute(ATTR_TOPIC_IDEAL_CONFIG_KEY)) {
            markFailed("Missing attributes");
            return ;
        }
        String topicName = getAttribute(this, ATTR_TOPIC_NAME_KEY).getValue();
        Attribute desiredConfigAttr = getAttribute(this, ATTR_TOPIC_IDEAL_CONFIG_KEY);
        Map<String, String> idealConfigMap = desiredConfigAttr.getValue();
        if (idealConfigMap == null) {
            idealConfigMap = new HashMap<>();
        }
        Collection<ConfigEntry> idealConfigEntries = new ArrayList<>();

        for (Map.Entry<String, String> idealConfig: idealConfigMap.entrySet()) {
            String configName = idealConfig.getKey();
            String configValue = idealConfig.getValue();
            idealConfigEntries.add(new ConfigEntry(configName, configValue));
        }
        Config idealConfig = new Config(idealConfigEntries);
        ConfigResource topicConfigResource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);  // would resource always exist?
        AlterConfigsResult result = adminClient.alterConfigs(Collections.singletonMap(topicConfigResource, idealConfig));
        try {
            result.all().get();
        } catch (Exception e) {
            markFailed(e);
        }
        markSucceeded();
    }

    @Override
    public String getName() {
        return "Update Kafka Topic Configuration";
    }
}
