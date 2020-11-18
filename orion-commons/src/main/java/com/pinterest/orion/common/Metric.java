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
package com.pinterest.orion.common;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Metric implements Serializable {

  private static final long serialVersionUID = 1L;
  private String series;
  private Map<String, String> tags;
  private long timestamp;
  private List<Value> values;

  @JsonIgnore
  private Set<String> transmission;
  
  public Metric() {
  }
  
  public Metric(String series, Map<String, String> tags, long timestamp, List<Value> values, Set<String> transmission) {
    this.series = series;
    this.tags = tags;
    this.timestamp = timestamp;
    this.values = values;
    this.transmission = transmission;
  }
  
  /**
   * @return the series
   */
  public String getSeries() {
    return series;
  }
  /**
   * @param series the series to set
   */
  public void setSeries(String series) {
    this.series = series;
  }
  /**
   * @return the tags
   */
  public Map<String, String> getTags() {
    return tags;
  }
  /**
   * @param tags the tags to set
   */
  public void setTags(Map<String, String> tags) {
    this.tags = tags;
  }
  /**
   * @return the timestamp
   */
  public long getTimestamp() {
    return timestamp;
  }
  /**
   * @param timestamp the timestamp to set
   */
  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }
  /**
   * @return the values
   */
  public List<Value> getValues() {
    return values;
  }
  /**
   * @param values the values to set
   */
  public void setValues(List<Value> values) {
    this.values = values;
  }
  public void addToValues(Value value) {
    values.add(value);
  }

  @JsonIgnore
  public Set<String> getTransmission() {
    return transmission;
  }

  @JsonIgnore
  public void setTransmission(Set<String> transmission) {
    this.transmission = transmission;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Metric [series=" + series + ", tags=" + tags + ", timestamp=" + timestamp + ", values="
        + values + ", transmission=" + transmission + "]";
  }
  
  
}
