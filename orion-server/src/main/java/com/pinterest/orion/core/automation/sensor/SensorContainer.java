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
package com.pinterest.orion.core.automation.sensor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pinterest.orion.core.Cluster;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SensorContainer implements Runnable {
  private static final Logger logger = Logger.getLogger(SensorContainer.class.getName());
  private Sensor sensor;
  private Cluster cluster;
  private volatile boolean previousSuccess = true;
  private volatile Exception previousError = null;
  private volatile long previousFinishTime = 0;
  private ScheduledExecutorService executor;
  private ScheduledFuture<?> currentScheduledFuture;

  public SensorContainer(Sensor monitor, Cluster cluster, ScheduledExecutorService executor) {
    this.sensor = monitor;
    this.cluster = cluster;
    this.executor = executor;
  }

  public ScheduledFuture<?> scheduleNow() {
    if(currentScheduledFuture != null ){
      currentScheduledFuture.cancel(true);
    }
    currentScheduledFuture = this.executor.schedule(this, 0, TimeUnit.SECONDS);
    return currentScheduledFuture;
  }

  public void start() {
    scheduleNow();
  }

  @Override
  public void run() {
    try {
      sensor.observe(cluster);
      previousSuccess = true;
      previousError = null;
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Exception when running sensor " + sensor.getName() + " on cluster " + cluster.getClusterId(), e);
      previousSuccess = false;
      previousError = e;
    }
    previousFinishTime = System.currentTimeMillis();
    currentScheduledFuture = this.executor.schedule(this, sensor.getInterval(), TimeUnit.SECONDS);
  }

  public Sensor getSensor() {
    return sensor;
  }

  public boolean isPreviousSuccess() {
    return previousSuccess;
  }

  public Exception getPreviousError() {
    return previousError;
  }

  public long getPreviousFinishTime() { return previousFinishTime; }

  @JsonIgnore
  public long getSensorInterval() {
    return sensor.getInterval();
  }
}
