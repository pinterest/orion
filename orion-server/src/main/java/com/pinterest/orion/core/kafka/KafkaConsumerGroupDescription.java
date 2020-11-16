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
package com.pinterest.orion.core.kafka;

import java.util.Collection;
import java.util.Map;

import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.common.TopicPartition;

public class KafkaConsumerGroupDescription {
    private final String groupId;
    private final String coordinator;
    private final boolean isSimpleConsumerGroup;
    private final String partitionAssignor;
    private final String state;
    private final Collection<MemberDescription> members;
    private final Map<TopicPartition, KafkaConsumerGroupOffsetsAndLag> offsets;

    public KafkaConsumerGroupDescription(ConsumerGroupDescription consumerGroupDescription, Map<TopicPartition, KafkaConsumerGroupOffsetsAndLag> offsets) {
        groupId = consumerGroupDescription.groupId();
        coordinator = consumerGroupDescription.coordinator().idString();
        isSimpleConsumerGroup = consumerGroupDescription.isSimpleConsumerGroup();
        partitionAssignor = consumerGroupDescription.partitionAssignor();
        state = consumerGroupDescription.state().toString();
        members = consumerGroupDescription.members();
        this.offsets = offsets;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getCoordinator() {
        return coordinator;
    }

    public boolean isSimpleConsumerGroup() {
        return isSimpleConsumerGroup;
    }

    public String getPartitionAssignor() {
        return partitionAssignor;
    }

    public String getState() {
        return state;
    }

    public Collection<MemberDescription> getMembers() {
        return members;
    }

    public Map<TopicPartition, KafkaConsumerGroupOffsetsAndLag> getOffsets() {
        return offsets;
    }
}
