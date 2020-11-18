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

import com.pinterest.orion.agent.metrics.MetricValue;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;

public class JMXMetricValueRetrievingTask implements Callable<MetricValue> {

  private static final Logger logger = Logger.getLogger(JMXMetricValueRetrievingTask.class.getCanonicalName());

  private MBeanServerConnection mbs;
  private String metricName;
  private String attributeName;

  public JMXMetricValueRetrievingTask(MBeanServerConnection mbs,
                                      String metricName,
                                      String attributeName) {
    this.mbs = mbs;
    this.metricName = metricName;
    this.attributeName = attributeName;
  }

  @Override
  public MetricValue call() throws Exception {
    try {
      ObjectName pattern = ObjectName.getInstance(metricName);
      Set<ObjectInstance> objects = mbs.queryMBeans(pattern, null);
      logger.info("Got " + objects.size() + " metrics from MBeans pattern " + metricName);
      long sum = 0;
      for (ObjectInstance inst: objects) {
        Object obj = mbs.getAttribute(inst.getObjectName(), attributeName);
        sum += new MetricValue(obj).toLong();
      }
      logger.info("Retrieved value " + sum + " for MBeans pattern " + metricName + " from JMX");
      return new MetricValue(sum);
    } catch (Exception e) {
      return new MetricValue(e);
    }
  }
}
