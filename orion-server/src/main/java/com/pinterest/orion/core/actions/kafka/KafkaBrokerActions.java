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

import com.pinterest.orion.common.NodeCmd;
import com.pinterest.orion.core.actions.generic.GenericActions;
import com.pinterest.orion.core.kafka.KafkaCluster;

public class KafkaBrokerActions {

  public static class KafkaBrokerRestartAction extends GenericActions.ServiceRestartAction {
    public static final String ATTR_ONLY_CHECK_BROKER_URPS_KEY = "only_check_node_urps";

    @Override
    public NodeCmd runNodeAction(int timeout) throws Exception {
      return super.runNodeAction(timeout);
    }

    public boolean isServiceHealthy(long lastCheckTimestamp) {
      boolean checkAgent = false;
      boolean onlyCheckBrokerURPs = false;
      if (containsAttribute("checkAgent")) {
        checkAgent = getAttribute("checkAgent").getValue();
      }
      if (containsAttribute(ATTR_ONLY_CHECK_BROKER_URPS_KEY)) {
        onlyCheckBrokerURPs = true;
      }

      boolean ret = true;
      if (checkAgent) {
        ret = super.isServiceHealthy(lastCheckTimestamp);
      }

      if (onlyCheckBrokerURPs) {
        ret &= ((KafkaCluster) node.getCluster()).brokerHealthy(nodeId);
      } else {
        ret &= node.getCluster().clusterHealthy();
      }
      return ret;
    }

  }

}