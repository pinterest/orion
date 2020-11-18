package com.pinterest.orion.core.actions.schema;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class Value {
  @JsonProperty
  private String name;
  @JsonProperty
  private String label;
  @JsonProperty
  private boolean required;

  public Value(String name, String label, boolean required) {
    this.name = name;
    this.label = label;
    this.required = required;
  }

  @JsonProperty("type")
  public abstract String getType();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public boolean isRequired() {
    return required;
  }

  public void setRequired(boolean required) {
    this.required = required;
  }
}
