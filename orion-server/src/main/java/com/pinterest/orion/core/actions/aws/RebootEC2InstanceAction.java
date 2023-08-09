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
package com.pinterest.orion.core.actions.aws;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.actions.alert.AlertLevel;
import com.pinterest.orion.core.actions.alert.AlertMessage;
import com.pinterest.orion.core.actions.generic.GenericActions;
import com.pinterest.orion.core.actions.generic.NodeAction;
import com.pinterest.orion.core.actions.generic.GenericActions.ServiceStopAction;
import com.pinterest.orion.server.OrionServer;
import com.pinterest.orion.utils.OrionConstants;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.RebootInstancesRequest;

public class RebootEC2InstanceAction extends NodeAction {

  private static Set<InstanceStateName> RUNNING_STATE = Collections
      .singleton(InstanceStateName.RUNNING);
  private String hostname;
  private String instanceId;

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    super.initialize(config);
    setAttribute(RECOVERY_TIMEOUT, 3600_000);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.pinterest.orion.core.actions.generic.NodeAction#runAction()
   */
  @Override
  public void runAction() throws Exception {
    if (!initializeNode(true)) {
      return;
    }
    boolean prevMaintenanceMode = false;
    prevMaintenanceMode = node.isUnderMaintenance();
    node.setMaintenance(true);
    String hostname = extractHostname();
    Map<String, String> env = node.getAgentNodeInfo().getEnvironment();
    if (!env.containsKey(OrionConstants.INSTANCE_ID)) {
      throw new Exception("Failed to get instance ID for " + hostname + " from agent node info");
    } else {
      instanceId = env.get(OrionConstants.INSTANCE_ID);
      try (Ec2Client ec2Client = Ec2Client.create()) {
        // try to gracefully shutdown service
        GenericActions.ServiceStopAction stopService = new ServiceStopAction();
        stopService.copyAttributeFrom(this, OrionConstants.NODE_ID);
        getEngine().dispatchChild(this, stopService);
        try {
          stopService.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
          getResult()
              .appendErr("Service stop failed, will now perform a hard reboot, error:"
                  + e.getMessage());
        }
        rebootInstance(ec2Client);
        // command ran successfully
        try {
          Ec2Utils.waitForInstanceStateChange(ec2Client, instanceId, RUNNING_STATE,
              logger());
        } catch (InterruptedException e) {
          markFailed(getName() + " has been interrupted.");
        }

        try {
          // this will check if there are any URPs, unless the emergency flag was enabled
          checkAndWaitForNodeToRecover();
          node.setMaintenance(prevMaintenanceMode);
          markSucceeded();
        } catch (java.util.concurrent.TimeoutException e) {
          markFailed("Post validaton timed out for node reboot on " + hostname);
          getEngine().alert(AlertLevel.HIGH,
              new AlertMessage("Replacement error on " + hostname,
                  "Post reboot of " + hostname + " health check timed out", getOwner(), hostname));
          OrionServer.metricsGaugeNum(
                  "broker.reboot.healthcheck.timeout", 1,
                  new HashMap<String, String>() {{
                    put("hostname", hostname);
                    put("instanceId", instanceId);
                  }}
          );
          return;
        }
      }
    }
  }

  private String extractHostname() {
    // get hostname from agent node info if it exists
    if (node.getAgentNodeInfo() != null) {
      hostname = node.getAgentNodeInfo().getHostname();
    }
    // fallback to current node info if hostname not in agent node info
    if (hostname == null) {
      hostname = node.getCurrentNodeInfo().getHostname();
    }
    return hostname;
  }

  private void rebootInstance(Ec2Client ec2Client) {
    RebootInstancesRequest request = RebootInstancesRequest.builder().instanceIds(instanceId)
        .build();
    ec2Client.rebootInstances(request);
  }

  @Override
  public String getName() {
    return "RebootEC2Node " + getHostname() + " (" + getInstanceId() + ")";
  }

  public String getInstanceId() {
    return instanceId;
  }

  public String getHostname() {
    return hostname;
  }

}