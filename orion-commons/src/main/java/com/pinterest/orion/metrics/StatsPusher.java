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
import java.util.logging.Level;
import java.util.logging.Logger;

import io.dropwizard.metrics5.SharedMetricRegistries;

/**
 * Provides an abstraction to plugin custom metrics destination for Singer
 * metrics. To implement your own {@link StatsPusher} you will need to override
 * the sendMetrics method which is invoked at the configured frequency. <br>
 * <br>
 * {@link StatsPusher} is designed to run as an independent Daemon Thread.
 */
public abstract class StatsPusher extends Thread {

  private static final Logger LOG = Logger.getLogger(StatsPusher.class.getCanonicalName());
  protected long pollMillis;
  protected String sourceHostname;
  protected String metricsPrefix;
  protected String destinationHost;
  protected int destinationPort;

  public StatsPusher() {
    setDaemon(true);
  }

  public void configure(String sourceHostname,
                        String metricsPrefix,
                        String destinationHost,
                        int destinationPort,
                        long pollMillis) throws IOException {
    this.sourceHostname = sourceHostname;
    this.metricsPrefix = metricsPrefix;
    this.destinationHost = destinationHost;
    this.destinationPort = destinationPort;
    this.pollMillis = pollMillis;
  }

  @Override
  public void run() {
    try {
      // Ignore the first interval, since we don't know when stats started being
      // recorded,
      // and we want to make sure all intervals are roughly the same length.
      Thread.sleep(pollMillis);
      while (!Thread.currentThread().isInterrupted()) {
        long elapsedTimeMillis = sendMetrics(true);
        Thread.sleep(Math.max(0, pollMillis - elapsedTimeMillis));
      }
    } catch (InterruptedException ex) {
      LOG.info("OpenTsdbMetricsPusher thread interrupted, exiting");
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, "Unexpected error in OpenTSDBMetricsPusher, exiting", ex);
    }
  }

  public abstract long sendMetrics(boolean retryOnFailure) throws InterruptedException, IOException;

}