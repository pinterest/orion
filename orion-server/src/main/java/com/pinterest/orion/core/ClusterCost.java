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

import java.util.HashMap;
import java.util.Map;

public class ClusterCost {

  private double nodeCost;
  private Map<String, EntityCost> entityCostMap;

  public ClusterCost() {
    entityCostMap = new HashMap<>();
  }

  /**
   * @return the nodeCost
   */
  public double getNodeCost() {
    return nodeCost;
  }

  /**
   * @param nodeCost the nodeCost to set
   */
  public void setNodeCost(double nodeCost) {
    this.nodeCost = nodeCost;
  }

  /**
   * @param entityCostMap the entityCostMap to set
   */
  public void setEntityCostMap(Map<String, EntityCost> entityCostMap) {
    this.entityCostMap = entityCostMap;
  }

  /**
   * @return the entityCostMap
   */
  public Map<String, EntityCost> getEntityCostMap() {
    return entityCostMap;
  }

  public static class EntityCost {
    private double networkCost;
    private double nodeCost;

    public EntityCost(double networkCost, double nodeCost) {
      this.networkCost = networkCost;
      this.nodeCost = nodeCost;
    }

    /**
     * @return the networkCost
     */
    public double getNetworkCost() {
      return networkCost;
    }

    /**
     * @param networkCost the networkCost to set
     */
    public void setNetworkCost(double networkCost) {
      this.networkCost = networkCost;
    }

    /**
     * @return the nodeCost
     */
    public double getNodeCost() {
      return nodeCost;
    }

    /**
     * @param nodeCost the nodeCost to set
     */
    public void setNodeCost(double nodeCost) {
      this.nodeCost = nodeCost;
    }
  }

}
