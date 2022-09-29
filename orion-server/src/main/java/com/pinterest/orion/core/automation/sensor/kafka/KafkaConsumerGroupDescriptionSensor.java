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

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsOptions;

import com.pinterest.orion.core.kafka.KafkaCluster;
import org.apache.kafka.clients.admin.ListConsumerGroupsOptions;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KafkaConsumerGroupDescriptionSensor extends KafkaSensor {
  public static final String ATTR_CONSUMER_GROUP_IDS_KEY = "consumerGroupIds";
  public static final String ATTR_CONSUMER_GROUP_DESC_KEY = "consumerGroupDescriptions";

  @Override
  public void sense(KafkaCluster cluster) throws Exception {
    AdminClient adminClient = cluster.getAdminClient();
    if (adminClient == null) {
      return;
    }

    ListConsumerGroupsOptions listConsumerGroupsOptions = new ListConsumerGroupsOptions();
    DescribeConsumerGroupsOptions describeConsumerGroupsOptions = new DescribeConsumerGroupsOptions();
    if (cluster.containsKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds()) {
      listConsumerGroupsOptions.timeoutMs(cluster.getKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds());
      describeConsumerGroupsOptions.timeoutMs(cluster.getKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds());
    }
    long start = System.currentTimeMillis();
    Collection<ConsumerGroupListing> listConsumerGroupResult = adminClient.listConsumerGroups(
            listConsumerGroupsOptions).all().get();
    List<String> groupIds = listConsumerGroupResult.stream().map(ConsumerGroupListing::groupId).collect(
        Collectors.toList());
    Map<String, ConsumerGroupDescription> consumerGroupDescriptionMap = adminClient.describeConsumerGroups(
            groupIds, describeConsumerGroupsOptions).all().get();
    logger.info(String.format("Updated %d consumer groups of %s in %d ms", groupIds.size(), cluster.getClusterId(), System.currentTimeMillis() - start));
    setHiddenAttribute(cluster, ATTR_CONSUMER_GROUP_IDS_KEY, groupIds);
    setHiddenAttribute(cluster, ATTR_CONSUMER_GROUP_DESC_KEY, consumerGroupDescriptionMap);
  }

  @Override
  public String getName() {
    return "KafkaConsumerGroupDescriptionSensor";
  }
}
