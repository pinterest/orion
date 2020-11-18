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
package com.pinterest.orion.core;

public class Utilization {

  private double networkUtilizationInMBPerSecond;
  private double diskUtilizationInMB;
  private double memoryUtilizationInMB;
  private double cpuUtilizationInPercentage;

  public Utilization() {
  }

  public Utilization(double networkUtilizationInMBPerSecond, double diskUtilizationInMB) {
    this.networkUtilizationInMBPerSecond = networkUtilizationInMBPerSecond;
    this.diskUtilizationInMB = diskUtilizationInMB;
  }

  public Utilization(double networkUtilizationInMBPerSecond,
                     double diskUtilizationInMB,
                     double memoryUtilizationInMB) {
    this.networkUtilizationInMBPerSecond = networkUtilizationInMBPerSecond;
    this.diskUtilizationInMB = diskUtilizationInMB;
    this.memoryUtilizationInMB = memoryUtilizationInMB;
  }

  public Utilization(double networkUtilizationInMBPerSecond,
                     double diskUtilizationInMB,
                     double memoryUtilizationInMB,
                     double cpuUtilizationInPercentage) {
    this.networkUtilizationInMBPerSecond = networkUtilizationInMBPerSecond;
    this.diskUtilizationInMB = diskUtilizationInMB;
    this.memoryUtilizationInMB = memoryUtilizationInMB;
    this.cpuUtilizationInPercentage = cpuUtilizationInPercentage;
  }

  /**
   * @return the cpuUtilizationInPercentage
   */
  public double getCpuUtilizationInPercentage() {
    return cpuUtilizationInPercentage;
  }

  /**
   * @param cpuUtilizationInPercentage the cpuUtilizationInPercentage to set
   */
  public void setCpuUtilizationInPercentage(double cpuUtilizationInPercentage) {
    this.cpuUtilizationInPercentage = cpuUtilizationInPercentage;
  }


  /**
   * @return the networkUtilizationInMBPerSecond
   */
  public double getNetworkUtilizationInMBPerSecond() {
    return networkUtilizationInMBPerSecond;
  }

  /**
   * @param networkUtilizationInMBPerSecond the networkUtilizationInMBPerSecond to set
   */
  public void setNetworkUtilizationInMBPerSecond(double networkUtilizationInMBPerSecond) {
    this.networkUtilizationInMBPerSecond = networkUtilizationInMBPerSecond;
  }

  /**
   * @return the memoryUtilizationInMB
   */
  public double getMemoryUtilizationInMB() {
    return memoryUtilizationInMB;
  }

  /**
   * @param memoryUtilizationInMB the memoryUtilizationInMB to set
   */
  public void setMemoryUtilizationInMB(double memoryUtilizationInMB) {
    this.memoryUtilizationInMB = memoryUtilizationInMB;
  }

  /**
   * @return the diskUtilizationInMB
   */
  public double getDiskUtilizationInMB() {
    return diskUtilizationInMB;
  }

  /**
   * @param diskUtilizationInMB the diskUtilizationInMB to set
   */
  public void setDiskUtilizationInMB(double diskUtilizationInMB) {
    this.diskUtilizationInMB = diskUtilizationInMB;
  }

}
