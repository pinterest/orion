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

    private List<String> getTopicsList(String mirrorConfigDirectory) {
        String topicAllowlist = null;
        String mirrorConfigFilepath = mirrorConfigDirectory;
        try {
            File[] configFiles = new File(mirrorConfigDirectory).listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getName().endsWith(".conf");
                }
            });
            if (Objects.requireNonNull(configFiles).length != 1) {
                logger.severe("More than one file under " + mirrorConfigDirectory);
                return Collections.emptyList();
            }
            mirrorConfigFilepath = configFiles[0].getAbsolutePath();
            BufferedReader reader = new BufferedReader(new FileReader(mirrorConfigFilepath));
            String line = reader.readLine();
            while (line != null) {
                if (line.startsWith("TOPIC_ALLOWLIST")) {
                    topicAllowlist = line.replace("TOPIC_ALLOWLIST=", "");
                    break;
                }
                line = reader.readLine();
            }
        } catch (Exception e) {
            logger.severe("Error reading config file " + mirrorConfigFilepath);
        }
        if (topicAllowlist == null) {
            logger.severe("Error getting topic list from " + mirrorConfigFilepath);
            return Collections.emptyList();
        }
        logger.info("Topic allowlist extracted from " + mirrorConfigFilepath + ": " + topicAllowlist);
        return Arrays.asList(topicAllowlist.split(","));
    }

    @Override
    public List<String> getEntityValues(String entity) {
        switch (entity) {
            case "topic":
                return getTopicsList(config.getAgentConfigs().get(KAFKAMIRROR_CONFIG_DIRECTORY).toString());
            case "host":
                return Collections.singletonList(config.getHostname());
        }
        return null;
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
