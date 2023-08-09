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

import com.pinterest.orion.server.OrionServer;
import org.apache.kafka.clients.admin.AdminClient;

import com.pinterest.orion.core.Attribute;
import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.alert.AlertLevel;
import com.pinterest.orion.core.actions.alert.AlertMessage;
import com.pinterest.orion.core.automation.conflicts.MinIsrRfConflict;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class MinIsrRfConflictResolutionAction extends AbstractKafkaAction {

    public static String ATTR_CONFLICT_OBJ_KEY = "conflictObj";

    private static Logger logger = Logger.getLogger(MinIsrRfConflictResolutionAction.class.getName());

    @Override
    public void run(String zkUrl, AdminClient adminClient) {

        if (!containsAttribute(ATTR_CONFLICT_OBJ_KEY)) {
            markFailed("Missing Conflict object attribute");
            return;
        }

        Attribute conflictObjAttr = getAttribute(ATTR_CONFLICT_OBJ_KEY);
        MinIsrRfConflict conflict = conflictObjAttr.getValue();

        String topicName = conflict.getTopicName();
        boolean topicInLCONF = conflict.isTopicInLCONF();
        int clusterDefaultRf = conflict.getClusterDefaultRf();
        int currRf = conflict.getCurrRf();
        int currMinIsr = conflict.getCurrMinIsr();

        if (currRf < 1) {
            // something is wrong. most likely incorrect assignment that led to sampled RF mismatch
            String title = "Detected mismatch in replicas size for sampled partitions";
            String message = "Topic: " + topicName + ", cluster: " + getEngine().getCluster().getClusterId();
            AlertMessage alertMessage = new AlertMessage(title, message, "orion");
            getEngine().alert(AlertLevel.MEDIUM, alertMessage);
            markSucceeded();
            return ;
        } else if (topicInLCONF) {
            String title = "min.insync.replicas conflicts with replication factor for topic " +
                    topicName + " on cluster " + getEngine().getCluster().getClusterId();
            String message =
                    "min.insync.replicas = " + currMinIsr + " > " + currRf + " = replication factor for topic " +
                    topicName + " on cluster " + getEngine().getCluster().getClusterId();
            AlertMessage alertMessage = new AlertMessage(title, message, "orion");
            getEngine().alert(AlertLevel.HIGH, alertMessage);
            OrionServer.metricsGaugeNum(
                    "replicationfactor.minisr.conflict", 1,
                    new HashMap<String, String>() {{
                        put("cluster", getEngine().getCluster().getClusterId());
                        put("topic", topicName);
                    }}
            );
            markSucceeded();
            logger.info("Sent pager for minIsr-Rf conflict for topic " + topicName + " on cluster " +
                    getEngine().getCluster().getClusterId());
        } else {
            int newMinIsr = currRf;
            Map<String, String> targetConfig = new HashMap<>();
            targetConfig.put("min.insync.replicas", Integer.toString(newMinIsr));
            Action configUpdateAction = getUpdateMinIsrConfigAction(topicName, targetConfig);
            getChildren().add(configUpdateAction);
            try {
                getEngine().dispatchChild(this, configUpdateAction);
                String title = "Action dispatched to set minIsr = RF for topic " + topicName + " on cluster " +
                        getEngine().getCluster().getClusterId() + ".";
                String message = "min.insync.replicas was " + currMinIsr + ". Setting it to " + newMinIsr +
                        " to match RF on the topic.";
                AlertMessage alertMessage = new AlertMessage(title, message, "orion");
                getEngine().alert(AlertLevel.MEDIUM, alertMessage);
                logger.info("Sent slack alert for minIsr-Rf conflict for topic " + topicName + " on cluster " +
                        getEngine().getCluster().getClusterId());
                if (!configUpdateAction.get().isSuccess()) {
                    markFailed("Config update action for topic " + topicName + " failed");
                    return;
                }
            } catch (Exception e) {
                logger.warning("Failed to dispatch config update action for topic " + topicName);
                markFailed(e);
            }
            markSucceeded();
        }
    }

    private Action getUpdateMinIsrConfigAction(String topicName, Map<String, String> idealConfig) {
        Action configUpdateAction = new KafkaTopicConfigUpdateAction();
        configUpdateAction.setAttribute(KafkaTopicConfigUpdateAction.ATTR_TOPIC_NAME_KEY, topicName);
        configUpdateAction.setAttribute(KafkaTopicConfigUpdateAction.ATTR_TOPIC_IDEAL_CONFIG_KEY, idealConfig);
        return configUpdateAction;
    }

    @Override
    public String getName() {
        return "MinIsrRfConflictResolutionAction";
    }
}
