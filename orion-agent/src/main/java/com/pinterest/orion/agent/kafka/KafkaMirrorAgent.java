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
package com.pinterest.orion.agent.kafka;

import com.pinterest.orion.agent.BaseAgent;
import com.pinterest.orion.agent.OrionAgentConfig;
import com.pinterest.orion.agent.utils.OrionCmd;
import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.common.StatusInfo;

import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class KafkaMirrorAgent extends BaseAgent {

    private static final Logger logger = Logger.getLogger(KafkaMirrorAgent.class.getCanonicalName());
    public static final String KAFKAMIRROR_CONFIG_DIRECTORY = "mirrorConfigDirectory";

    public KafkaMirrorAgent(OrionAgentConfig config) throws IOException {
        super(config);
    }

    @Override
    protected int getMetricsPort() {
        return 9999;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public List<String> getEntityValues(String entity) {
        switch (entity) {
            case "topic":
                return getJMXPropertyValuesForKey("kafka.consumer:*", "topic");
            case "host":
                return Collections.singletonList(config.getHostname());
            case "consumer-client-id":
                return getJMXPropertyValuesForKey("kafka.consumer:*", "client-id");
            case "producer-client-id":
                return getJMXPropertyValuesForKey("kafka.producer:*", "client-id");
        }
        return null;
    }

    private List<String> getJMXPropertyValuesForKey(String jmxMetricPrefix, String key) {
        Set<String> result = new HashSet<>();
        try {
            Set<ObjectInstance> objects = mbs.queryMBeans(new ObjectName(jmxMetricPrefix), null);
            for (ObjectInstance obj: objects) {
                String match = obj.getObjectName().getKeyProperty(key);
                if (match != null) {
                    result.add(match);
                }
            }
        } catch (Exception e) {
            logger.severe("Failed to get " + key + " JMX property values for prefix " + jmxMetricPrefix);
            return Collections.emptyList();
        }
        logger.info("Retrieved JMX property values " + result + " for metric prefix: " + jmxMetricPrefix + ", key: " + key);
        return new ArrayList<>(result);
    }

    @Override
    public StatusInfo getAgentStatus() throws Exception {
        return null;
    }

    @Override
    public OrionCmd startService() throws Exception {
        return null;
    }

    @Override
    public OrionCmd stopService() throws Exception {
        return null;
    }

    @Override
    public OrionCmd restartService() throws Exception {
        return null;
    }

    @Override
    public OrionCmd updateConfigs() throws Exception {
        return null;
    }

    @Override
    public OrionCmd upgradeAgent() throws Exception {
        return null;
    }

    @Override
    public OrionCmd upgradeService() throws Exception {
        return null;
    }

    @Override
    public StatusInfo getServiceStatus() throws Exception {
        return null;
    }

    @Override
    public NodeInfo getNodeInfo() throws Exception {
        return null;
    }

    @Override
    public OrionCmd probeNetstat() throws Exception {
        return null;
    }
}
