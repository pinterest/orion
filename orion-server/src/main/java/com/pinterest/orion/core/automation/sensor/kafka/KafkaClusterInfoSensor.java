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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.kafka.Brokerset;
import com.pinterest.orion.core.kafka.ConsumerInfo;
import com.pinterest.orion.core.kafka.KafkaCluster;
import com.pinterest.orion.core.kafka.TopicAssignment;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.pinterest.orion.core.kafka.Brokerset.BrokersetRangeMap;

public class KafkaClusterInfoSensor extends KafkaSensor {

  public static final String ATTR_CONSUMER_INFO_KEY = "consumers";
  public static final String ATTR_BROKERSET_KEY = "brokerset";
  public static final String CONF_CLUSTER_INFO_DIR_KEY = "clusterInfoDir";
  public static final String ATTR_TOPIC_ASSIGNMENTS_KEY = "topicAssignment";

  @Override
  public String getName() {
    return "BrokersetAssignmentSensor";
  }

  @Override
  public void sense(KafkaCluster cluster) throws Exception {
    Map<String, Object> conf = cluster.getAttribute(Cluster.ATTR_CONF_KEY).getValue();
    if (conf != null && conf.containsKey(CONF_CLUSTER_INFO_DIR_KEY)) {
      File infoDir = new File(conf.get(CONF_CLUSTER_INFO_DIR_KEY).toString());
      if (infoDir.exists() && infoDir.isDirectory()){
        loadAndSetBrokerset(infoDir, cluster);
        loadAndSetTopicAssignments(infoDir, cluster);
        loadAndSetConsumerInfo(infoDir, cluster);
        logger.info("Updated cluster info");
      } else {
        logger.info("clusterInfoDir " + infoDir.getPath() + " of " + cluster.getClusterId() + " doesn't exist, skipped update");
      }
    }
  }

  protected void loadAndSetTopicAssignments(File infoDir,
                                            KafkaCluster cluster) {
    File file = new File(infoDir, "topicassignments.json");
    try {
      List<TopicAssignment> assignments = loadTopicAssignmentsFromFile(file);
      setAttribute(cluster, ATTR_TOPIC_ASSIGNMENTS_KEY, assignments);
    } catch (IOException e) {
      logger.log(Level.SEVERE,
          "Failed to load topic assignment file for cluster:" + cluster.getClusterId());
    }
  }

  public static List<TopicAssignment> loadTopicAssignmentsFromFile(File file) throws IOException {
    return jsonfileToObject(file, new TypeReference<List<TopicAssignment>>(){});
  }

  protected void loadAndSetBrokerset(File infoDir, KafkaCluster cluster) {
    Map<String, Brokerset> brokersetMap = new ConcurrentHashMap<>();
    File file = new File(infoDir, "brokerset.json");
    File overrideFile = new File(infoDir, "brokersetOverrides.json");
    try {
      List<Brokerset> brokersets = loadBrokersetsFromFile(file, overrideFile);
      for (Brokerset brokerset : brokersets) {
        brokersetMap.put(brokerset.getBrokersetAlias(), brokerset);
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE,
              "Failed to load brokerset for cluster:" + cluster.getClusterId(), e);
    }
    setAttribute(cluster, ATTR_BROKERSET_KEY, brokersetMap);
  }

  public static List<Brokerset> loadBrokersetsFromFile(File file) throws IOException {
    return loadBrokersetsFromFile(file, null);
  }

  public static List<Brokerset> loadBrokersetsFromFile(File file, @Nullable File overrides) throws IOException{
    List<Brokerset> unsanitized;
    unsanitized = jsonfileToObject(file, new TypeReference<List<Brokerset>>() {});
    if (overrides == null || !overrides.exists() || overrides.length() == 0) {
      return unsanitized;
    }
    List<Brokerset> sanitized;
    BrokersetRangeMap map = jsonfileToObject(overrides, new TypeReference<BrokersetRangeMap>(){});
    sanitized = unsanitized.stream().
            map(brokerset -> brokerset.applyBrokerOverrides(map)).
            collect(Collectors.toList());

    return sanitized;
  }

  protected void loadAndSetConsumerInfo(File infoDir, KafkaCluster cluster) {
    Map<String, ConsumerInfo> consumerInfoMap = new ConcurrentHashMap<>();
    File file = new File(infoDir, "consumers.json");
    if(!file.exists()) {
      logger.info("Consumer info doesn't exist for " + cluster.getClusterId());
      return;
    }
    try {
      List<ConsumerInfo> consumers = loadConsumerInfoFromFile(file);
      for (ConsumerInfo consumer : consumers) {
        consumerInfoMap.put(consumer.getName(), consumer);
      }
    } catch (JsonSyntaxException | IOException e) {
      logger.log(Level.SEVERE,
          "Failed to load consumer file for cluster:" + cluster.getClusterId(), e);
    }
    setAttribute(cluster, ATTR_CONSUMER_INFO_KEY, consumerInfoMap);
  }

  public static List<ConsumerInfo> loadConsumerInfoFromFile(File file) throws IOException {
    return jsonfileToObject(file, new TypeReference<List<ConsumerInfo>>() {});
  }

  public static <T> T jsonfileToObject(File jsonFile, TypeReference<T> typeReference ) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.readValue(jsonFile, typeReference);
    } catch (Exception e){
      throw new IOException("Problem found when reading " + jsonFile, e);
    }
  }
}
