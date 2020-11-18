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
