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
