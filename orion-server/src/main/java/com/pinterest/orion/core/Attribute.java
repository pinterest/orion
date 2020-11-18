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
package com.pinterest.orion.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Set;

@JsonIgnoreProperties(value = {"publishingSensors"})
public class Attribute {

  private Set<String> publishingSensors;
  private Object value;
  private long updateTimestamp;

  @JsonIgnore
  private boolean hidden = false;

  public Attribute() {

  }

  public Attribute(Set<String> publishingSensors, Object value, long updateTimestamp) {
    this.publishingSensors = publishingSensors;
    this.value = value;
    this.updateTimestamp = updateTimestamp;
  }

  public Attribute(Set<String> publishingSensors, Object value, long updateTimestamp, boolean hidden) {
    this.publishingSensors = publishingSensors;
    this.value = value;
    this.updateTimestamp = updateTimestamp;
    this.hidden = hidden;
  }
  /**
   * @return the publishingSensor
   */
  public Set<String> getPublishingSensors() {
    return publishingSensors;
  }
  /**
   * @param publishingSensors the publishingSensor to set
   */
  public void setPublishingSensors(Set<String> publishingSensors) {
    this.publishingSensors = publishingSensors;
  }
  /**
   * @return the value
   */
  @SuppressWarnings("unchecked")
  public <T> T getValue() {
    return (T) value;
  }
  /**
   * @param value the value to set
   */
  public void setValue(Object value) {
    this.value = value;
  }
  /**
   * @return the updateTimestamp
   */
  public long getUpdateTimestamp() {
    return updateTimestamp;
  }
  /**
   * @param updateTimestamp the updateTimestamp to set
   */
  public void setUpdateTimestamp(long updateTimestamp) {
    this.updateTimestamp = updateTimestamp;
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj instanceof Attribute) {
      return this.value.equals(((Attribute) obj).value);
    }
    return false;
  }

  @Override
  public String toString() {
    return "Attribute( value=" + value + " publishingSensors=" + publishingSensors + " timestamp=" + updateTimestamp +")";
  }

  @JsonIgnore
  public boolean isHidden() {
    return hidden;
  }
}
