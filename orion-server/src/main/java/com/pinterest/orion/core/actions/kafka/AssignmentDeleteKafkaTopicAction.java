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

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DeleteTopicsResult;

import java.util.Collections;

public class AssignmentDeleteKafkaTopicAction extends AbstractKafkaAction {
    public static final String ATTR_TOPIC_NAME_KEY = "topic";

    @Override
    public void run(String zkUrl, AdminClient adminClient) {
        checkRequiredArgs(new String[] { ATTR_TOPIC_NAME_KEY });
        String topicName = getAttribute(this, ATTR_TOPIC_NAME_KEY).getValue().toString();
        DeleteTopicsResult result = adminClient.deleteTopics(Collections.singletonList(topicName));
        try {
            result.all().get();
        } catch (Exception e) {
            markFailed(e);
            return;
        }

        try {
            onTopicDeleted(topicName);
        } catch (Exception e) {
            markFailed(e);
            return;
        }
        markSucceeded();
    }

    protected void onTopicDeleted(String topicName) throws Exception {
        // e.g. some sort of notification
    }

    @Override
    public String getName() {
        return "Delete Kafka Topic " + getAttribute(this, ATTR_TOPIC_NAME_KEY).getValue().toString();
    }
}
