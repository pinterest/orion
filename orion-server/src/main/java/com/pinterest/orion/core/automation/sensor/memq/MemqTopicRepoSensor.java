package com.pinterest.orion.core.automation.sensor.memq;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.pinterest.orion.core.Attribute;
import com.pinterest.orion.core.automation.sensor.kafka.KafkaClusterInfoSensor;
import com.pinterest.orion.core.kafka.Brokerset;
import com.pinterest.orion.core.memq.MemqCluster;

public class MemqTopicRepoSensor extends MemqSensor {

  public static final String TARGET_TOPIC_CONFIGS = "target_topic_configs";

  @Override
  public String getName() {
    return "Topic Repo Sensor";
  }

  @Override
  public void sense(MemqCluster cluster) throws Exception {
    Attribute attribute = cluster.getAttribute(MemqCluster.CLUSTER_INFO_DIR);
    String clusterInfoDir = attribute.getValue();

    Map<String, TopicConfig> targetConfig = new HashMap<>();
    Gson gson = new Gson();
    File infoDirFile = new File(clusterInfoDir);
    for (File configFile : infoDirFile.listFiles()) {
      if (!configFile.getName().endsWith(".json")) {
        continue;
      }
      TopicConfig config = gson.fromJson(new String(Files.readAllBytes(configFile.toPath())),
          TopicConfig.class);
      targetConfig.put(config.getTopic(), config);
    }
    setAttribute(cluster, TARGET_TOPIC_CONFIGS, targetConfig);

    attribute = cluster.getAttribute(MemqCluster.NOTIFICATION_CLUSTER_CONFIG);
    Map<String, String> value = attribute.getValue();

    Map<String, Map<String, Brokerset>> notificationClusterBrokersetMap = new HashMap<>();
    for (Entry<String, String> entry : value.entrySet()) {
      File file = new File(entry.getValue() + "/brokerset.json");
      Brokerset[] brokersets = gson.fromJson(new String(Files.readAllBytes(file.toPath())),
          Brokerset[].class);

      Map<String, Brokerset> brokersetMap = new HashMap<>();
      for (Brokerset brokerset : brokersets) {
        brokersetMap.put(brokerset.getBrokersetAlias(), brokerset);
      }
      notificationClusterBrokersetMap.put(entry.getKey(), brokersetMap);
    }
    setAttribute(cluster, KafkaClusterInfoSensor.ATTR_BROKERSET_KEY,
        notificationClusterBrokersetMap);
  }

}
