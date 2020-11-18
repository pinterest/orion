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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.management.MBeanServerConnection;

import com.pinterest.orion.agent.kafka.JMXMetricValueRetrievingTask;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.pinterest.orion.common.Metric;


public class MetricsRetriever {

  private static final int METRIC_COLLECTOR_THREADPOOL_SIZE = 40;

  private static ExecutorService metricValuesThreadPool = Executors.newFixedThreadPool(
      METRIC_COLLECTOR_THREADPOOL_SIZE,
      new ThreadFactoryBuilder().setNameFormat("MetricsRetriever-MetricValuesThread: %d").build());

  private static ExecutorService metricsThreadPool = Executors.newFixedThreadPool(METRIC_COLLECTOR_THREADPOOL_SIZE,
      new ThreadFactoryBuilder().setNameFormat("MetricsRetriever-MetricThread: %d").build());

  public static Future<MetricValue> getJMXMetricValue(MBeanServerConnection mbs,
                                                      String metricName, String attributeName) {
    Callable<MetricValue> task =
        new JMXMetricValueRetrievingTask(mbs, metricName, attributeName);
    return metricValuesThreadPool.submit(task);
  }

  public static Future<Metric> getMetric(MetricRetrieverTask task) {
    return metricsThreadPool.submit(task);
  }
}
