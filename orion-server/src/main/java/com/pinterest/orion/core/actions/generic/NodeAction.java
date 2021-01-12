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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pinterest.orion.common.CmdResult;
import com.pinterest.orion.common.NodeCmd;
import com.pinterest.orion.core.Attribute;
import com.pinterest.orion.core.Node;
import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.utils.OrionConstants;

public abstract class NodeAction extends Action {

  public static final String POSTHEALTHCHECK_INTERVAL = "postHealthcheckInterval";
  public static final String RECOVERY_TIMEOUT = "recoveryTimeout";

  enum MaintenancePolicy {
    NONE,
    ENABLE_AND_RESTORE_WHEN_COMPLETE,
    ENABLE_BEFORE_EXECUTION,
    DISABLE_AFTER_EXECUTION
  }

  private static final int DEFAULT_NODE_ACTION_TIMEOUT = 300;// 300s default timeout
  protected static final String ATTR_ACTION_TIMEOUT = "actionTimeout";
  protected Node node;
  protected String nodeId;

  @Override
  public void runAction() throws Exception {
    if(!initializeNode(true)){
      return;
    }
    if (!isCancelled()) {
      MaintenancePolicy maintenancePolicy = configureMaintenancePolicy();
      boolean prevMaintenanceMode = node.isUnderMaintenance();

      if (maintenancePolicy == MaintenancePolicy.ENABLE_AND_RESTORE_WHEN_COMPLETE ||
          maintenancePolicy == MaintenancePolicy.ENABLE_BEFORE_EXECUTION) {
          node.setMaintenance(true);
      }

      try {
        int timeout = getTimeout();
        NodeCmd cmd = runNodeAction(timeout);
        if (cmd == null) {
          throw new UnsupportedOperationException();
        }
        this.setResult(cmd.getResult());
        getResult().appendOut("Triggered command on node");
        CmdResult cmdResult = cmd.get(timeout, TimeUnit.SECONDS);
        getResult().appendOut("Node reported action completed. Post action validation running...");
        if (!isCmdSuccessful(cmdResult)) {
          markFailed("Service action on " + node.getCurrentNodeInfo().getHostname()
              + " failed due to non-zero exit code");
          return;
        }
        postActionValidation();
        if (maintenancePolicy == MaintenancePolicy.DISABLE_AFTER_EXECUTION) {
          node.setMaintenance(false);
        }
      } catch (Exception e) {
        e.printStackTrace();
        markFailed(e);
      } finally {
        node.clearActionQueue();
        if (maintenancePolicy == MaintenancePolicy.ENABLE_AND_RESTORE_WHEN_COMPLETE) {
          node.setMaintenance(prevMaintenanceMode);
        }
      }
    }
  }

  /**
   * Override to return maintenance policy settings for the action
   * @return maintenance policy settings for the action
   */
  public MaintenancePolicy configureMaintenancePolicy() {
    return MaintenancePolicy.NONE;
  }

  @JsonIgnore
  protected boolean isCmdSuccessful(CmdResult cmdResult) {
    return cmdResult.hasCompleted() && cmdResult.getExitCode() == 0;
  }

  @JsonIgnore
  protected int getTimeout() {
    int timeout = DEFAULT_NODE_ACTION_TIMEOUT;
    if (containsAttribute(ATTR_ACTION_TIMEOUT)) {
      timeout = getAttribute(this, ATTR_ACTION_TIMEOUT).getValue();
    }
    return timeout;
  }

  protected boolean initializeNode(boolean checkIfAgentPresent) {
    Attribute attribute = getAttribute(OrionConstants.NODE_ID);
    if (attribute == null) {
      markFailed("NodeId is null");
      return false;
    }
    nodeId = attribute.getValue();
    node = getEngine().getCluster().getNodeMap().get(nodeId);
    if (node == null) {
      markFailed("Node is null");
      return false;
    }
    if ( checkIfAgentPresent && !node.isAgentPresent()) {
      markFailed("No agent present");
      return false;
    }
    return true;
  }

  protected void postActionValidation() throws Exception {
    int timeout = getTimeout();
    int countdown = timeout;
    long lastTime = System.currentTimeMillis();
    while(countdown > 0) {
      if(getEngine().getCluster().clusterHealthy()){
        checkAndWaitForNodeToRecover();
        this.getResult().setOut(this.getResult().getOut() + "\nService Health Check Passed");
        markSucceeded();
        return;
      }
      long now = System.currentTimeMillis();
      countdown -= (now - lastTime)/1000;
      lastTime = now;
      Thread.sleep(1000);
    }
    markFailed("Cluster is not healthy after " + timeout + " seconds");
    return;
  }

  public void checkAndWaitForNodeToRecover() throws TimeoutException {
    long completeTime = System.currentTimeMillis();
    int timeout = 60_000;
    int healthCheckInterval = 10_000;
    if (containsAttribute(RECOVERY_TIMEOUT)) {
      timeout = getAttribute(RECOVERY_TIMEOUT).getValue();
    }
    if (containsAttribute(POSTHEALTHCHECK_INTERVAL)) {
      healthCheckInterval = getAttribute(POSTHEALTHCHECK_INTERVAL).getValue();
    }
    int times = 0;
    long checkTimestamp = System.currentTimeMillis();
    while (true) {
      try {
        if (isServiceHealthy(checkTimestamp)) {
          break;
        }
      } catch (Exception e1) {
        getResult().appendErr("\n" + e1);
      }
      checkTimestamp = System.currentTimeMillis();
      if (times++ % 10 == 0) {
        getResult().appendOut("Action completed, waiting for healthcheck to pass (" + times + " times) \n");
      }
      if ((System.currentTimeMillis() - completeTime) >= timeout) {
        // if we are not getting service to recover in a reasonable amount of time then
        // fire an alert to pager human operator
        getResult().appendErr("Healthcheck timed out, node didn't recover in " + timeout + " ms");
        throw new TimeoutException();
      }
      // wait to re-check service status
      try {
        Thread.sleep(healthCheckInterval);
      } catch (InterruptedException e) {
        break;
      }
    }
  }

  public boolean isServiceHealthy(long lastCheckTimestamp) {
    return node.isServiceHealthy(lastCheckTimestamp);
  }

  protected NodeCmd runNodeAction(int timeout) throws Exception {
    return null;
  }

  public static String hostnameToString(Node node, Action action) {
    return " on " + ((node != null && node.getCurrentNodeInfo() != null
        && node.getCurrentNodeInfo().getHostname() != null)
            ? node.getCurrentNodeInfo().getHostname().split("\\.", 2)[0]
            : action.containsAttribute(OrionConstants.NODE_ID)
                ? action.getAttribute(OrionConstants.NODE_ID).getValue().toString()
                : " N/A");
  }
}
