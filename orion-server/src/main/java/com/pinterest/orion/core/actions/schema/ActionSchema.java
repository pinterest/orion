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
package com.pinterest.orion.core.actions.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ActionSchema {
  @JsonProperty
  private String actionKey;
  @JsonProperty
  private String displayName;
  @JsonProperty
  private String label;
  @JsonProperty
  private String icon;
  @JsonIgnore
  private AttributeSchema attributes;

  public ActionSchema() {
  }

  public ActionSchema(String actionKey, String displayName, String label, String icon,
                      AttributeSchema attributes) {
    this.actionKey = actionKey;
    this.displayName = displayName;
    this.label = label;
    this.icon = icon;
    this.attributes = attributes;
  }

  public String getActionKey() {
    return actionKey;
  }

  public void setActionKey(String actionKey) {
    this.actionKey = actionKey;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  @JsonProperty("attributes")
  public List<Value> getAttributes() {
    if (attributes == null) {
      return null;
    }
    return attributes.getAttributes();
  }

  public void setAttributes(AttributeSchema attributes) {
    this.attributes = attributes;
  }
}
