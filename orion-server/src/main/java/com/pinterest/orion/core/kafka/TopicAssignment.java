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
package com.pinterest.orion.core.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(value={ "consumers" })
public class TopicAssignment {
  private String topicName;
  private boolean delete = false;
  private int partitions = -1;  // deprecating this field
  private String brokerset;
  private int replicationFactor = 3; // default
  private int stride;
  private String project;
  private String description;
  private String jira;
  private Map<String, String> config;
  private long dailyTraffic;

  /**
   * @return the topicName
   */
  public String getTopicName() {
    return topicName;
  }

  /**
   * @param topicName the topicName to set
   */
  public void setTopicName(String topicName) {
    this.topicName = topicName;
  }

  /**
   * @return if the topic should be absent
   */
  public boolean isDelete() {
    return delete;
  }

  /**
   * @param delete if the topic should be absent
   */
  public void setDelete(boolean delete) {
    this.delete = delete;
  }

  /**
   * @return the partitions
   */
  public int getPartitions() {
    return partitions;
  }

  /**
   * @param partitions the partitions to set
   */
  public void setPartitions(int partitions) {
    this.partitions = partitions;
  }

  /**
   * @return the brokerset
   */
  public String getBrokerset() {
    return brokerset;
  }

  /**
   * @param brokerset the brokerset to set
   */
  public void setBrokerset(String brokerset) {
    this.brokerset = brokerset;
  }

  /**
   * @return the replicationFactor
   */
  public int getReplicationFactor() { return replicationFactor; }

  /**
   * @param replicationFactor the replicationFactor to set
   */
  public void setReplicationFactor(int replicationFactor) { this.replicationFactor = replicationFactor; }

  public int getStride() {
    return stride;
  }

  public void setStride(int stride) {
    this.stride = stride;
  }

  /**
   * @return the project
   */
  public String getProject() {
    return project;
  }

  /**
   * @param project the project to set
   */
  public void setProject(String project) {
    this.project = project;
  }

  /**
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * @param description the description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  public String getJira() {
    return jira;
  }

  public void setJira(String jira) {
    this.jira = jira;
  }

  /**
   * @return the configs
   */
  public Map<String, String> getConfig() {
    if (config == null) {
      return Collections.EMPTY_MAP;
    } else {
      return config;
    }
  }

  /**
   * @param config the configs to set
   */
  public void setConfig(Map<String, String> config) {
    this.config = config;
  }

  /**
   * @return the daily traffic
   */
  public long getDailyTraffic() {
    return dailyTraffic;
  }

  /**
   * @param dailyTraffic the dailyTraffic to set
   */
  public void setDailyTraffic(long dailyTraffic) {
    this.dailyTraffic = dailyTraffic;
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == this){
      return true;
    }

    if (obj instanceof TopicAssignment) {
      return ((TopicAssignment) obj).getTopicName().equals(this.topicName);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.topicName.hashCode();
  }
}