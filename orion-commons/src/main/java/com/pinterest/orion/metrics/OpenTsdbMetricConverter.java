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
package com.pinterest.orion.metrics;

import io.dropwizard.metrics5.Histogram;
import io.dropwizard.metrics5.MetricName;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Converts Dropwizard metrics to OpenTSDB metrics.
 *
 * The converter takes a prefix and a hostname. The prefix is added to the front of every OpenTSDB
 * metric name, and the hostname is added as a "host=HOSTNAME" tag on every OpenTSDB metric.
 *
 * Ostrich stats are expected to be named like this:
 *
 *  "a.b.c.d tag1=value1 tag2=value2 ..."
 *
 * For counters and gauges these names are converted to OpenTSDB metrics like this:
 *
 *  "PREFIX.a.b.c.d host=HOSTNAME tag1=value1 tag2=value2 ..."
 *
 * For metrics these names are converted to a number of percentiles and counts:
 *
 *  "PREFIX.a.b.c.d.p50 host=HOSTNAME tag1=value1 ..."
 *  "PREFIX.a.b.c.d.p90 host=HOSTNAME tag1=value1 ..."
 *  "PREFIX.a.b.c.d.p95 host=HOSTNAME tag1=value1 ..."
 *  "PREFIX.a.b.c.d.p99 host=HOSTNAME tag1=value1 ..."
 *  "PREFIX.a.b.c.d.max host=HOSTNAME tag1=value1 ..."
 *  "PREFIX.a.b.c.d.count host=HOSTNAME tag1=value1 ..."
 *  "PREFIX.a.b.c.d.avg host=HOSTNAME tag1=value1 ..."
 *
 * The "addMetric" static function is provided to make it easier to add Ostrich metric names that
 * contain tags.
 */
public class OpenTsdbMetricConverter {
  
  // According to http://opentsdb.net/docs/build/html/user_guide/writing.html
  public static final String VALID_OPENSTD_STAT_TAG_PATTERN = "[a-zA-Z0-9_./-]+";
  private static boolean enableGranularMetrics;

  private final String prefix;
  private final Map<String, String> defaultTags = new HashMap<>();

  public OpenTsdbMetricConverter(String prefix, String hostname) {
    this.prefix = prefix;
    this.defaultTags.put("host", hostname);
  }

  public boolean convertCounter(
      MetricName name, int epochSecs, float value, OpenTsdbClient.MetricsBuffer buffer) {
    String statName = getPrefixedKey(name);
    String tags = getTags(name);

    buffer.addMetric(statName, epochSecs, value, tags);
    return true;
  }

  public boolean convertGauge(
      MetricName name, int epochSecs, Object value, OpenTsdbClient.MetricsBuffer buffer) {
    String statName = getPrefixedKey(name);
    String tags = getTags(name);

    if(value instanceof Number) {
      buffer.addMetric(statName, epochSecs, ((Number) value).floatValue(), tags);
      return true;
    }
    return false;
  }

  public boolean convertMetric(
      MetricName name, int epochSecs, Histogram hist, OpenTsdbClient.MetricsBuffer buffer) {
    String statName = getPrefixedKey(name);
    String tags = getTags(name);

    float p90 = (float) hist.getSnapshot().getValue(0.9);
    float p99 = (float) hist.getSnapshot().getValue(0.99);
    long max = hist.getSnapshot().getMax();

    long count = hist.getCount();
    float avg = (float) hist.getSnapshot().getMean();

    buffer.addMetric(statName + ".p90", epochSecs, p90, tags);
    buffer.addMetric(statName + ".p99", epochSecs, p99, tags);
    buffer.addMetric(statName + ".max", epochSecs, max, tags);
    buffer.addMetric(statName + ".count", epochSecs, count, tags);
    buffer.addMetric(statName + ".avg", epochSecs, avg, tags);
    return true;
  }

  private String getTags(MetricName name) {

    return Stream.concat(
        name.getTags().entrySet().stream(),
        defaultTags.entrySet().stream()
    )
    .map(e -> e.getKey()+ "=" + e.getValue())
    .collect(Collectors.joining(" "));
  }

  private String getPrefixedKey(MetricName name) {
    return new StringBuilder(prefix).append(".").append(name.getKey()).toString();
  }
}