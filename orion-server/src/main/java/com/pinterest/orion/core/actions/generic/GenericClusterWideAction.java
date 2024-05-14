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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.ActionEngine;
import com.pinterest.orion.core.actions.alert.AlertLevel;
import com.pinterest.orion.core.actions.alert.AlertMessage;
import com.pinterest.orion.core.actions.aws.RebootEC2InstanceAction;
import com.pinterest.orion.core.actions.aws.ReplaceEC2InstanceAction;
import com.pinterest.orion.core.actions.schema.AttributeSchema;
import com.pinterest.orion.core.actions.schema.TextValue;
import com.pinterest.orion.utils.OrionConstants;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.InstanceType;

public class GenericClusterWideAction {

  private GenericClusterWideAction() {
  }

  public static abstract class ClusterAction extends Action {

    public boolean failIfNoNodeIds() {
      return false;
    }

    protected Collection<String> getActionNodeIds(Cluster cluster) throws Exception {
      List<String> actionNodes;
      if (containsAttribute(OrionConstants.NODE_IDS)) {
        List<String> nodeIds = getAttribute(OrionConstants.NODE_IDS).getValue();
        for (String id : nodeIds) {
          if (!cluster.getNodeMap().containsKey(id)) {
            throw new Exception("Failed to find node " + id + " in cluster " + cluster.getClusterId());
          }
        }
        actionNodes = new ArrayList<>(nodeIds);
      } else if(failIfNoNodeIds()) {
        markFailed("No nodes provided in attribute");
        return null;
      } else {
        actionNodes = cluster.getNodeMap().values().stream()
            .map(n -> {
              if (n.getAgentNodeInfo() != null) return n.getAgentNodeInfo().getNodeId();
              else if (n.getCurrentNodeInfo() != null) return n.getCurrentNodeInfo().getNodeId();
              else return "n/a";
            })
            .collect(Collectors.toList());
      }
      return actionNodes;
    }

  }

  public static class ClusterDemoAction extends ClusterAction {

    @Override
    public void runAction() throws InterruptedException {
      if (!containsAttribute(OrionConstants.NODE_ID)) {
        markFailed("NodeId not provided in attributes");
        return;
      }
      String nodeId = getAttribute(this, OrionConstants.NODE_ID).getValue();
      List<Action> children = getChildren();
      int numTestChildrenActions = 5;
      for (int i = 0; i < numTestChildrenActions; i++) {
        Action a = new DemoSlackAction();
        a.setAttribute(OrionConstants.NODE_ID, nodeId);
        children.add(a);
        Thread.sleep(2000); // for testing longer periods between children create time
      }
      for (Action childAction : children) {
        // dispatch 5 children for testing cluster action
        try {
          getEngine().dispatchChild(this, childAction);
          childAction.get();
        } catch (Exception e) {
          markFailed(e);
        }
        if (childAction.isSuccess()) {
          markSucceeded();
        } else {
          markFailed("Child action failed");
        }
      }
    }

    @Override
    public String getName() {
      return "ClusterDemoAction";
    }
  }

  public static class ParallelDemoAction extends ParallelAction {

    public ParallelDemoAction() {
      super("DemoAction");
    }

    @Override
    public Action getChildAction() {
      return new DemoAction();
    }

    @Override
    public boolean isCancelAllIfFailed() {
      return false;
    }
  }

  public static class ClusterRestartAction extends RollingAction {

    public ClusterRestartAction() {
      super("Restart");
    }

    @Override
    public Action getChildAction() {
      return new GenericActions.ServiceRestartAction();
    }

    @Override
    public boolean isCancelAllIfFailed() {
      return true;
    }

  }

  public static class ClusterConfigUpdateAction extends RollingAction {

    public ClusterConfigUpdateAction() {
      super("Config Update");
    }

    @Override
    public Action getChildAction() {
      return new GenericActions.ServiceConfigUpdateAction();
    }

    @Override
    public boolean isCancelAllIfFailed() {
      return false;
    }

  }

  public static class RollingRebootAction extends RollingAction {

    public RollingRebootAction() {
      super("Reboot");
    }

    @Override
    public Action getChildAction() {
      return new RebootEC2InstanceAction();
    }

    @Override
    public boolean isCancelAllIfFailed() {
      return true;
    }

  }

  public static class RollingReplacementAction extends RollingAction {
    public RollingReplacementAction() {
      super("Replace");
    }

    @Override
    public boolean validateAttributes() {
      if (containsAttribute(ReplaceEC2InstanceAction.ATTR_AMI_KEY)) {
        try (Ec2Client ec2 = Ec2Client.create()) {
          String amiId = getAttribute(ReplaceEC2InstanceAction.ATTR_AMI_KEY).getValue();
          try {
            DescribeImagesResponse resp = ec2
                .describeImages(DescribeImagesRequest.builder().imageIds(amiId).build());
            if (!resp.hasImages()) {
              markFailed("AMI " + amiId + " does not exist");
              return false;
            }
          } catch (Exception e) {
            markFailed("Failed to fetch AMI from AWS: " + e);
            return false;
          }
        }
      }
      if (containsAttribute(ReplaceEC2InstanceAction.ATTR_INSTANCE_TYPE_KEY)) {
        String instanceTypeStr = getAttribute(ReplaceEC2InstanceAction.ATTR_INSTANCE_TYPE_KEY)
            .getValue();
        if (InstanceType.fromValue(instanceTypeStr).equals(InstanceType.UNKNOWN_TO_SDK_VERSION)) {
          markFailed("Unknown instance type provided: " + instanceTypeStr);
          return false;
        }
      }
      return true;
    }

    @Override
    public Action getChildAction() {
      Action childAction = new ReplaceEC2InstanceAction();
      return childAction;
    }

    @Override
    public boolean isCancelAllIfFailed() {
      return true;
    }
  }

  public static abstract class RollingAction extends ClusterAction {

    private static final String ATTR_NODE_IDS = "nodeIds";
    private String actionName;

    public RollingAction(String actionName) {
      this.actionName = actionName;
    }

    @Override
    public String getName() {
      String name = "Rolling-" + actionName;
      if (containsAttribute(ATTR_NODE_IDS)) {
        List<String> nodeIds = getAttribute(ATTR_NODE_IDS).getValue();
        name += " on " + nodeIds.size() + " nodes";
      }
      return name;
    }

    @Override
    public void initialize(Map<String, Object> config) throws PluginConfigurationException {
      super.initialize(config);
    }

    public boolean validateAttributes() {
      return true;
    }

    @Override
    public void runAction() throws Exception {
      if (!validateAttributes()) {
        return;
      }
      ActionEngine engine = getEngine();
      Cluster cluster = engine.getCluster();
      // get nodes
      List<Action> children = getChildren();
      Collection<String> nodeIds;
      nodeIds = getActionNodeIds(cluster);
      if (nodeIds == null) {
        return;
      }
      for (String nodeId : nodeIds) {
        Action childAction = getChildAction();
        childAction.copyAttributesFrom(this);
        childAction.setAttribute(OrionConstants.NODE_ID, nodeId);
        children.add(childAction);
      }
      AlertMessage message = null;
      if (!isCancelled()) {
        try {
          synchronized (engine.getCluster()) {
            for (int i = 0; i < children.size(); i++) {
              Action action = children.get(i);
              if (isCancelled()) {
                throw new Exception("Operation cancelled");
              }
              getEngine().dispatchChild(this, action);
              action.get();
              if (!action.isSuccess()) {
                throw new Exception("Child action:" + action.getName() + " with UUID:"
                    + action.getUuid() + " failed");
              }
            }
            message = new AlertMessage(getName() + " completed on cluster " + cluster.getName(), "",
                this.getOwner());
            markSucceeded();
          }
        } catch (Exception e) {
          message = new AlertMessage(getName() + " failed due to " + e.getMessage(), "",
              this.getOwner());
          markFailed(e);
        }
      } else {
        for (Action childAction : children) {
          childAction.cancel(true);
        }
      }
      if (message != null) {
        try {
          getEngine().alert(AlertLevel.MEDIUM, message);
        } catch (Exception e) {
          getResult().appendErr("\nFailed to send alert");
        }
      }
    }

    public abstract Action getChildAction();

    public abstract boolean isCancelAllIfFailed();

    @Override
    public AttributeSchema generateSchema(Map<String, Object> config) {
      return new AttributeSchema()
          .addValue(new TextValue(OrionConstants.NODE_IDS, "Node IDs", true))
          .addSchema(getChildAction().generateSchema(config));
    }
  }

  public static abstract class ParallelAction extends ClusterAction {

    private String actionName;

    public ParallelAction(String actionName) {
      this.actionName = actionName;
    }

    @Override
    public String getName() {
      String name = "Parallel-" + actionName;
      if (containsAttribute(OrionConstants.NODE_IDS)) {
        List<String> nodeIds = getAttribute(OrionConstants.NODE_IDS).getValue();
        name += " on " + nodeIds.size() + " nodes";
      }
      return name;
    }

    @Override
    public void initialize(Map<String, Object> config) throws PluginConfigurationException {
      super.initialize(config);
    }

    @Override
    public void runAction() throws Exception {
      ActionEngine engine = getEngine();
      Cluster cluster = engine.getCluster();
      // get nodes
      List<Action> children = getChildren();

      Collection<String> nodeIds;
      nodeIds = getActionNodeIds(cluster);
      if (nodeIds == null) {
        return;
      }
      for (String nodeId : nodeIds) {
        Action childAction = getChildAction();
        childAction.copyAttributesFrom(this);
        childAction.setAttribute(OrionConstants.NODE_ID, nodeId);
        children.add(childAction);
      }
      AlertMessage message = null;
      if (!isCancelled()) {
        try {
          synchronized (engine.getCluster()) {
            for (int i = 0; i < children.size(); i++) {
              Action action = children.get(i);
              getEngine().dispatchChild(this, action);
            }
            for (int i = 0; i < children.size(); i++) {
              Action action = children.get(i);
              action.get();
              if (!action.isSuccess()) {
                throw new Exception("Child action:" + action.getName() + " with UUID:"
                    + action.getUuid() + " failed");
              }
            }
            message = new AlertMessage(getName() + " completed on cluster " + cluster.getName(), "",
                this.getOwner());
            markSucceeded();
          }
        } catch (Exception e) {
          message = new AlertMessage(getName() + " failed due to " + e.getMessage(), "",
              this.getOwner());
          markFailed(e);
        }
      } else {
        for (Action childAction : children) {
          childAction.cancel(true);
        }
      }
      if (message != null) {
        try {
          getEngine().alert(AlertLevel.MEDIUM, message);
        } catch (Exception e) {
          getResult().appendErr("\nFailed to send alert");
        }
      }
    }

    public abstract Action getChildAction();

    public abstract boolean isCancelAllIfFailed();

    @Override
    public AttributeSchema generateSchema(Map<String, Object> config) {
      return new AttributeSchema()
          .addValue(new TextValue(OrionConstants.NODE_IDS, "Node IDs", true))
          .addSchema(getChildAction().generateSchema(config));
    }

  }

  public static class ClusterUpgradeAction extends RollingAction {

    public ClusterUpgradeAction() {
      super("Upgrade");
    }

    @Override
    public Action getChildAction() {
      return new GenericActions.ServiceUpgradeAction();
    }

    @Override
    public boolean isCancelAllIfFailed() {
      return true;
    }
  }

  public static class ParallelClusterStartAction extends ParallelAction {

    public ParallelClusterStartAction() {
      super("Start");
    }

    @Override
    public Action getChildAction() {
      return new GenericActions.ServiceStartAction();
    }

    @Override
    public boolean isCancelAllIfFailed() {
      return true;
    }
  }

  public static class ParallelClusterStopAction extends ParallelAction {

    public ParallelClusterStopAction() {
      super("Stop");
    }

    @Override
    public Action getChildAction() {
      return new GenericActions.ServiceStopAction();
    }

    @Override
    public boolean isCancelAllIfFailed() {
      return true;
    }
  }

  public static class ParallelEnableNodeMaintenanceModeAction extends ParallelAction {
    public ParallelEnableNodeMaintenanceModeAction() {
      super("Enable Maintenance");
    }

    @Override
    public Action getChildAction() {
      return new NodeMaintenanceActions.EnableNodeMaintenanceAction();
    }

    @Override
    public boolean isCancelAllIfFailed() {
      return true;
    }
  }

  public static class ParallelDisableNodeMaintenanceModeAction extends ParallelAction {
    public ParallelDisableNodeMaintenanceModeAction() {
      super("Disable Maintenance");
    }

    @Override
    public Action getChildAction() {
      return new NodeMaintenanceActions.DisableNodeMaintenanceAction();
    }

    @Override
    public boolean isCancelAllIfFailed() {
      return true;
    }
  }

  public static class ParallelDecommissionNodeAction extends ParallelAction {
    public ParallelDecommissionNodeAction() {
      super("Decommission");
    }

    @Override
    public Action getChildAction() {
      return new NodeDecommissionAction();
    }

    @Override
    public boolean isCancelAllIfFailed() {
      return true;
    }

    @Override
    public boolean failIfNoNodeIds() {
      return true;
    }
  }

  public static class ParallelReplaceNodeAction extends ParallelAction {
    public ParallelReplaceNodeAction() {
      super("Replace");
    }

    @Override
    public Action getChildAction() {
      return new NodeAction() {
        @Override
        public String getName() {
          return "ReplaceNodeAction";
        }
      };
    }

    @Override
    public boolean isCancelAllIfFailed() {
      return true;
    }

    @Override
    public boolean failIfNoNodeIds() {
      return true;
    }
  }

  public static class ParallelRebootNodeAction extends ParallelAction {
    public ParallelRebootNodeAction() {
      super("Reboot");
    }

    @Override
    public Action getChildAction() {
      return new RebootEC2InstanceAction();
    }

    @Override
    public boolean isCancelAllIfFailed() {
      return true;
    }

    @Override
    public boolean failIfNoNodeIds() {
      return true;
    }
  }
}