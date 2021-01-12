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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.*;

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
  public MetricValue call() {
    try {
      ObjectName pattern = ObjectName.getInstance(metricName);
      Set<ObjectInstance> objects = mbs.queryMBeans(pattern, null);
      if (objects.size() != 1) {
        logger.warning("Retrieved " + objects.size() + " MBeans for metric " + pattern + " but expected 1." +
                               " Will default to performing summation aggregation.");
      }
      logger.fine("Got " + objects.size() + " beans from MBeans pattern " + metricName);
      double sum = 0;
      for (ObjectInstance inst: objects) {
        Object obj = mbs.getAttribute(inst.getObjectName(), attributeName);
        sum += new MetricValue(obj).toDouble();
      }
      logger.fine("Retrieved value " + sum + " for metricName " + metricName +
                          " attributeName " + attributeName + " from JMX");
      return new MetricValue(sum);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Exception when retrieving MBean attribute " +
                             attributeName + " for metric " + metricName + ", defaulting to value=0", e);
      return new MetricValue(0);
    }
  }
}
