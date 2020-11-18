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

import org.apache.kafka.common.TopicPartition;

import com.pinterest.orion.common.Metric;
import com.pinterest.orion.common.MetricType;
import com.pinterest.orion.common.Metrics;
import com.pinterest.orion.common.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

/**
 * Log size metric on each topic partition.
 *
 * log size: kafka.log:type=Log,name=Size,topic=%s,partition=%d"* num log segs:
 * kafka.log:type=Log,name=NumLogSegments,topic=%s,partition=%d start offset:
 * kafka.log:type=Log,name=LogStartOffset,topic=%s,partition=%d end offset:
 * kafka.log:type=Log,name=LogEndOffset,topic=%s,partition=%d under-rep:
 * kafka.cluster:type=Partition,name=UnderReplicated,topic=%s,partition=%d
 *
 */
public class TopicPartitionMetricsRetrieverTask implements Callable<Metrics> {

  private static final Logger LOG = Logger
      .getLogger(TopicPartitionMetricsRetrieverTask.class.getCanonicalName());

  private MBeanServerConnection mbs;
  private TopicPartition topicPartition;
  private boolean isLeader;
  private boolean inReassignment;

  public TopicPartitionMetricsRetrieverTask(MBeanServerConnection mbs,
                                           TopicPartition topicPartition,
                                           boolean isLeader,
                                           boolean inReassignment) {
    this.mbs = mbs;
    this.topicPartition = topicPartition;
    this.isLeader = isLeader;
    this.inReassignment = inReassignment;
  }

  @Override
  public Metrics call() throws Exception {

    Metrics metrics = new Metrics();
    Metric tpMetric = new Metric();
    tpMetric.setTimestamp(System.currentTimeMillis());
    Map<String, String> tags = new HashMap<>();
    tags.put("topic", topicPartition.topic());
    tags.put("partition", String.valueOf(topicPartition.partition()));
    tpMetric.setTags(tags);
    tpMetric.setSeries("tpMetric");
    metrics.addToMetrics(tpMetric);

    String logSizeMetric = String.format("kafka.log:type=Log,name=Size,topic=%s,partition=%d",
        topicPartition.topic(), topicPartition.partition());

    LOG.info("logSizeMetric = " + logSizeMetric);

    Long longValue = 0L;
    try {
      longValue = (Long) mbs.getAttribute(new ObjectName(logSizeMetric), "Value");
      tpMetric.addToValues(new Value(MetricType.GAUGE, "logSize", longValue));
    } catch (InstanceNotFoundException e) {
      LOG.log(Level.INFO, "Could not find metric " + logSizeMetric, e);
    }

    String numSegmentsMetric = String.format(
        "kafka.log:type=Log,name=NumLogSegments,topic=%s,partition=%d", topicPartition.topic(),
        topicPartition.partition());
    int intValue;
    try {
      intValue = (Integer) mbs.getAttribute(new ObjectName(numSegmentsMetric), "Value");
      tpMetric.addToValues(new Value(MetricType.GAUGE, "numSegments", intValue));
    } catch (InstanceNotFoundException e) {
      LOG.log(Level.INFO, "Could not find metric " + numSegmentsMetric, e);
    }

    String startOffsetMetric = String.format(
        "kafka.log:type=Log,name=LogStartOffset,topic=%s,partition=%d", topicPartition.topic(),
        topicPartition.partition());
    try {
      longValue = (Long) mbs.getAttribute(new ObjectName(startOffsetMetric), "Value");
      tpMetric.addToValues(new Value(MetricType.GAUGE, "startOffsetMetric", longValue));
    } catch (InstanceNotFoundException e) {
      LOG.log(Level.INFO, "Could not find metric " + startOffsetMetric, e);
    }

    String endOffsetMetric = String.format(
        "kafka.log:type=Log,name=LogEndOffset,topic=%s,partition=%d", topicPartition.topic(),
        topicPartition.partition());
    try {
      longValue = (Long) mbs.getAttribute(new ObjectName(endOffsetMetric), "Value");
      tpMetric.addToValues(new Value(MetricType.GAUGE, "endOffsetMetric", longValue));
    } catch (InstanceNotFoundException e) {
      LOG.log(Level.INFO, "Could not find metric " + endOffsetMetric, e);
    }

    String underReplicatedMetric = String.format(
        "kafka.cluster:type=Partition,name=UnderReplicated,topic=%s,partition=%d",
        topicPartition.topic(), topicPartition.partition());
    try {
      intValue = (Integer) mbs.getAttribute(new ObjectName(underReplicatedMetric), "Value");
      tpMetric.addToValues(new Value(MetricType.GAUGE, "underReplicatedMetric", intValue));
    } catch (InstanceNotFoundException e) {
      LOG.log(Level.INFO, "Could not find metric " + underReplicatedMetric, e);
    }
    tpMetric.addToValues(new Value(MetricType.GAUGE, "isLeader", isLeader ? 1 : 0));
    tpMetric.addToValues(new Value(MetricType.GAUGE, "inReassignment", inReassignment ? 1 : 0));
    return metrics;
  }
}
