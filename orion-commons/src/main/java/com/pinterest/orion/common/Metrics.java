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
package com.pinterest.orion.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Metrics implements Serializable {
  
  private static final long serialVersionUID = 1L;
  private List<Metric> metrics;
  
  public Metrics() {
    metrics = new ArrayList<>();
  }

  /**
   * @return the metrics
   */
  public List<Metric> getMetrics() {
    return metrics;
  }

  /**
   * @param metrics the metrics to set
   */
  public void setMetrics(List<Metric> metrics) {
    this.metrics = metrics;
  }

  public void addToMetrics(Metric metric) {
    metrics.add(metric);
  }

}
