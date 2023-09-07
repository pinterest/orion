package com.pinterest.orion.core.automation.sensor.memq;

import java.util.List;
import java.util.Properties;

import com.pinterest.orion.core.kafka.KafkaTopicDescription;

public class MemqTopicDescription {
  private TopicConfig config;
  private List<String> writeAssignments;
  private Properties usefulLinks;
  private KafkaTopicDescription readAssignments;

  public List<String> getWriteAssignments() {
    return writeAssignments;
  }

  public void setWriteAssignments(List<String> writeAssignments) {
    this.writeAssignments = writeAssignments;
  }

  public Properties getUsefulLinks() {
    return usefulLinks;
  }

  public void setUsefulLinks(Properties usefulLinks) {
    this.usefulLinks = usefulLinks;
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