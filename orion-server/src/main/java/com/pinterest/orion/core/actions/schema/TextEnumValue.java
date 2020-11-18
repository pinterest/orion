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
