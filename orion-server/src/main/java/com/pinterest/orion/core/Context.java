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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.w3c.dom.Attr;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Contexts are key-value objects that keeps attributes for plugins to access.
 */
public abstract class Context {
  private Map<String, Attribute> attributes = new ConcurrentHashMap<>();

  public Attribute getAttribute(String key) {
    return attributes.get(key);
  }

  public void setHiddenAttribute(String key, Object value, Set<String> sensorKeys) {
    attributes.put(key, new Attribute(sensorKeys, value, System.currentTimeMillis(), true));
  }

  public void setHiddenAttribute(String key, Object value, String sensorKey) {
    attributes.put(key, new Attribute(Collections.singleton(sensorKey), value, System.currentTimeMillis(), true));
  }

  public void setHiddenAttribute(String key, Object value){
    attributes.put(key, new Attribute(null, value, System.currentTimeMillis(), true));
  }

  public void setHiddenAttributeInternal(String key, Attribute attribute) {
    attributes.put(key, attribute);
  }

  public void setAttribute(String key, Object value, Set<String> sensorKeys) {
    attributes.put(key, new Attribute(sensorKeys, value, System.currentTimeMillis()));
  }

  public void setAttribute(String key, Object value, String sensorKey) {
    attributes.put(key, new Attribute(Collections.singleton(sensorKey), value, System.currentTimeMillis()));
  }
  
  public void setAttribute(String key, Object value){
    attributes.put(key, new Attribute(null, value, System.currentTimeMillis()));
  }

  public void setAttributeInternal(String key, Attribute attribute) {
    attributes.put(key, attribute);
  }

  public boolean containsAttribute(String key) {
    return attributes.containsKey(key);
  }

  @JsonIgnore
  public Map<String, Attribute> getAttributes() {
    return attributes;
  }

  @JsonGetter("attributes")
  public Map<String, Attribute> getExposedAttributes() {
    return attributes.entrySet().parallelStream().filter(e -> !e.getValue().isHidden()).collect(
        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @JsonSetter("attributes")
  public void setAttributes(Map<String, Attribute> attributes) {
    this.attributes = attributes;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof Context)) {
      return false;
    }

    return ((Context) obj).attributes.equals(this.attributes);
  }
}
