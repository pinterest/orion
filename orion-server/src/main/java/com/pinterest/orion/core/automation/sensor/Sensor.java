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

import java.util.Map;
import java.util.logging.Logger;

import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.Context;
import com.pinterest.orion.core.Plugin;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.State;

/**
 * A sensor plugin observes the external system, populates the {@link State}
 * with derived attributes based on the observations it made.
 *
 * <pre>

             +------------+    +------------+
             |  External  |    |  External  |
             |  System(s) |    |  System(s) |
             +------------+    +------------+
                   +                 +
                   |                 |
                   v                 v
             +------------+    +------------+          +-----------+
  +-----+    |            |    |            |          |           |
  |State|+-->|  Monitor1  |+-->|  Monitor2  |+--....-->|Operator(s)|
  +-----+    |            |    |            |          |           |
             +------------+    +------------+          +-----------+

       observe            observe
 * </pre>
 */
public abstract class Sensor implements Plugin {

  private String sensorIdentifier;
  protected Logger logger = Logger.getLogger(this.getClass().getName());
  private long interval;

  /**
   * @return the interval for scheduling the monitor
   */
  public final long getInterval() {
    return interval;
  };

  public final void setInterval(long interval) {
    this.interval = interval;
  }

  /**
   * @param cluster to observe
   * @throws Exception
   */

  public abstract void observe(Cluster cluster) throws Exception;

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {

  }

  public void setAttribute(Context ctx, String key, Object value) {
    ctx.setAttribute(key, value, this.sensorIdentifier);
  }

  public void setHiddenAttribute(Context ctx, String key, Object value) {
    ctx.setHiddenAttribute(key, value, this.sensorIdentifier);
  }

  public Logger getLogger(Cluster cluster) {
    return Logger.getLogger(cluster.getClusterId() + ":" + getName());
  }
  
  public String getSensorIdentifier() {
    return sensorIdentifier;
  }
  
  public void setSensorIdentifier(String sensorIdentifier) {
    this.sensorIdentifier = sensorIdentifier;
  }
}
