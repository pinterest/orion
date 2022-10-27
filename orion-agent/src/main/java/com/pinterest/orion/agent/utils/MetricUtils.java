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
package com.pinterest.orion.agent.utils;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.google.common.collect.Lists;
import com.pinterest.orion.agent.BaseAgent;
import com.pinterest.orion.agent.metrics.JMXMetricRetreiverTask;
import com.pinterest.orion.agent.metrics.MetricDefinition;
import com.pinterest.orion.agent.metrics.MetricRetrieverTask;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;

import com.pinterest.orion.common.Metric;
import com.pinterest.orion.common.MetricType;
import com.pinterest.orion.common.Metrics;
import com.pinterest.orion.common.Value;

import io.dropwizard.metrics5.Counter;
import io.dropwizard.metrics5.Gauge;
import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;

public class MetricUtils {

  private static Logger logger = Logger.getLogger(MetricUtils.class.getCanonicalName());

  /**
   * Get an JMXConnector object. Return null if there is any failure.
   */
  public static JMXConnector getJMXConnector(String host, int jmxPort) {
    try {
      Map<String, String[]> env = new HashMap<>();
      JMXServiceURL address = new JMXServiceURL(
          "service:jmx:rmi://" + host + "/jndi/rmi://" + host + ":" + jmxPort + "/jmxrmi");
      return JMXConnectorFactory.connect(address, env);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to connect to MBeanServer " + host + ":" + jmxPort, e);
    }
    return null;
  }

  public static Set<MetricRetrieverTask> getTasksFromMetricDefinitionTemplates(
          Map<String, List<String>> entityValueMap,
          List<MetricDefinition> metricDefinitions,
          BaseAgent agent
  ) throws Exception {
    Set<MetricRetrieverTask> tasks = new HashSet<>();
    for (MetricDefinition def: metricDefinitions) {
      Set<MetricDefinition> newDefs = fillMetricDefinitionTemplate(entityValueMap, def, agent);
      for (MetricDefinition newDef: newDefs) {
        agent.addToTasksFromDefinition(tasks, newDef);
      }
    }
    return tasks;
  }

  public static Set<MetricDefinition> fillMetricDefinitionTemplate(Map<String, List<String>> entityValueMap, MetricDefinition def, BaseAgent agent) {
    try {
      List<List<Map.Entry<String, String>>> tupleListHolder = new ArrayList<>();
      for (String entity : def.getInjectedEntities()) {
        List<String> entityValues = entityValueMap.computeIfAbsent(entity, f -> agent.getEntityValues(entity));
        if (entityValues == null) {
          throw new Exception("Missing entity dependency: " + entity);
        }
        List<Map.Entry<String, String>> tupleList = new ArrayList<>();
        for (String val : entityValues) {
          tupleList.add(new AbstractMap.SimpleEntry<>(entity, val));
        }
        tupleListHolder.add(tupleList);
      }
      List<List<Map.Entry<String, String>>> cartesianProduct = Lists.cartesianProduct(tupleListHolder);
      Set<MetricDefinition> newDefs = cartesianProduct.stream()
              .map(l -> {
                Map<String, String> tupleMap = new HashMap<>();
                for (Map.Entry<String, String> entry : l) {
                  tupleMap.put(entry.getKey(), entry.getValue());
                }
                return tupleMap;
              })
              .map(def::substituteEntityValues)
              .collect(Collectors.toSet());
      return newDefs;
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to materialize metric for definition: " + def.toString(), e);
      return Collections.emptySet();
    }
  }

  @SuppressWarnings("rawtypes") 
  private static void convertGauges(Metrics toConvert, SortedMap<MetricName, Gauge> gauges, long timestamp) {
    for (Map.Entry<MetricName, Gauge> gaugeEntry: gauges.entrySet()) {
      MetricName name = gaugeEntry.getKey();
      Gauge entryValue = gaugeEntry.getValue();
      Metric converted = new Metric();
      Value val = new Value(MetricType.GAUGE, name.getKey(), (long) entryValue.getValue());
      converted.setValues(Collections.singletonList(val));
      converted.setTags(name.getTags());
      converted.setSeries(name.getKey());
      converted.setTimestamp(timestamp);
      toConvert.addToMetrics(converted);
    }
  }

  private static void convertCounters(Metrics toConvert, SortedMap<MetricName, Counter> counters, long timestamp) {
    for (Map.Entry<MetricName, Counter> counterEntry: counters.entrySet()) {
      MetricName name = counterEntry.getKey();
      Counter entryValue = counterEntry.getValue();
      Metric converted = new Metric();
      Value val = new Value(MetricType.COUNTER, name.getKey(), entryValue.getCount());
      converted.setValues(Collections.singletonList(val));
      converted.setTags(name.getTags());
      converted.setSeries(name.getKey());
      converted.setTimestamp(timestamp);
      toConvert.addToMetrics(converted);
    }
  }

  public static Metrics convertRegistryToMetrics(MetricRegistry registry) {
    Metrics converted = new Metrics();
    long timestamp = System.currentTimeMillis();
    SortedMap<MetricName, Gauge> gauges = registry.getGauges();
    SortedMap<MetricName, Counter> counters = registry.getCounters();
    // skip histograms for now
    convertGauges(converted, gauges, timestamp);
    convertCounters(converted, counters, timestamp);
    return converted;
  }

}
