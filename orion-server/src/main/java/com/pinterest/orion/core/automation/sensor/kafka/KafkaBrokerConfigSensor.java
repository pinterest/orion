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
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.config.ConfigResource;

import com.pinterest.orion.core.kafka.KafkaCluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class KafkaBrokerConfigSensor extends KafkaSensor {

  @Override
  public String getName() {
    return "KafkaBrokerConfigSensor";
  }

  @Override
  public void sense(KafkaCluster cluster) throws Exception {
    AdminClient adminClient = cluster.getAdminClient();
    if (adminClient == null) {
      return;
    }

    if (!cluster.containsAttribute(KafkaBrokerSensor.ATTR_BROKERS_KEY)) {
      return;
    }

    List<ConfigResource> brokerIds = new ArrayList<>();

    Map<Integer, Node> brokers = cluster.getAttribute(KafkaBrokerSensor.ATTR_BROKERS_KEY).getValue();
    for(Node broker : brokers.values()) {
      brokerIds.add(new ConfigResource(ConfigResource.Type.BROKER, broker.idString()));
    }

    // add broker configs
    DescribeConfigsResult describeConfigs = adminClient.describeConfigs(brokerIds);
    Map<ConfigResource, Config> map = describeConfigs.all().get();
    for (Map.Entry<ConfigResource, Config> entry : map.entrySet()) {
      com.pinterest.orion.core.Node node = cluster.getNodeMap().get(entry.getKey().name());
      Map<String, String> serviceInfo = node.getCurrentNodeInfo().getServiceInfo();
      if (serviceInfo != null) {
        for (ConfigEntry configEntry : entry.getValue().entries()) {
          serviceInfo.put(configEntry.name(), configEntry.value());
        }
      }
    }
    Optional<com.pinterest.orion.core.Node> node = cluster.getNodeMap().values().stream()
        .filter((n -> n.getCurrentNodeInfo() != null
            && n.getCurrentNodeInfo().getServiceInfo().containsKey(KafkaCluster.ZOOKEEPER_CONNECT)))
        .findAny();
    node.ifPresent(value -> setAttribute(cluster, KafkaCluster.ZOOKEEPER_CONNECT,
        value.getCurrentNodeInfo().getServiceInfo().get(KafkaCluster.ZOOKEEPER_CONNECT)));
    logger.info(() -> "Updated broker info");
  }
}
