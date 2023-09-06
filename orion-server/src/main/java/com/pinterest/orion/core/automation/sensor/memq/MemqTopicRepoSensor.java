package com.pinterest.orion.core.automation.sensor.memq;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.pinterest.orion.core.Attribute;
import com.pinterest.orion.core.memq.MemqCluster;

public class MemqTopicRepoSensor extends MemqSensor {

  private static final Logger logger = Logger
      .getLogger(MemqTopicRepoSensor.class.getCanonicalName());
  public static final String TARGET_TOPIC_CONFIGS = "target_topic_configs";

  @Override
  public String getName() {
    return "Topic Repo Sensor";
  }

  @Override
  public void sense(MemqCluster cluster) throws Exception {
    if (!cluster.containsAttribute(MemqCluster.CLUSTER_INFO_DIR)) {
      throw new Exception("Missing cluster info directory");
    }
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
  }
}
