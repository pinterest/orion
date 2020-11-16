package com.pinterest.orion.agent.metrics;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

public class MetricInputDefinition implements Serializable {

  public static final String METRIC_NAME = "metricName";
  public static final String ATTRIBUTE_NAME = "attributeName";

  private static final long serialVersionUID = 1L;

  private Map<String, String> inputDefinitionAttributes = new HashMap<>();
  
  public MetricInputDefinition() {
  }

  public MetricInputDefinition(Map<String, String> inputDefinitionAttributes) {
    super();
    this.inputDefinitionAttributes = inputDefinitionAttributes;
  }

  public MetricInputDefinition substituteEntityValues(Map<String, String> substitute) {
    if (substitute.isEmpty()) {
      return this;
    }
    Map<String, String> substitutedAttributes = new HashMap<>();
    for (Entry<String, String> attributeEntry : inputDefinitionAttributes.entrySet()) {
      String value = attributeEntry.getValue();
      for (Entry<String, String> entry : substitute.entrySet()) {
        if (substitutedAttributes.containsKey(attributeEntry.getKey())) {
          value = substitutedAttributes.get(attributeEntry.getKey());
        }
        String replace = value.replace("${" + entry.getKey() + "}", entry.getValue());
        substitutedAttributes.put(attributeEntry.getKey(), replace);
      }
      if (substitutedAttributes.get(attributeEntry.getKey()).contains("${")) {
        throw new RuntimeException("Unable to perform substitution for attributes " + substitutedAttributes + " and values " + substitute);
      }
    }
    return new MetricInputDefinition(substitutedAttributes);
  }

  public Map<String, String> getInputDefinitionAttributes() {
    return inputDefinitionAttributes;
  }

  public void setInputDefinitionAttributes(Map<String, String> inputDefinitionAttributes) {
    this.inputDefinitionAttributes = inputDefinitionAttributes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MetricInputDefinition that = (MetricInputDefinition) o;
    return inputDefinitionAttributes.equals(that.inputDefinitionAttributes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(inputDefinitionAttributes);
  }

  @Override
  public String toString() {
    return "MetricInputDefinition [inputDefinitionAttributes=" + inputDefinitionAttributes + "]";
  }

}
