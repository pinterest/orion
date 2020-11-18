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
package com.pinterest.orion.agent;

import java.io.FileInputStream;
import java.util.logging.Logger;

import com.pinterest.orion.agent.kafka.KafkaMirrorAgent;
import com.pinterest.orion.agent.metrics.TSDBMetricsCollectorService;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;
import org.yaml.snakeyaml.Yaml;

import com.pinterest.orion.agent.kafka.KafkaAgent;

/**
 *
 */
public class OrionAgent {

  public static MetricRegistry HEARTBEAT_METRICS = SharedMetricRegistries.getOrCreate("heartbeat");
  public static MetricRegistry TSDB_METRICS = SharedMetricRegistries.setDefault("tsdb");
  private static final Logger logger = Logger.getLogger(OrionAgent.class.getCanonicalName());

  public static void main(String[] args) throws Exception {
    // fix Java in-memory DNS caching
    java.security.Security.setProperty("networkaddress.cache.ttl" , "300");
    OrionAgentConfig config = new Yaml().loadAs(new FileInputStream(args[0]), OrionAgentConfig.class);
    BaseAgent agent;
    if (config.getAgentConfigs().containsKey(KafkaMirrorAgent.KAFKAMIRROR_CONFIG_DIRECTORY)) {
      agent = new KafkaMirrorAgent(config);
    } else {
      agent = new KafkaAgent(config);
    }
    logger.info("Starting Orion agent");
    Runtime.getRuntime().addShutdownHook(new Thread(()->{
      logger.info("Orion agent is being shutdown");
    }));
    agent.initialize();
    if (config.isEnableHeartbeat()) {
      HeartbeatService heartbeatService = new HeartbeatService(agent);
      heartbeatService.start();
    }
    if (config.getStatsConfigs().isEnabled()) {
      TSDBMetricsCollectorService metricsService = new TSDBMetricsCollectorService(agent);
      metricsService.start();
    }
  }

}