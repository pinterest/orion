package com.pinterest.orion.core.actions.schema;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class AttributeSchema {
  @JsonProperty
  private List<Value> attributes = new ArrayList<>();

  public AttributeSchema() {
  }

  public AttributeSchema addValue(Value value) {
    if (value != null) {
      attributes.add(value);
    }
    return this;
  }

  public AttributeSchema addSchema(AttributeSchema schema) {
    if (schema != null) {
      attributes.addAll(schema.attributes);
    }
    return this;
  }

  public List<Value> getAttributes() {
    return attributes;
  }
}
