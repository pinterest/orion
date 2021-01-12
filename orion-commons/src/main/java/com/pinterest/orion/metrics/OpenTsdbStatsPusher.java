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

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.dropwizard.metrics5.Counter;
import io.dropwizard.metrics5.Gauge;
import io.dropwizard.metrics5.Histogram;
import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;

/**
 * A daemon thread that periodically reads stats from dropwizard-metrics and sends them to
 * OpenTSDB.
 *
 * The thread polls the MetricsRegistry every N milliseconds to get the counter, gauge and
 * histogram values since the last interval (the very first interval is discarded,
 * since its start time is unknown). It is important that the intervals be as
 * close to each other as possible (so they can be compared to each other), so
 * every effort is made to keep the intervals identical (but this isn't a
 * real-time system, so there are no guarantees).
 *
 * A {@link OpenTsdbMetricConverter} is used to convert from Ostrich stats to
 * OpenTSDB stats. The converter can be used to rename stats, add tags and even
 * to modify the stat value. In addition, the logic for converting
 * {@link Histogram}s to OpenTSDB stats must be done inside an
 * {@link OpenTsdbMetricConverter}.
 */
public class OpenTsdbStatsPusher extends StatsPusher {

  private static final Logger LOG = Logger.getLogger(OpenTsdbStatsPusher.class.getCanonicalName());
  private static final int RETRY_SLEEP_MS = 100;
  private static final int MIN_SOCKET_TIME_MS = 200;
  private OpenTsdbClient client;
  protected OpenTsdbClient.MetricsBuffer buffer;
  protected OpenTsdbMetricConverter converter;

  public OpenTsdbStatsPusher() {
    this.buffer = new OpenTsdbClient.MetricsBuffer();
  }

  @Override
  public void configure(String sourceHostname,
                        String metricsPrefix,
                        String destinationHost,
                        int destinationPort,
                        long pollMillis) throws IOException {
    super.configure(sourceHostname, metricsPrefix, destinationHost, destinationPort, pollMillis);
    this.client = new OpenTsdbClient(destinationHost, destinationPort);
    this.converter = new OpenTsdbMetricConverter(metricsPrefix, sourceHostname);
  }

  protected void fillMetricsBuffer(MetricRegistry registry, int epochSecs) {
    buffer.reset();
    OpenTsdbClient.MetricsBuffer buf = buffer;

    for(Map.Entry<MetricName, Counter> entry : registry.getCounters().entrySet()) {
      converter.convertCounter(entry.getKey(), epochSecs, entry.getValue().getCount(), buf);
    }

    for(Map.Entry<MetricName, Gauge> entry : registry.getGauges().entrySet()) {
      converter.convertGauge(entry.getKey(), epochSecs, entry.getValue().getValue(), buf);
    }

    for(Map.Entry<MetricName, Histogram> entry : registry.getHistograms().entrySet()) {
      converter.convertMetric(entry.getKey(), epochSecs, entry.getValue(), buf);
    }
  }

  private void logOstrichStats(int epochSecs) {
    LOG.fine(() -> "Ostrich Metrics " + epochSecs + " " + buffer.toString());
  }

  @Override
  public long sendMetrics(boolean retryOnFailure) throws InterruptedException,
                                                  UnknownHostException {
    long startTimeinMillis = System.currentTimeMillis();
    long end = startTimeinMillis + pollMillis;

    int epochSecs = (int) (startTimeinMillis / 1000L);
    fillMetricsBuffer(SharedMetricRegistries.getDefault(), epochSecs);
    logOstrichStats(epochSecs);

    while (true) {
      try {
        LOG.fine("Sending metrics from buffer: " + buffer);
        client.sendMetrics(buffer);
        break;
      } catch (Exception ex) {
        LOG.log(Level.WARNING, "Failed to send stats to OpenTSDB, will retry up to next interval");
        if (!retryOnFailure) {
          break;
        }
        Thread.sleep(RETRY_SLEEP_MS);
        // re-initiaize OpenTsdbClient before retrying
        client = new OpenTsdbClient(destinationHost, destinationPort);
      }
      if (end - System.currentTimeMillis() < RETRY_SLEEP_MS + MIN_SOCKET_TIME_MS) {
        LOG.log(Level.SEVERE,
            "Failed to send epoch " + epochSecs + " to OpenTSDB, moving to next interval");
        break;
      }
      Thread.sleep(RETRY_SLEEP_MS);
    }

    return System.currentTimeMillis() - startTimeinMillis;
  }

  /**
   * @return the buffer
   */
  public OpenTsdbClient.MetricsBuffer getBuffer() {
    return buffer;
  }

  public void setConverter(OpenTsdbMetricConverter converter) {
    this.converter = converter;
  }

}