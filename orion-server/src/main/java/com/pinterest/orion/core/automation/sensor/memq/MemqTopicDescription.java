package com.pinterest.orion.core.automation.sensor.memq;

import java.util.List;

import com.pinterest.orion.core.kafka.KafkaTopicDescription;

public class MemqTopicDescription {
  private TopicConfig config;
  private List<String> writeAssignments;
  private KafkaTopicDescription readAssignments;

  public List<String> getWriteAssignments() {
    return writeAssignments;
  }

  public void setWriteAssignments(List<String> writeAssignments) {
    this.writeAssignments = writeAssignments;
  }

  public KafkaTopicDescription getReadAssignments() {
    return readAssignments;
  }

  public void setReadAssignments(KafkaTopicDescription readAssignments) {
    this.readAssignments = readAssignments;
  }

  public TopicConfig getConfig() {
    return config;
  }

  public void setConfig(TopicConfig config) {
    this.config = config;
  }
}