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

import com.pinterest.orion.core.Attribute;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.actions.alert.AlertLevel;
import com.pinterest.orion.core.actions.alert.AlertMessage;
import com.pinterest.orion.core.actions.generic.NodeAction;
import com.pinterest.orion.core.actions.schema.AttributeSchema;
import com.pinterest.orion.core.actions.schema.TextEnumValue;
import com.pinterest.orion.core.actions.schema.TextValue;
import com.pinterest.orion.utils.OrionConstants;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.GroupIdentifier;
import software.amazon.awssdk.services.ec2.model.IamInstanceProfileSpecification;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesResponse;

import java.util.Base64;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ReplaceEC2InstanceAction extends NodeAction {
  public static final String ATTR_CAUSE_KEY = "cause";
  public static final String ATTR_AMI_KEY = "ami";
  public static final String ATTR_INSTANCE_TYPE_KEY = "instance_type";
  public static final String ATTR_SKIP_CLUSTER_HEALTH_CHECK_KEY = "skip_cluster_health_check";
  public static final String ATTR_NODE_EXISTS_KEY = "node_exists";
  public static final String ATTR_HOSTNAME_KEY = "hostname";
  private String confRoute53ZoneId;
  private String confRoute53Name;

  private String hostname;
  private String instanceId;
  protected boolean skipClusterHealthCheck = false;

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    super.initialize(config);
    if (!config.containsKey(Ec2Utils.CONF_ROUTE53_ZONE_ID)) {
      throw new PluginConfigurationException(
          "Cannot find key " + Ec2Utils.CONF_ROUTE53_ZONE_ID + " in config of " + getName());
    }
    if (!config.containsKey(Ec2Utils.CONF_ROUTE53_ZONE_NAME)) {
      throw new PluginConfigurationException(
          "Cannot find key " + Ec2Utils.CONF_ROUTE53_ZONE_NAME + " in config " + getName());
    }
    confRoute53ZoneId = config.get(Ec2Utils.CONF_ROUTE53_ZONE_ID).toString();
    confRoute53Name = config.get(Ec2Utils.CONF_ROUTE53_ZONE_NAME).toString();
    setAttribute(RECOVERY_TIMEOUT, 3600_000);
  }

  @Override
  public void runAction() {
    boolean nodeExists = true; // indicate whether the node exists in the cluster's nodeMap
    String privateIpAddress;

    // if node is not in nodeMap, skip all maintenance mode related stuff
    // also get the hostname from attributes
    if (containsAttribute(ATTR_NODE_EXISTS_KEY)) {
      nodeExists = getAttribute(ATTR_NODE_EXISTS_KEY).getValue();
      if (!nodeExists && containsAttribute(ATTR_HOSTNAME_KEY)) {
        hostname = getAttribute(ATTR_HOSTNAME_KEY).getValue();
      }
    }

    // this is currently disabled for operators, should only be used for emergencies using manual actions
    if (containsAttribute(ATTR_SKIP_CLUSTER_HEALTH_CHECK_KEY)) {
      skipClusterHealthCheck = getAttribute(ATTR_SKIP_CLUSTER_HEALTH_CHECK_KEY).getValue();
    }

    // Used to record the reason of replacement
    if (containsAttribute(ATTR_CAUSE_KEY)) {
      this.getResult()
          .appendOut("Cause of replacement: " + getAttribute(ATTR_CAUSE_KEY).getValue() + "\n");
    }

    // put the node into maintenance mode if node exists in the clusterMap
    boolean prevMaintenanceMode = false;
    if (nodeExists) {
      if (!initializeNode(false)) {
        return;
      }
      prevMaintenanceMode = node.isUnderMaintenance();
      node.setMaintenance(true);
    }

      try (Ec2Client ec2Client = Ec2Client.create()) {
        InstanceStateAndRequest stateAndRequest;
        try {
          stateAndRequest = getInstanceStateAndRequest(ec2Client, nodeExists);
        } catch (Exception e) {
          // alert if the instance state / run instance request cannot be populated
          logger().log(Level.SEVERE, "Failed to get state and/or info of broker " + hostname, e);
          AlertMessage msg = new AlertMessage(
              "Failed to get state and/or info of broker " + hostname + " during broker replacement",
              e.getMessage(),
              "orion"
          );
          getEngine().alert(AlertLevel.MEDIUM, msg);
          getEngine().alert(AlertLevel.HIGH, msg);
          markFailed(e);
          return;
        }
        // only try to terminate the host if the host is in running state
        if (stateAndRequest.getInstanceStateName().equals(InstanceStateName.RUNNING)) {
          getResult().appendOut("Terminating instance...");
          if (!terminateInstanceAndWaitUntilCompleted(instanceId, ec2Client)) {
            return;
          }
          getResult().appendOut("Instance terminated.");
        } else {
          getResult().appendOut("Skipping termination since host is not in RUNNING state.");
        }

        // launch instance based on the request built
        privateIpAddress = launchInstanceAndReturnIPIfSuccessful(
            ec2Client,
            stateAndRequest.getRunInstancesRequest()
        );

        // privateIpAddress will be null if the launch failed
        if(privateIpAddress == null) {
          return;
        }
        getResult().appendOut("Instance healthcheck completed");
      }

      // update DNS record
      try {
        Ec2Utils
            .upsertR53Record(hostname, privateIpAddress, confRoute53Name, confRoute53ZoneId, logger());
      } catch (Exception e) {
        markFailed(e);
        return;
      }

      try {
        // this will check if there are any URPs, unless the emergency flag was enabled
        checkAndWaitForNodeToRecover();
        markSucceeded();
      } catch (java.util.concurrent.TimeoutException e) {
        markFailed("Post validaton timed out for node replacement on "
            + hostname);
        getEngine().alert(AlertLevel.HIGH,
            new AlertMessage("Replacement error on " + hostname,
                "Post replacement health check timed out", getOwner(), hostname));
        return;
      }
      if (nodeExists) {
        node.setMaintenance(prevMaintenanceMode);
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
      logger().log(Level.SEVERE, "Failed to terminate instance", e);
      markFailed(e);
      return false;
    }

    getResult().appendOut("Terminate instance issued");
    try {
      Ec2Utils.waitForInstanceStateChange(ec2Client, instanceId, Ec2Utils.EC2_TERMIMATED_STATES, logger());
    } catch (InterruptedException e) {
      markFailed(getName() + " has been interrupted.");
      return false;
    }
    return true;
  }

  protected String launchInstanceAndReturnIPIfSuccessful(
      Ec2Client ec2Client,
      RunInstancesRequest runInstancesRequest
  ) {
    String newInstanceId;
    String privateIpAddress;
    RunInstancesResponse runInstancesResponse;
    try {
      runInstancesResponse = runInstance(ec2Client, runInstancesRequest);
    } catch (Exception e) {
      markFailed("Failed to request runInstances from EC2: " + e);
      return null;
    }

    Instance newInstance = runInstancesResponse.instances().get(0);
    newInstanceId = newInstance.instanceId();
    getResult().appendOut("New instanceId: " + newInstanceId);
    getResult().appendOut("Instance launched");

    try {
      Ec2Utils.waitForInstanceStateChange(ec2Client, newInstanceId, Ec2Utils.EC2_RUNNING_STATES, logger());
    } catch (InterruptedException e) {
      markFailed(getName() + " has been interrupted.");
      return null;
    }

    privateIpAddress = newInstance.privateIpAddress();
    return privateIpAddress;
  }

  protected RunInstancesResponse runInstance(Ec2Client ec2Client,
                                             RunInstancesRequest runInstancesRequest)
      throws Exception {
    RunInstancesResponse runInstancesResponse;
    try {
      runInstancesResponse = ec2Client.runInstances(runInstancesRequest);
    } catch (Ec2Exception ec2e) {
      // check if it's IP address conflict, retry without specifying the same address if necessary
      if (ec2e.awsErrorDetails().errorCode().equals("InvalidIPAddress.InUse")) {
        getResult().appendOut(
            "IP address already in use, will retry launching without specifying same address");
        logger().warning("IP conflict: " + ec2e + " , retry without using same address");
        runInstancesRequest = runInstancesRequest.toBuilder().privateIpAddress(null).build();
        runInstancesResponse = ec2Client.runInstances(runInstancesRequest);
      } else {
        throw ec2e;
      }
    }
    return runInstancesResponse;
  }

  @Override
  public boolean isServiceHealthy(long lastCheckTimestamp) {
    // node might be null if the node never existed in the cluster map, try to initialize the node before checking node health
    if (node == null) {
      Attribute attribute = getAttribute(OrionConstants.NODE_ID);
      if (attribute == null) {
        logger().warning("Missing node_id in attributes");
        return false;
      }
      String nodeId = attribute.getValue();
      node = getEngine().getCluster().getNodeMap().get(nodeId);
      if (node == null) {
        logger().warning("Failed to initialize node during node service health check");
        return false;
      }
    }
    return super.isServiceHealthy(lastCheckTimestamp) &&
        ( skipClusterHealthCheck || node.getCluster().clusterHealthy() );
  }

  protected Instance getAndValidateInstance(Ec2Client client, String instanceId) throws Exception {
    DescribeInstancesRequest instancesRequest = DescribeInstancesRequest.builder()
        .instanceIds(instanceId).build();
    DescribeInstancesResponse instancesResponse = client.describeInstances(instancesRequest);
    Instance ret = null;
    for (Instance instance : instancesResponse.reservations().get(0).instances()) {
      if (instance.instanceId().equals(instanceId)) {
        ret = instance;
      }
    }
    if (ret == null) {
      throw new Exception("Failed to find instance " + instanceId);
    } else if (!validateInstance(ret)) {
      throw new Exception("Invalid instance info from instance " + instanceId);
    }
    return ret;
  }

  @Override
  public String getName() {
    return "ReplaceEC2Node " + getHostname() + " (" + getInstanceId() + ")";
  }

  protected InstanceStateAndRequest getInstanceStateAndRequest(Ec2Client ec2Client, boolean nodeExists) throws Exception {
    // fetch instance info from EC2
    if(nodeExists) {
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
        String userdata = env.getOrDefault(OrionConstants.USERDATA, "");
        Instance victim = getAndValidateInstance(ec2Client, instanceId);
        // build launch instance request based on source of instance info
        RunInstancesRequest runInstancesRequest = getRunInstancesRequestFromInstance(userdata, victim);
        InstanceStateName instanceStateName = victim.state().name();
        return new InstanceStateAndRequest(instanceStateName, runInstancesRequest);
      }
    } else {
      throw new Exception("Host does not exist in cluster, cannot get host data");
    }
  }

  protected boolean validateInstance(Instance instance) {
    return instance.keyName() != null &&
        instance.securityGroups() != null &&
        instance.iamInstanceProfile() != null &&
        instance.iamInstanceProfile().arn() != null &&
        instance.placement() != null &&
        instance.subnetId() != null;
  }

  protected RunInstancesRequest getRunInstancesRequestFromInstance(String userdata,
                                                                 Instance victim) {
    String targetAmi = getTargetAmi(victim.imageId());
    InstanceType instanceType = getTargetInstanceType(victim.instanceType());

    RunInstancesRequest.Builder req =  RunInstancesRequest.builder()
        .imageId(targetAmi)
        .instanceType(instanceType)
        .keyName(victim.keyName())
        .securityGroupIds(victim.securityGroups().stream().map(GroupIdentifier::groupId)
            .collect(Collectors.toList()))
        .placement(victim.placement())
        .iamInstanceProfile(
            IamInstanceProfileSpecification.builder()
                .arn(victim.iamInstanceProfile().arn())
                .build()
        )
        .userData(Base64.getEncoder().encodeToString(userdata.getBytes()))
        .subnetId(victim.subnetId())
        .privateIpAddress(victim.privateIpAddress())
        .minCount(1)
        .maxCount(1);
    return req.build();
  }

  // override instance type if provided in attributes
  protected InstanceType getTargetInstanceType(InstanceType instanceType) {
    if (containsAttribute(ATTR_INSTANCE_TYPE_KEY)) {
      instanceType = InstanceType.fromValue(getAttribute(ATTR_INSTANCE_TYPE_KEY).getValue());
      logger().info(
          "Instance type override requested for:" + hostname);
    }
    return instanceType;
  }

  // override AMI if provided in attributes
  protected String getTargetAmi(String targetAmi) {
    if (containsAttribute(ATTR_AMI_KEY)) {
      targetAmi = getAttribute(ATTR_AMI_KEY).getValue();
      targetAmi = targetAmi.trim();
      logger().info("AMI override requested for:" + hostname);
    }
    return targetAmi;
  }

  protected static class InstanceStateAndRequest {
    private InstanceStateName instanceStateName;
    private RunInstancesRequest runInstancesRequest;

    public InstanceStateAndRequest(
        InstanceStateName instanceStateName,
        RunInstancesRequest runInstancesRequest) {
      this.instanceStateName = instanceStateName;
      this.runInstancesRequest = runInstancesRequest;
    }

    public InstanceStateName getInstanceStateName() {
      return instanceStateName;
    }

    public RunInstancesRequest getRunInstancesRequest() {
      return runInstancesRequest;
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

  @Override
  public AttributeSchema generateSchema(Map<String, Object> config) {
    return new AttributeSchema()
        .addValue(
            new TextEnumValue(ATTR_INSTANCE_TYPE_KEY, "Instance Type (optional, will inherit current type if not selected)", false)
              .addOption("i3.2xlarge", "i3.2xlarge")
              .addOption("i3.4xlarge", "i3.4xlarge")
              .addOption("i3.8xlarge", "i3.8xlarge")
              .addOption("d2.2xlarge", "d2.2xlarge")
        )
        .addValue(new TextValue(ATTR_AMI_KEY, "AMI id (optional, will inherit current AMI if not provided)", false))
        .addSchema(super.generateSchema(config));
  }
}
