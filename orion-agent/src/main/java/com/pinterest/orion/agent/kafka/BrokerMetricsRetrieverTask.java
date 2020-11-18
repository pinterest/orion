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
package com.pinterest.orion.agent.kafka;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;

import com.pinterest.orion.agent.metrics.MetricValue;
import com.pinterest.orion.agent.metrics.MetricsRetriever;
import com.pinterest.orion.common.Metric;
import com.pinterest.orion.common.MetricType;
import com.pinterest.orion.common.Value;

public class BrokerMetricsRetrieverTask implements Callable<Metric> {
  private static final Logger LOG = Logger
      .getLogger(BrokerMetricsRetrieverTask.class.getCanonicalName());
  private static List<MetricHolder> METRICS = new ArrayList<>();

  static {
    METRICS.add(
        new MetricHolder("kafka.server:type=ReplicaFetcherManager,name=MaxLag,clientId=Replica",
            "Value", "MaxLag"));
  }
  private MBeanServerConnection mbs;

  public BrokerMetricsRetrieverTask(MBeanServerConnection mbs) {
    this.mbs = mbs;
  }

  @Override
  public Metric call() throws Exception {
    List<Value> values = new ArrayList<>();
    Metric brokerMetric = new Metric("brokerMetric", Collections.EMPTY_MAP,
        System.currentTimeMillis(), values, Collections.emptySet());

    Map<String, Future<MetricValue>> listOfFutures = new HashMap<>();
    for (MetricHolder entry : METRICS) {
      Future<MetricValue> metricValue = MetricsRetriever.getJMXMetricValue(mbs,
          entry.getMetricName(), entry.getAttributeName());
      listOfFutures.put(entry.getKey(), metricValue);
    }

    for (Map.Entry<String, Future<MetricValue>> future : listOfFutures.entrySet()) {
      MetricValue metricValue = future.getValue().get();
      if (!metricValue.hadException()) {
        brokerMetric.addToValues(new Value(MetricType.GAUGE, future.getKey(), metricValue.toLong()));
      } else {
        LOG.log(Level.FINE, "Got exception for " + future.getKey(), metricValue.getException());
      }
    }
    return brokerMetric;
  }

  public static class MetricHolder {

    private String metricName;
    private String attributeName;
    private String key;

    public MetricHolder(String metricName, String attributeName, String key) {
      super();
      this.metricName = metricName;
      this.attributeName = attributeName;
      this.key = key;
    }

    /**
     * @return the metricName
     */
    public String getMetricName() {
      return metricName;
    }

    /**
     * @return the attributeName
     */
    public String getAttributeName() {
      return attributeName;
    }

    /**
     * @return the key
     */
    public String getKey() {
      return key;
    }

  }
}
