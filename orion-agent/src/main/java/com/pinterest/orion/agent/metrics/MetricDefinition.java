package com.pinterest.orion.agent.metrics;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.pinterest.orion.common.MetricType;

public class MetricDefinition implements Serializable {

  private static final long serialVersionUID = 1L;
  private String metricsSource;
  private MetricInputDefinition input;
  private MetricType metricType;
  private List<String> injectedEntities;
  private MetricOutputDefinition output;

  public MetricDefinition() {
  }

  public MetricDefinition(String metricsSource,
                          MetricInputDefinition input,
                          MetricType metricType,
                          List<String> injectedEntities,
                          MetricOutputDefinition output) {
    this.metricsSource = metricsSource;
    this.input = input;
    this.metricType = metricType;
    this.injectedEntities = injectedEntities;
    this.output = output;
  }

  public String getMetricsSource() {
    return metricsSource;
  }

  public void setMetricsSource(String metricsSource) {
    this.metricsSource = metricsSource;
  }

  public MetricInputDefinition getInput() {
    return input;
  }

  public void setInput(MetricInputDefinition input) {
    this.input = input;
  }

  public MetricType getMetricType() {
    return metricType;
  }

  public void setMetricType(MetricType metricType) {
    this.metricType = metricType;
  }

  public List<String> getInjectedEntities() {
    return injectedEntities;
  }

  public void setInjectedEntities(List<String> injectedEntities) {
    this.injectedEntities = injectedEntities;
  }

  public MetricOutputDefinition getOutput() {
    return output;
  }

  public void setOutput(MetricOutputDefinition output) {
    this.output = output;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof MetricDefinition) {
      MetricDefinition v2 = ((MetricDefinition) obj);
      return this.input.equals(v2.input) && this.output.equals(v2.output);
    }
    return super.equals(obj);
  }

  public MetricDefinition substituteEntityValues(Map<String, String> entityValueMap) {
    return new MetricDefinition(metricsSource, input.substituteEntityValues(entityValueMap),
                                metricType, injectedEntities, output.substituteEntityValues(entityValueMap));
  }

  @Override
  public int hashCode() {
    return Objects.hash(metricsSource, input, metricType, injectedEntities, output);
  }

  @Override
  public String toString() {
    return "MetricDefinition [metricsInputType=" + metricsSource + ", input=" + input
        + ", metricType=" + metricType + ", injectedEntities=" + injectedEntities + ", output="
        + output + "]";
  }
}