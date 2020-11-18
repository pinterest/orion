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

import org.apache.curator.framework.CuratorFramework;
import org.apache.kafka.clients.admin.AdminClient;

import com.pinterest.orion.core.utils.kafka.CuratorClient;

public class PreferredLeaderElectionAction extends AbstractKafkaAction {

  private static final String PREFERRED_REPLICA_PATH = "/admin/preferred_replica_election";

  @Override
  public void run(String zkUrl, AdminClient adminClient) {
    Boolean waitForPrevious = false;
    if (containsAttribute("wait_for_previous")) {
      waitForPrevious = getAttribute("wait_for_previous").getValue();
    }
    try (CuratorFramework zkClient = CuratorClient.buildAndGetZkClient(zkUrl)){
      zkClient.start();
      zkClient.blockUntilConnected();

      if (zkClient.checkExists().forPath(PREFERRED_REPLICA_PATH) != null && !waitForPrevious) {
        markFailed("Reassignment already running");
        return;
      } else {
        CuratorClient.waitForZnodeToBeDeleted(zkClient, PREFERRED_REPLICA_PATH);
      }
      zkClient.create().forPath(PREFERRED_REPLICA_PATH);
      CuratorClient.waitForZnodeToBeDeleted(zkClient, PREFERRED_REPLICA_PATH);
      markSucceeded();
    } catch (Exception e) {
      markFailed(e);
    }
  }

  @Override
  public String getName() {
    return "Preferred Leader Election";
  }

}
