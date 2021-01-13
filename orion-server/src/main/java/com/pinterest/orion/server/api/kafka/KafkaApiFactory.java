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
package com.pinterest.orion.server.api.kafka;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.pinterest.orion.core.ClusterManager;
import com.pinterest.orion.core.kafka.KafkaTopicDescription;
import com.pinterest.orion.core.kafka.KafkaTopicPartitionInfo;
import com.pinterest.orion.server.api.CustomApiFactory;

import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Environment;

public class KafkaApiFactory extends CustomApiFactory {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = Logger.getLogger(KafkaApiFactory.class.getCanonicalName());

  public KafkaApiFactory() {
    super();
    addSerializer(KafkaTopicDescription.class, new KafkaTopicDescriptionSerializer());
    addSerializer(MemberDescription.class, new MemberDescriptionSerializer());
    addSerializer(OffsetAndMetadata.class, new OffsetAndMetadataSerializer());
    addSerializer(TopicPartition.class, new TopicPartitionSerializer());
  }

  @Override
  public void registerAPIs(Environment globalEnv,
                           JerseyEnvironment jerseyEnv,
                           ClusterManager clusterMgr) {
    jerseyEnv.register(new KafkaClusterApi(clusterMgr));
    globalEnv.getObjectMapper().registerModule(this);
  }

  public static class KafkaTopicDescriptionSerializer
      extends JsonSerializer<KafkaTopicDescription> {

    @Override
    public void serialize(KafkaTopicDescription value,
                          JsonGenerator gen,
                          SerializerProvider serializers) throws IOException {
      gen.writeStartObject();
      gen.writeStringField("topic", value.getName());
      gen.writeStringField("brokerset", value.getBrokersetAlias());
      gen.writeObjectField("configs", value.getTopicConfigs());
      gen.writeObjectField("overrideConfigs", value.getOverrideConfigs());
      gen.writeArrayFieldStart("partitions");
      for (KafkaTopicPartitionInfo topicPartitionInfo : value.getPartitions()) {
        gen.writeStartObject();
          gen.writeNumberField("leader",
              topicPartitionInfo.getLeader() != null ? topicPartitionInfo.getLeader().id() : -1);
        gen.writeNumberField("partition", topicPartitionInfo.getPartition());
        gen.writeObjectFieldStart("replicainfo");
        topicPartitionInfo.getReplicaInfo().entrySet().stream().forEach(v -> {
          try {
            gen.writeObjectFieldStart(v.getKey());
            gen.writeNumberField("size", v.getValue().size);
            gen.writeNumberField("offsetLag", v.getValue().offsetLag);
            gen.writeEndObject();
          } catch (IOException e) {
            logger.log(Level.SEVERE, "Serialization error with ISRs", e);
          }
        });
        gen.writeEndObject();
        gen.writeArrayFieldStart("isrs");
        topicPartitionInfo.getIsrs().stream().mapToInt(n -> n.id()).forEach(n -> {
          try {
            gen.writeNumber(n);
          } catch (IOException e) {
            logger.log(Level.SEVERE, "Serialization error with ISRs", e);
          }
        });
        gen.writeEndArray();
        gen.writeArrayFieldStart("replicas");
        topicPartitionInfo.getReplicas().stream().mapToInt(n -> n.id()).forEach(n -> {
          try {
            gen.writeNumber(n);
          } catch (IOException e) {
            logger.log(Level.SEVERE, "Serialization error with Replicas", e);
          }
        });
        gen.writeEndArray();
        gen.writeEndObject();
      }
      gen.writeEndArray();
      gen.writeEndObject();
    }

  }

  public static class TopicPartitionSerializer extends JsonSerializer<TopicPartition> {

    @Override
    public void serialize(TopicPartition value,
                          JsonGenerator gen,
                          SerializerProvider serializers) throws IOException {
      gen.writeStartObject();
      gen.writeStringField("topic", value.topic());
      gen.writeNumberField("partition", value.partition());
      gen.writeEndObject();
    }

  }

  public static class MemberDescriptionSerializer extends JsonSerializer<MemberDescription> {

    @Override
    public void serialize(MemberDescription value,
                          JsonGenerator gen,
                          SerializerProvider serializers) throws IOException {
      gen.writeStartObject();
      gen.writeStringField("clientId", value.clientId());
      gen.writeStringField("consumerId", value.consumerId());
      gen.writeStringField("host", value.host());
      gen.writeArrayFieldStart("assignment");
      for (TopicPartition topicPartition : value.assignment().topicPartitions()) {
        gen.writeStartObject();
        gen.writeStringField("topic", topicPartition.topic());
        gen.writeNumberField("partition", topicPartition.partition());
        gen.writeEndObject();
      }
      gen.writeEndArray();
      gen.writeEndObject();
    }
  }

  public static class OffsetAndMetadataSerializer extends JsonSerializer<OffsetAndMetadata> {

    @Override
    public void serialize(OffsetAndMetadata value,
                          JsonGenerator gen,
                          SerializerProvider serializers) throws IOException {
      gen.writeStartObject();
      gen.writeNumberField("offset", value.offset());
      if (!value.metadata().isEmpty())
        gen.writeStringField("metadata", value.metadata());
      if (value.leaderEpoch().isPresent())
        gen.writeNumberField("leaderEpoch", value.leaderEpoch().get());
      gen.writeEndObject();
    }
  }
}
