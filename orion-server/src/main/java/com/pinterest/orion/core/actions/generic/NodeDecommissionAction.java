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
package com.pinterest.orion.core.actions.generic;

import java.util.logging.Level;

import com.pinterest.orion.core.Node;

public class NodeDecommissionAction extends NodeAction {


  @Override
  public MaintenancePolicy configureMaintenancePolicy() {
    return MaintenancePolicy.ENABLE_BEFORE_EXECUTION;
  }

  @Override
  public void runAction() throws Exception {
    // first check if node exists in the cluster
    if (!initializeNode(false)){
      return;
    }

    if (!checkIfDecommissionable(node)) {
      markFailed("Node " + nodeId + " is not decommissionable. Please check if the node has been evacuated.");
      return;
    }

    try {
      if(!decommission(node)) {
        markFailed("Failed to decommission node " + nodeId);
        return;
      }
    } catch (Exception e) {
      markFailed("Failed to decommission node " + nodeId + ": " + e);
      logger().log(Level.SEVERE, "Failed to decommission node " + nodeId, e);
      return;
    }
    getResult().appendOut("Decommissioned node " + nodeId + " from cluster.");
    markSucceeded();
  }

  public boolean checkIfDecommissionable(Node node) {
    return true;
  }

  public boolean decommission(Node node) throws Exception {
    node.getCluster().getNodeMap().remove(node.getCurrentNodeInfo().getNodeId());
    return true;
  }

  @Override
  public String getName() {
    return "NodeDecommissionAction";
  }
}
