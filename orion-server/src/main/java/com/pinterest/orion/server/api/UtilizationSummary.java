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
package com.pinterest.orion.server.api;

import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;

public class UtilizationSummary {

  private Map<String, MutableInt> instancesByType;
  private NetworkSummary networkSummary;
  private DiskSummary diskSummary;
  
  public UtilizationSummary() {
  }
  
  /**
   * @return the instancesByType
   */
  public Map<String, MutableInt> getInstancesByType() {
    return instancesByType;
  }

  /**
   * @param instancesByType the instancesByType to set
   */
  public void setInstancesByType(Map<String, MutableInt> instancesByType) {
    this.instancesByType = instancesByType;
  }

  /**
   * @return the networkSummary
   */
  public NetworkSummary getNetworkSummary() {
    return networkSummary;
  }

  /**
   * @param networkSummary the networkSummary to set
   */
  public void setNetworkSummary(NetworkSummary networkSummary) {
    this.networkSummary = networkSummary;
  }

  /**
   * @return the diskSummary
   */
  public DiskSummary getDiskSummary() {
    return diskSummary;
  }

  /**
   * @param diskSummary the diskSummary to set
   */
  public void setDiskSummary(DiskSummary diskSummary) {
    this.diskSummary = diskSummary;
  }


  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "UtilizationSummary [instancesByType=" + instancesByType + ", networkSummary="
        + networkSummary + ", diskSummary=" + diskSummary + "]";
  }


  public static class NetworkSummary {

    private long currentNetworkUsageInMB;
    private long totalNetworkUsageInMB;

    public NetworkSummary() {
    }

    public NetworkSummary(long currentNetworkUsageInMB, long totalNetworkUsageInMB) {
      this.currentNetworkUsageInMB = currentNetworkUsageInMB;
      this.totalNetworkUsageInMB = totalNetworkUsageInMB;
    }

    /**
     * @return the currentNetworkUsageInMB
     */
    public long getCurrentNetworkUsageInMB() {
      return currentNetworkUsageInMB;
    }

    /**
     * @param currentNetworkUsageInMB the currentNetworkUsageInMB to set
     */
    public void setCurrentNetworkUsageInMB(long currentNetworkUsageInMB) {
      this.currentNetworkUsageInMB = currentNetworkUsageInMB;
    }

    /**
     * @return the totalNetworkUsageInMB
     */
    public long getTotalNetworkUsageInMB() {
      return totalNetworkUsageInMB;
    }

    /**
     * @param totalNetworkUsageInMB the totalNetworkUsageInMB to set
     */
    public void setTotalNetworkUsageInMB(long totalNetworkUsageInMB) {
      this.totalNetworkUsageInMB = totalNetworkUsageInMB;
    }

  }

  public static class DiskSummary {

    private long currentDiskUsageInMB;
    private long totalDiskAvailableInMB;
    
    public DiskSummary() {
    }

    public DiskSummary(long currentDiskUsageInMB, long totalDiskAvailableInMB) {
      this.currentDiskUsageInMB = currentDiskUsageInMB;
      this.totalDiskAvailableInMB = totalDiskAvailableInMB;
    }

    /**
     * @return the currentDiskUsageInMB
     */
    public long getCurrentDiskUsageInMB() {
      return currentDiskUsageInMB;
    }

    /**
     * @param currentDiskUsageInMB the currentDiskUsageInMB to set
     */
    public void setCurrentDiskUsageInMB(long currentDiskUsageInMB) {
      this.currentDiskUsageInMB = currentDiskUsageInMB;
    }

    /**
     * @return the totalDiskAvailableInMB
     */
    public long getTotalDiskAvailableInMB() {
      return totalDiskAvailableInMB;
    }

    /**
     * @param totalDiskAvailableInMB the totalDiskAvailableInMB to set
     */
    public void setTotalDiskAvailableInMB(long totalDiskAvailableInMB) {
      this.totalDiskAvailableInMB = totalDiskAvailableInMB;
    }

  }

}
