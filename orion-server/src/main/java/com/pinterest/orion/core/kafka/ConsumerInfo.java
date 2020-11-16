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

import java.util.List;

@JsonIgnoreProperties(value={ "notes" })
public class ConsumerInfo {
  public enum Tier {
    P0,
    P1,
    P2,
    P3
  }

  private String name;
  private String project;
  private String clientVersion;
  private String clientTechnology;
  private String clientLibrary;
  private String description;
  private String environment;
  private List<String> topics;
  private Tier tier = Tier.P3;
  /**
   * @return the name
   */
  public String getName() {
    return name;
  }
  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
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
   * @return the clientVersion
   */
  public String getClientVersion() {
    return clientVersion;
  }
  /**
   * @param clientVersion the clientVersion to set
   */
  public void setClientVersion(String clientVersion) {
    this.clientVersion = clientVersion;
  }
  /**
   * @return the clientTechnology
   */
  public String getClientTechnology() {
    return clientTechnology;
  }
  /**
   * @param clientTechnology the clientTechnology to set
   */
  public void setClientTechnology(String clientTechnology) {
    this.clientTechnology = clientTechnology;
  }

  public String getClientLibrary() {
    return clientLibrary;
  }

  public void setClientLibrary(String clientLibrary) {
    this.clientLibrary = clientLibrary;
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
  /**
   * @return the environment
   */
  public String getEnvironment() {
    return environment;
  }
  /**
   * @param environment the environment to set
   */
  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  public Tier getTier() {
    return tier;
  }

  public void setTier(Tier tier) {
    this.tier = tier;
  }

  public List<String> getTopics() {
    return topics;
  }

  public void setTopics(List<String> topics) {
    this.topics = topics;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return name.equals(((ConsumerInfo)obj).getName());
  }
}
