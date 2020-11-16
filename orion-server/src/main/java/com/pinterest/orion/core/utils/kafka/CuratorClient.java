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
package com.pinterest.orion.core.utils.kafka;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.data.Stat;

public class CuratorClient {

  public static CuratorFramework buildAndGetZkClient(String zkUrl) {
    return CuratorFrameworkFactory.newClient(zkUrl, new ExponentialBackoffRetry(1000, 3));
  }

  public static void waitForZnodeToBeDeleted(CuratorFramework zkClient, String reassignmentPath) throws Exception {
    while (true) {
      Thread.sleep(1000);
      Stat forPath = zkClient.checkExists().forPath(reassignmentPath);
      if (forPath == null) {
        break;
      }
    }
  }
}
