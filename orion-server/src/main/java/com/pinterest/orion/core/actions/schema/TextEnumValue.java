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
import java.util.Map;

public class TextEnumValue extends Value {
  @JsonProperty
  private List<Data> data = new ArrayList<>();

  public TextEnumValue(String name, String label, boolean required) {
    super(name, label, required);
  }

  @Override
  public String getType() {
    return "select";
  }

  public TextEnumValue addOption(String label, String value) {
    this.data.add(new Data(label, value));
    return this;
  }

  public TextEnumValue addOptions(Map<String, String> options) {
    for (Map.Entry<String, String> o : options.entrySet()) {
      this.data.add(new Data(o.getKey(), o.getValue()));
    }
    return this;
  }

  private static class Data {
    @JsonProperty
    private String label;
    @JsonProperty
    private String value;

    public Data(String label, String value) {
      this.label = label;
      this.value = value;
    }
  }

}
