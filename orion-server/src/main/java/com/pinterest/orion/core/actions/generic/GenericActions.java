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


import com.pinterest.orion.common.CmdResult;
import com.pinterest.orion.common.NodeCmd;
import com.pinterest.orion.core.Attribute;
import com.pinterest.orion.utils.OrionConstants;

public class GenericActions {

  private GenericActions() {
  }

  public static class ServiceConfigUpdateAction extends NodeAction {
    @Override
    public String getName() {
      return "Service Config Update";
    }

    @Override
    public NodeCmd runNodeAction(int timeout) throws Exception {
      return node.updateConfigs(getUuidString(), timeout);
    }

    private static final String PID_EXIST_ERROR_MESSAGE = "The pwrapper script is currently running, aborted";

    @Override
    protected boolean isCmdSuccessful(CmdResult cmdResult) {
      if ( !cmdResult.hasCompleted() ) {
        return false;
      }
      if ( cmdResult.getExitCode() != 0 &&
           cmdResult.getErr() != null &&
           !cmdResult.getErr().startsWith(PID_EXIST_ERROR_MESSAGE)
      ) {
        return false;
      }

      return true;
    }
  }

  public static class ServiceRestartAction extends NodeAction {

    @Override
    public String getName() {
      return "Service Restart" + NodeAction.hostnameToString(node, this);
    }

    @Override
    public NodeCmd runNodeAction(int timeout) throws Exception {
      Attribute attribute = getAttribute(OrionConstants.NODE_ID);
      node = getEngine().getCluster().getNodeMap().get(attribute.getValue());
      return node.restartService(getUuidString(), timeout);
    }

    @Override
    public MaintenancePolicy configureMaintenancePolicy() {
      return MaintenancePolicy.ENABLE_AND_RESTORE_WHEN_COMPLETE;
    }
  }

  public static class ServiceStartAction extends NodeAction {

    @Override
    public String getName() {
      return "Service Start" + NodeAction.hostnameToString(node, this);
    }

    @Override
    public NodeCmd runNodeAction(int timeout) throws Exception {
      return node.startService(getUuidString(), timeout);
    }

    @Override
    protected void postActionValidation() throws Exception {
      // TODO service up logic check
      markSucceeded();
    }

    @Override
    public MaintenancePolicy configureMaintenancePolicy() {
      return MaintenancePolicy.DISABLE_AFTER_EXECUTION;
    }
  }

  public static class ServiceStopAction extends NodeAction {

    @Override
    public String getName() {
      return "Service Stop" + NodeAction.hostnameToString(node, this);
    }

    @Override
    public NodeCmd runNodeAction(int timeout) throws Exception {
      return node.stopService(getUuidString(), timeout);
    }
    
    @Override
    protected void postActionValidation() throws Exception {
      // TODO service down logic check
      markSucceeded();
    }

    @Override
    public MaintenancePolicy configureMaintenancePolicy() {
      return MaintenancePolicy.ENABLE_BEFORE_EXECUTION;
    }

  }

  public static class ServiceUpgradeAction extends NodeAction {
    @Override
    public String getName() {
      return "Service Upgrade " + hostnameToString(node, this);
    }

    @Override
    public void runAction() throws Exception {
      if(!initializeNode(true)){
        return;
      }
      checkAndWaitForNodeToRecover();
      String nodeId = node.getCurrentNodeInfo().getNodeId();

      int timeout = getTimeout();
      ServiceConfigUpdateAction serviceConfigUpdateAction = new ServiceConfigUpdateAction();
      serviceConfigUpdateAction.setEngine(getEngine());
      serviceConfigUpdateAction.copyAttributeFrom(this, OrionConstants.NODE_ID);
      serviceConfigUpdateAction.setAttribute(ATTR_ACTION_TIMEOUT, timeout);
      getChildren().add(serviceConfigUpdateAction);
      serviceConfigUpdateAction.run();
      if(!serviceConfigUpdateAction.isSuccess()) {
        markFailed("Failed to update configuration for node "+ nodeId +" : " + serviceConfigUpdateAction.getResult().getErr());
        return;
      }

      ServiceRestartAction serviceRestartAction = new ServiceRestartAction();
      serviceRestartAction.setEngine(getEngine());
      serviceRestartAction.copyAttributeFrom(this, OrionConstants.NODE_ID);
      serviceRestartAction.setAttribute(ATTR_ACTION_TIMEOUT, timeout);
      getChildren().add(serviceRestartAction);
      serviceRestartAction.run();
      if(!serviceRestartAction.isSuccess()) {
        markFailed("Failed to restart service on node "+ nodeId +" : " + serviceConfigUpdateAction.getResult().getErr());
        return;
      }
      postActionValidation();
    }

  }

}

