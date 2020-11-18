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

import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.alert.AlertMessage;
import com.pinterest.orion.core.actions.alert.SlackAlert;

public class DemoSlackAction extends Action {

  @Override
  public String getName() {
    return "DemoSlack";
  }

  @Override
  public void runAction() throws Exception {
    Cluster cluster = getEngine().getCluster();
    SlackAlert alert = new SlackAlert();
    AlertMessage message = new AlertMessage(
        getName() + " completed on cluster " + cluster.getName(), "Action triggered ",
        this.getOwner());
    try {
      getEngine().alert(alert, message);
      markSucceeded();
    } catch (Exception e) {
      markFailed("Failed to send slack message");
      e.printStackTrace();
      throw e;
    }
  }
}