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
package com.pinterest.orion.agent.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;

import com.pinterest.orion.common.Metric;
import com.pinterest.orion.common.Value;

public class JMXMetricRetreiverTask extends MetricRetrieverTask {

  private static final Logger logger = Logger
      .getLogger(JMXMetricRetreiverTask.class.getCanonicalName());

  private MBeanServerConnection mbs;
  private MetricDefinition definition;

  public JMXMetricRetreiverTask(MetricDefinition definition, MBeanServerConnection mbs) {
    this.definition = definition;
    this.mbs = mbs;
  }

  @Override
  public Metric call() throws Exception {
    List<Value> values = new ArrayList<>();
    MetricOutputDefinition output = definition.getOutput();
    Metric metric = new Metric(output.getName(), output.getTags(), System.currentTimeMillis(),
        values, output.getTransmission());
    Map<String, String> def = definition.getInput().getInputDefinitionAttributes();
    String metricName = def.get(MetricInputDefinition.METRIC_NAME);
    String attributeName = def.get(MetricInputDefinition.ATTRIBUTE_NAME);
    Future<MetricValue> metricValueFuture = MetricsRetriever.getJMXMetricValue(mbs, metricName,
        attributeName);

    MetricValue metricValue = metricValueFuture.get();
    double metricValueDouble;
    try {
      metricValueDouble = metricValue.toDouble();
    } catch (Exception e) {
      logger.warning("Failed to convert metric value to double for " + definition.getInput().toString());
      return metric;
    }
    if (!metricValue.hadException()) {
      metric.addToValues(new Value(definition.getMetricType(), output.getName(), metricValueDouble));
    } else {
      logger.log(Level.FINE, "Got exception for " + output.getName(), metricValue.getException());
    }
    return metric;
  }

}
