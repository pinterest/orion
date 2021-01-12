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
package com.pinterest.orion.agent.metrics;

public class MetricValue {

  private final Object value;
  private final Exception exception;

  public MetricValue(Object value) {
    this(value, null);
  }

  public MetricValue(Exception e) {
    this(null, e);
  }

  public MetricValue(Object value, Exception e) {
    this.value = value;
    this.exception = e;
  }

  public boolean hadException() {
    return exception != null;
  }
  
  public Exception getException() {
    return exception;
  }
  
  public double toDouble() {
    if (value instanceof Long) {
      return ((Long) value).doubleValue();
    }
    return (Double) value;
  }

  public long toLong() {
    if (value instanceof Double) {
      return ((Double) value).longValue();
    }
    return (Long) value;
  }

}
