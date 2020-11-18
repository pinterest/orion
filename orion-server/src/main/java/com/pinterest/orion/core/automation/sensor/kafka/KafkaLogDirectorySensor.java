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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeLogDirsOptions;
import org.apache.kafka.clients.admin.DescribeLogDirsResult;
import org.apache.kafka.common.requests.DescribeLogDirsResponse.LogDirInfo;

import com.pinterest.orion.core.kafka.KafkaCluster;

public class KafkaLogDirectorySensor extends KafkaSensor {

  public static final String ATTR_BROKER_LOG_DIRS_KEY = "brokerLogDirs";

  @Override
  public String getName() {
    return "KafkaLogDirSensor";
  }

  @Override
  public void sense(KafkaCluster cluster) throws Exception {
    AdminClient adminClient = cluster.getAdminClient();
    if (adminClient == null) {
      return;
    }

    if (cluster.containsAttribute(KafkaBrokerSensor.ATTR_BROKERS_KEY)) {
      Map<Integer, org.apache.kafka.common.Node> attribute = cluster
          .getAttribute(KafkaBrokerSensor.ATTR_BROKERS_KEY).getValue();
      List<Integer> brokers = new ArrayList<>(attribute.keySet());
      DescribeLogDirsResult describeLogDirs = adminClient.describeLogDirs(brokers);
      Map<Integer, Map<String, LogDirInfo>> map = describeLogDirs.all().get();
      setHiddenAttribute(cluster, ATTR_BROKER_LOG_DIRS_KEY, map);
      logger.info("Updated log directory");
    }
  }

}
