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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;

import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.kafka.KafkaCluster;
import com.pinterest.orion.utils.NetworkUtils;

public class KafkaBrokerSensor extends KafkaSensor {

  public static final String CONF_SERVERSET_PATH_KEY = "serversetPath";
  public static final String ATTR_BOOTSTRAP_SERVERS_KEY = "bootstrapBrokers";
  public static final String ATTR_CONTROLLER_ID_KEY = "controllerId";
  public static final String ATTR_BROKERS_KEY = "brokers";

  @Override
  public String getName() {
    return "KafkaBrokerSensor";
  }

  @Override
  public void sense(KafkaCluster cluster) throws Exception {
    AdminClient adminClient = cluster.getAdminClient();
    if (adminClient == null) {
      adminClient = initializeAdminClient(cluster);
    }
    DescribeClusterOptions describeClusterOptions = new DescribeClusterOptions();
    if (cluster.containsKafkaAdminClientClusterRequestTimeoutMilliseconds()) {
      describeClusterOptions.timeoutMs(cluster.getKafkaAdminClientClusterRequestTimeoutMilliseconds());
    }
    DescribeClusterResult clusterResult = adminClient.describeCluster(describeClusterOptions);
    Collection<org.apache.kafka.common.Node> brokersList = clusterResult.nodes().get();
    brokersList.stream().forEach(n -> {
      NodeInfo info = new NodeInfo();
      info.setClusterId(cluster.getClusterId());
      info.setHostname(n.host());
      info.setIp(NetworkUtils.getIPFromHostnameIfAvailable(n.host()));
      info.setServicePort(n.port());
      info.setNodeId(n.idString());
      info.setRack(n.rack());
      info.setEnvironment(new HashMap<>());
      info.setAgentSettings(new HashMap<>());
      info.setServiceInfo(new HashMap<>());
      cluster.addNodeWithoutAgent(info);
    });

    // also record duplicate info as an attribute so we can compare agent reported
    // info Vs. cluster reported info
    Map<Integer, Node> collect = brokersList.stream()
        .collect(Collectors.toMap(Node::id, n -> n));
    setHiddenAttribute(cluster, ATTR_BROKERS_KEY, collect);

    KafkaFuture<org.apache.kafka.common.Node> controllerFuture = clusterResult.controller();
    String controllerId = controllerFuture.get().idString();
    setAttribute(cluster, ATTR_CONTROLLER_ID_KEY, controllerId);

    logger.info(() -> "Updated broker info");
  }

  private AdminClient initializeAdminClient(KafkaCluster cluster) throws PluginConfigurationException {
    AdminClient adminClient;
    if (cluster.getAttribute(ATTR_BOOTSTRAP_SERVERS_KEY) == null){
      setBootstrapServers(cluster);
    }

    String currentBootstrapServers = cluster.getAttribute(ATTR_BOOTSTRAP_SERVERS_KEY).getValue();
    Properties props = new Properties();
    props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, currentBootstrapServers);
    adminClient = AdminClient.create(props);
    cluster.setAdminClient(adminClient);
    return adminClient;
  }

  public static String getBrokersFromServerSet(String serversetPath) throws IOException {
    List<String> lines = Files.readAllLines(new File(serversetPath).toPath());
    return String.join(",", lines);
  }

  public void setBootstrapServers(KafkaCluster cluster) throws PluginConfigurationException {
    Map<String, Object> config = cluster.getAttribute("conf").getValue();
    String serversetPath = (String) config.get(CONF_SERVERSET_PATH_KEY);
    if (serversetPath == null) {
      throw new PluginConfigurationException(
          "Missing config " + CONF_SERVERSET_PATH_KEY + " for cluster " + cluster.getClusterId());
    }
    try {
      setAttribute(cluster, ATTR_BOOTSTRAP_SERVERS_KEY, getBrokersFromServerSet(serversetPath));
    } catch (IOException e) {
      throw new PluginConfigurationException(("Failed to load serversets for cluster "
          + cluster.getClusterId() + " from path " + serversetPath));
    }
  }
}
