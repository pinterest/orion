package com.pinterest.orion.core.actions.schema;

public class TextValue extends Value {
  public TextValue(String name, String label, boolean required) {
    super(name, label, required);
  }

  @Override
  public String getType() {
    return "textField";
  }
}
