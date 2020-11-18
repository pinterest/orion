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

import static org.junit.Assert.*;

import com.google.common.collect.Sets;
import com.pinterest.orion.core.automation.operator.kafka.BrokerHealingOperator;
import com.pinterest.orion.core.kafka.KafkaTopicDescription;

import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BrokerHealingOperatorTest {

  private BrokerHealingOperator optr;
  private static Node[] nodes = new Node[]{
      new org.apache.kafka.common.Node(100, "100", 9092),
      new org.apache.kafka.common.Node(101, "101", 9092),
      new org.apache.kafka.common.Node(102, "102", 9092),
      new org.apache.kafka.common.Node(103, "103", 9092),
      };

  @Before
  public void init() {
    optr = new BrokerHealingOperator();
  }

  @Test
  public void testGetUnhealthyBrokersAnyThreshold() throws Exception {
    optr.initialize(Collections.singletonMap("unhealthyBrokerURPRatioThreshold", "0"));
    Map<String, KafkaTopicDescription> topicMap = new HashMap<>();
    topicMap.put("topic", new KafkaTopicDescription(
        new TopicDescription("topic", false,
        Arrays.asList(
            new TopicPartitionInfo(0, nodes[0], Arrays.asList(nodes[0], nodes[1]), Arrays.asList(nodes[0], nodes[1])),
            new TopicPartitionInfo(1, nodes[2], Arrays.asList(nodes[2], nodes[3]), Arrays.asList(nodes[2], nodes[3]))
        )))
    );
    assertTrue(optr.getUnhealthyKafkaBrokers(topicMap).isEmpty());

    topicMap.put("topic", new KafkaTopicDescription(
        new TopicDescription("topic", false,
            Arrays.asList(
                new TopicPartitionInfo(0, nodes[0], Arrays.asList(nodes[0], nodes[1], nodes[2]), Arrays.asList(nodes[0], nodes[1])),
                new TopicPartitionInfo(1, nodes[1], Arrays.asList(nodes[1], nodes[2], nodes[3]), Arrays.asList(nodes[1], nodes[2]))
            )))
    );
    Set<String> offlineBrokers = optr.getUnhealthyKafkaBrokers(topicMap);
    assertEquals(2, offlineBrokers.size());
    assertEquals(Sets.newHashSet(nodes[2].idString(), nodes[3].idString()), offlineBrokers);
  }

  @Test
  public void testGetUnhealthyBrokersAllThreshold() throws Exception {
    optr.initialize(Collections.singletonMap("unhealthyBrokerURPRatioThreshold", "1"));
    Map<String, KafkaTopicDescription> topicMap = new HashMap<>();
    topicMap.put("topic", new KafkaTopicDescription(
        new TopicDescription("topic", false,
            Arrays.asList(
                new TopicPartitionInfo(0, nodes[0], Arrays.asList(nodes[0], nodes[1]), Arrays.asList(nodes[0], nodes[1])),
                new TopicPartitionInfo(1, nodes[2], Arrays.asList(nodes[2], nodes[3]), Arrays.asList(nodes[2], nodes[3]))
            )))
    );
    assertTrue(optr.getUnhealthyKafkaBrokers(topicMap).isEmpty());

    topicMap.put("topic", new KafkaTopicDescription(
        new TopicDescription("topic", false,
            Arrays.asList(
                new TopicPartitionInfo(0, nodes[0], Arrays.asList(nodes[0], nodes[1], nodes[2]), Arrays.asList(nodes[0], nodes[1])),
                new TopicPartitionInfo(1, nodes[1], Arrays.asList(nodes[1], nodes[2], nodes[3]), Arrays.asList(nodes[1], nodes[2]))
            )))
    );
    Set<String> offlineBrokers = optr.getUnhealthyKafkaBrokers(topicMap);
    assertEquals(1, offlineBrokers.size());
    assertEquals(Sets.newHashSet(nodes[3].idString()), offlineBrokers);
  }

  @Test
  public void testGetUnhealthyBrokersHalfThreshold() throws Exception {
    optr.initialize(Collections.singletonMap("unhealthyBrokerURPRatioThreshold", "0.5"));
    Map<String, KafkaTopicDescription> topicMap = new HashMap<>();
    topicMap.put("topic", new KafkaTopicDescription(
        new TopicDescription("topic", false,
            Arrays.asList(
                new TopicPartitionInfo(0, nodes[0], Arrays.asList(nodes[0], nodes[1]), Arrays.asList(nodes[0], nodes[1])),
                new TopicPartitionInfo(1, nodes[2], Arrays.asList(nodes[2], nodes[3]), Arrays.asList(nodes[2], nodes[3]))
            )))
    );
    assertTrue(optr.getUnhealthyKafkaBrokers(topicMap).isEmpty());


    topicMap.put("topic", new KafkaTopicDescription(
        new TopicDescription("topic", false,
            Arrays.asList(
                new TopicPartitionInfo(0, nodes[0], Arrays.asList(nodes[0], nodes[1], nodes[2]), Arrays.asList(nodes[0], nodes[1])),
                new TopicPartitionInfo(1, nodes[1], Arrays.asList(nodes[1], nodes[2], nodes[3]), Arrays.asList(nodes[1], nodes[2]))
            )))
    );
    Set<String> offlineBrokers = optr.getUnhealthyKafkaBrokers(topicMap);
    assertEquals(2, offlineBrokers.size());
    assertEquals(Sets.newHashSet(nodes[2].idString(), nodes[3].idString()), offlineBrokers);

    topicMap.put("topic2", new KafkaTopicDescription(
        new TopicDescription("topic2", false,
            Arrays.asList(
                new TopicPartitionInfo(0, nodes[0], Arrays.asList(nodes[0], nodes[1], nodes[2]), Arrays.asList(nodes[0], nodes[1])),
                new TopicPartitionInfo(1, nodes[1], Arrays.asList(nodes[1], nodes[2], nodes[3]), Arrays.asList(nodes[1], nodes[2], nodes[3])),
                new TopicPartitionInfo(2, nodes[3], Arrays.asList(nodes[3], nodes[0], nodes[1]), Arrays.asList(nodes[3], nodes[0], nodes[1]))
            )))
    );

    // Threshold = 0.5
    // node     URP ratio     offline?
    // 100      0/3 = 0       no
    // 101      0/5 = 0       no
    // 102      2/4 = 0.5     yes
    // 103      1/3 = 0.33    no

    offlineBrokers = optr.getUnhealthyKafkaBrokers(topicMap);
    assertEquals(1, offlineBrokers.size());
    assertEquals(Sets.newHashSet(nodes[2].idString()), offlineBrokers);
  }
}