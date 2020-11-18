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
package com.pinterest.orion.core.metrics;

import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;

import com.google.gson.Gson;
import com.pinterest.orion.common.AgentHeartbeat;

public class KafkaMetricsStore implements MetricsStore {

  private static final Gson gson = new Gson();
  private KafkaProducer<byte[], byte[]> producer;
  private String topic;

  @Override
  public void init(Map<String, Object> config) throws Exception {
    topic = config.getOrDefault("metrics.topic", "lancer_agent_heartbeats").toString();
    config.put(ProducerConfig.ACKS_CONFIG, "0");
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
    producer = new KafkaProducer<>(config);
  }

  @Override
  public void publishMetrics(AgentHeartbeat heartbeat) throws Exception {
    producer.send(new ProducerRecord<byte[], byte[]>(topic, gson.toJson(heartbeat).getBytes()));
  }

  @Override
  public List<SeriesOutput> getMetrics(String seriesPattern,
                                       String valueFieldPattern,
                                       Map<String, String> tags,
                                       long startTs,
                                       long endTs) throws Exception {
    // unsupported
    return null;
  }

}
