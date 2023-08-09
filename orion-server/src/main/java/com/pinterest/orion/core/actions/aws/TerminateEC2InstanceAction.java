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

import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.actions.alert.AlertLevel;
import com.pinterest.orion.core.actions.alert.AlertMessage;
import com.pinterest.orion.core.actions.generic.NodeAction;
import com.pinterest.orion.server.OrionServer;
import com.pinterest.orion.utils.OrionConstants;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusResponse;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.InstanceStatus;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesResponse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class TerminateEC2InstanceAction extends NodeAction {
  public static final String ATTR_CAUSE_KEY = "cause";
  private static Set<InstanceStateName> terminatedStates = new HashSet<>(
      Arrays.asList(InstanceStateName.STOPPED, InstanceStateName.TERMINATED));

  private String hostname;
  private String instanceId;

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    super.initialize(config);
  }

  @Override
  public void runAction() {
    // Used to record the reason of replacement
    if (containsAttribute(ATTR_CAUSE_KEY)) {
      this.getResult()
          .appendOut("Cause of termination: " + getAttribute(ATTR_CAUSE_KEY).getValue() + "\n");
    }
    if (!initializeNode(false)) {
      return;
    }

    try (Ec2Client ec2Client = Ec2Client.create()) {
      InstanceStateName state;
      try {
        state = getInstanceState(ec2Client);
      } catch (Exception e) {
        // alert if the instance state / run instance request cannot be populated
        logger().log(Level.SEVERE, "Failed to get state and/or info of broker " + hostname, e);
        AlertMessage msg = new AlertMessage(
            "Failed to get state and/or info of broker " + hostname + " during broker termination",
            e.getMessage(),
            "orion"
        );
        getEngine().alert(AlertLevel.MEDIUM, msg);
        getEngine().alert(AlertLevel.HIGH, msg);
        OrionServer.metricsGaugeOne(
                "broker.waitingtermination.getstate.error",
                new HashMap<String, String>() {{
                  put("hostname", hostname);
                  put("instanceId", instanceId);
                }}
        );
        markFailed(e);
        return;
      }
      // only try to terminate the host if the host is in running state
      if (state.equals(InstanceStateName.RUNNING)) {
        getResult().appendOut("Terminating instance...");
        if (!terminateInstanceAndWaitUntilCompleted(instanceId, ec2Client)) {
          return;
        }
        getResult().appendOut("Instance terminated.");
      } else {
        getResult().appendOut("Skipping termination since host is not in RUNNING state.");
      }

      markSucceeded();
    }
  }

  protected boolean terminateInstanceAndWaitUntilCompleted(String instanceId, Ec2Client ec2Client) {
    getResult().appendOut("Attempting to terminate the host");
    // termination
    TerminateInstancesRequest terminateInstancesRequest = TerminateInstancesRequest.builder()
        .instanceIds(instanceId).build();
    try {
      TerminateInstancesResponse terminateInstancesResponse = ec2Client
          .terminateInstances(terminateInstancesRequest);
      if (!terminateInstancesResponse.hasTerminatingInstances()) {
        markFailed("Could not terminate the instance");
        return false;
      }
    } catch (Exception e) {
      markFailed("Could not terminate instance: " + e);
      return false;
    }

    getResult().appendOut("Terminate instance issued");
    try {
      waitForInstanceStateChange(ec2Client, instanceId, terminatedStates);
    } catch (InterruptedException e) {
      markFailed(getName() + " has been interrupted.");
      return false;
    }
    return true;
  }

  protected InstanceStateName getInstanceStatus(Ec2Client client, String instanceId) throws Exception {
    DescribeInstanceStatusRequest statusRequest = DescribeInstanceStatusRequest.builder().instanceIds(instanceId).build();
    DescribeInstanceStatusResponse statusResponse = client.describeInstanceStatus(statusRequest);
    for (InstanceStatus status : statusResponse.instanceStatuses()) {
      if (status.instanceId().equals(instanceId)) {
        return status.instanceState().name();
      }
    }
    throw new Exception("Failed to find instance " + instanceId);
  }

  private void waitForInstanceStateChange(Ec2Client client,
                                          String instanceId,
                                          Set<InstanceStateName> targetStates) throws InterruptedException {
    DescribeInstanceStatusRequest instanceStatusRequest = DescribeInstanceStatusRequest.builder()
        .instanceIds(instanceId).includeAllInstances(true).build();
    int retries = 3;
    while (true) {
      Thread.sleep(5000);
      try {
        DescribeInstanceStatusResponse resp = client.describeInstanceStatus(instanceStatusRequest);
        if (resp.hasInstanceStatuses()) {
          InstanceStateName instanceState = resp.instanceStatuses().get(0).instanceState().name();
          if (targetStates.contains(instanceState)) {
            break;
          }
        }
      } catch (Exception e) {
        if (retries > 0) {
          logger().log(Level.WARNING, "Exception happened when waiting for instance status, retrying for " + retries + " times: ", e);
        } else {
          throw e;
        }
        retries--;
      }
    }
  }

  @Override
  public String getName() {
    return "TerminateEC2Node " + getHostname() + " (" + getInstanceId() + ")";
  }

  protected InstanceStateName getInstanceState(Ec2Client ec2Client) throws Exception {
    // fetch instance info from EC2
    String hostname = null;
    // get hostname from agent node info if it exists
    if (node.getAgentNodeInfo() != null) {
      hostname = node.getAgentNodeInfo().getHostname();
    }
    // fallback to current node info if hostname not in agent node info
    if (hostname == null) {
      hostname = node.getCurrentNodeInfo().getHostname();
    }
    setHostname(hostname);
    Map<String, String> env = node.getAgentNodeInfo().getEnvironment();
    if (!env.containsKey(OrionConstants.INSTANCE_ID)) {
      throw new Exception("Failed to get instance ID for " + hostname + " from agent node info");
    } else {
      String instanceId = env.get(OrionConstants.INSTANCE_ID);
      setInstanceId(instanceId);
      return getInstanceStatus(ec2Client, instanceId);
    }
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }
}
