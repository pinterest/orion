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

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;

import com.google.common.collect.ImmutableMap;
import com.pinterest.orion.agent.metrics.MetricValue;
import com.pinterest.orion.agent.metrics.MetricsRetriever;
import com.pinterest.orion.common.Metric;
import com.pinterest.orion.common.MetricType;
import com.pinterest.orion.common.Value;

public class TopicMetricsRetrieverTask implements Callable<Metric> {

  private static final Logger LOG = Logger
      .getLogger(TopicMetricsRetrieverTask.class.getCanonicalName());
  private static List<TopicMetricHolder> METRICS = new ArrayList<>();

  static {
    METRICS.add(
        new TopicMetricHolder("kafka.server:type=BrokerTopicMetrics,name=BytesInPerSec,topic=%s",
            "OneMinuteRate", "BytesInPerSec-OneMinuteRate"));
    METRICS.add(
        new TopicMetricHolder("kafka.server:type=BrokerTopicMetrics,name=BytesOutPerSec,topic=%s",
            "OneMinuteRate", "BytesOutPerSec-OneMinuteRate"));
    METRICS.add(
        new TopicMetricHolder("kafka.server:type=BrokerTopicMetrics,name=BytesInPerSec,topic=%s",
            "FiveMinuteRate", "BytesInPerSec-FiveMinuteRate"));
    METRICS.add(
        new TopicMetricHolder("kafka.server:type=BrokerTopicMetrics,name=BytesOutPerSec,topic=%s",
            "FiveMinuteRate", "BytesOutPerSec-FiveMinuteRate"));
    METRICS.add(
        new TopicMetricHolder("kafka.server:type=BrokerTopicMetrics,name=BytesInPerSec,topic=%s",
            "FifteenMinuteRate", "BytesInPerSec-FifteenMinuteRate"));
    METRICS.add(
        new TopicMetricHolder("kafka.server:type=BrokerTopicMetrics,name=BytesOutPerSec,topic=%s",
            "FifteenMinuteRate", "BytesOutPerSec-FifteenMinuteRate"));
  }
  private MBeanServerConnection mbs;
  private String topic;

  public TopicMetricsRetrieverTask(MBeanServerConnection mbs, String topic) {
    this.mbs = mbs;
    this.topic = topic;
  }

  @Override
  public Metric call() throws Exception {
    List<Value> values = new ArrayList<>();
    Metric topicMetric = new Metric("topicmetric", ImmutableMap.of("topic", topic),
                                    System.currentTimeMillis(), values, Collections.emptySet());

    Map<String, Future<MetricValue>> listOfFutures = new HashMap<>();
    for (TopicMetricHolder entry : METRICS) {
      Future<MetricValue> metricValue = MetricsRetriever.getJMXMetricValue(mbs,
          String.format(entry.getMetricName(), topic), entry.getAttributeName());
      listOfFutures.put(entry.getKey(), metricValue);
    }

    for (Entry<String, Future<MetricValue>> future : listOfFutures.entrySet()) {
      MetricValue metricValue = future.getValue().get();
      if (!metricValue.hadException()) {
        topicMetric.addToValues(new Value(MetricType.GAUGE, future.getKey(), metricValue.toLong()));
      } else {
        LOG.log(Level.FINE, "Got exception for " + future.getKey(), metricValue.getException());
      }
    }
    return topicMetric;
  }

  public static class TopicMetricHolder {

    private String metricName;
    private String attributeName;
    private String key;

    public TopicMetricHolder(String metricName, String attributeName, String key) {
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
