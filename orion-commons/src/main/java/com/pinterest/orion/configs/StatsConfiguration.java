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
package com.pinterest.orion.configs;

public class StatsConfiguration {
  private boolean enabled = true;
  private String metricsPrefix = "orion.server";
  private String destinationHostname = "localhost";
  private int destinationPort = 18126;
  private int pushInterval = 60000;

  /**
   * @return the enabled
   */
  public boolean isEnabled() {
    return enabled;
  }
  /**
   * @param enabled the enabled to set
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getMetricsPrefix() {
    return metricsPrefix;
  }

  public void setMetricsPrefix(String metricsPrefix) {
    this.metricsPrefix = metricsPrefix;
  }

  /**
   * @return the destinationHostname
   */
  public String getDestinationHostname() {
    return destinationHostname;
  }
  /**
   * @param destinationHostname the destinationHostname to set
   */
  public void setDestinationHostname(String destinationHostname) {
    this.destinationHostname = destinationHostname;
  }
  /**
   * @return the destinationPort
   */
  public int getDestinationPort() {
    return destinationPort;
  }
  /**
   * @param destinationPort the destinationPort to set
   */
  public void setDestinationPort(int destinationPort) {
    this.destinationPort = destinationPort;
  }
  /**
   * @return the pushInterval
   */
  public int getPushInterval() {
    return pushInterval;
  }
  /**
   * @param pushInterval the pushInterval to set
   */
  public void setPushInterval(int pushInterval) {
    this.pushInterval = pushInterval;
  }

}