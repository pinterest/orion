package com.pinterest.orion.core.automation.sensor.memq;

import java.util.List;
import java.util.Properties;

import com.pinterest.orion.core.kafka.KafkaTopicDescription;

public class MemqTopicDescription {

  private TopicConfig config;

  private List<String> writeAssignments;

  /*
  usefulLinks saves links shown in Links section of topic page in memq orion UI.
  The links can be resource management tool website or project website.
  Key of Properties is the name of the page; Value of Properties is the hyperlink.
  The map can be updated in wrapper class.
   */
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
