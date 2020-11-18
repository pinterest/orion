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
package com.pinterest.orion.core.actions.kafka;

import com.pinterest.orion.core.Node;
import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.aws.TerminateEC2InstanceAction;
import com.pinterest.orion.core.actions.generic.NodeDecommissionAction;
import com.pinterest.orion.core.automation.sensor.kafka.KafkaTopicSensor;
import com.pinterest.orion.core.kafka.KafkaBroker;
import com.pinterest.orion.utils.OrionConstants;

public class EC2BrokerDecommissionAction extends NodeDecommissionAction {

  @Override
  public boolean checkIfDecommissionable(Node node) {

    if (node.getCluster().containsAttribute(KafkaTopicSensor.ATTR_TOPICINFO_MAP_KEY)) {
      KafkaBroker broker = (KafkaBroker) node;
      return broker.getTopicPartitionsForNode().isEmpty();
    } else {
      getResult().appendErr("Failed to get topic info, server might be still initializing");
      return false;
    }
  }

  @Override
  public boolean decommission(Node node) throws Exception {
    Action terminationAction = new TerminateEC2InstanceAction();
    terminationAction.setAttribute(TerminateEC2InstanceAction.ATTR_CAUSE_KEY, "Decommissioned");
    terminationAction.setAttribute(OrionConstants.NODE_ID, nodeId);
    this.getChildren().add(terminationAction);
    getEngine().dispatchChild(this, terminationAction);
    terminationAction.get();
    if (!terminationAction.isSuccess()) {
      markFailed("Failed to terminate EC2 host " + nodeId);
      return false;
    }

    getResult().appendOut("Terminated host " + nodeId);
    return super.decommission(node);
  }

  @Override
  public String getName() {
    return "EC2BrokerDecommissionAction";
  }
}
