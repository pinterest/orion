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
package com.pinterest.orion.server;

import io.dropwizard.metrics5.Gauge;
import io.dropwizard.metrics5.MetricName;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class HeartbeatService {
  private static final long heartbeatInterval = 60000L; // one minute
  private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  public void start() {
    Heartbeat heartbeat = new Heartbeat();
    scheduler.scheduleAtFixedRate(heartbeat, 0, heartbeatInterval, TimeUnit.MILLISECONDS);
  }

  private static class Heartbeat implements Runnable {
    private Logger logger = Logger.getLogger(Heartbeat.class.getCanonicalName());
    @Override
    public void run() {
      OrionServer.METRICS.register(
          MetricName.build(MetricsConstants.HEARTBEAT),
          (Gauge<Integer>) () -> 1
      );
      logger.fine("Heartbeat set at " + System.currentTimeMillis());
    }
  }
}
