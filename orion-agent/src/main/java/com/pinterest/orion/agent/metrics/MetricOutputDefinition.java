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
package com.pinterest.orion.agent.metrics;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

public class MetricOutputDefinition implements Serializable {

  private static final long serialVersionUID = 1L;
  private String name;
  private Map<String, String> tags;
  private Set<String> transmission;

  public MetricOutputDefinition() {
  }

  public MetricOutputDefinition(String name, Map<String, String> tags, Set<String> transmission) {
    this.name = name;
    this.tags = tags;
    this.transmission = transmission;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Map<String, String> getTags() {
    return tags;
  }

  public void setTags(Map<String, String> tags) {
    this.tags = tags;
  }

  public Set<String> getTransmission() {
    return transmission;
  }

  public void setTransmission(Set<String> transmission) {
    this.transmission = transmission;
  }

  public MetricOutputDefinition substituteEntityValues(Map<String, String> entityValueMap) {
    if (entityValueMap.isEmpty()) {
      return this;
    }
    Map<String, String> substitutedAttributes = new HashMap<>();
    for (Entry<String, String> entry2 : tags.entrySet()) {
      String value = entry2.getValue();
      for (Entry<String, String> entry : entityValueMap.entrySet()) {
        if (substitutedAttributes.containsKey(entry2.getKey())) {
          value = substitutedAttributes.get(entry2.getKey());
        }
        String replace = value.replace("${" + entry.getKey() + "}", entry.getValue());
        substitutedAttributes.put(entry2.getKey(), replace);
      }
      if (substitutedAttributes.get(entry2.getKey()).contains("${")) {
        throw new RuntimeException("Unable to perform substitution for attributes " + tags + " and values " + entityValueMap);
      }
    }
    return new MetricOutputDefinition(new String(name), substitutedAttributes, transmission);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MetricOutputDefinition that = (MetricOutputDefinition) o;
    return name.equals(that.name) &&
            tags.equals(that.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, tags);
  }

  @Override
  public String toString() {
    return "MetricOutputDefinition [name=" + name + ", tags=" + tags + "]";
  }

}
