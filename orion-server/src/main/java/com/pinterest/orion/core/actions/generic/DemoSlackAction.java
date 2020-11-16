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
import com.pinterest.orion.core.actions.alert.SlackAlertAction;

public class DemoSlackAction extends Action {

  @Override
  public String getName() {
    return "DemoSlack";
  }

  @Override
  public void runAction() throws Exception {
    Cluster cluster = getEngine().getCluster();
    SlackAlertAction alertAction = new SlackAlertAction();
    AlertMessage message = new AlertMessage(
        getName() + " completed on cluster " + cluster.getName(), "Action triggered ",
        this.getOwner());
    try {
      getEngine().alert(message, alertAction);
      markSucceeded();
    } catch (Exception e) {
      markFailed("Failed to send slack message");
      e.printStackTrace();
      throw e;
    }
  }

  @Override
  public Type getActionType() {
    return Type.CLUSTER;
  }

//  @Override
//  protected NodeCmd runNodeAction() {
//    System.out.println("\nRunning: Demo: " + getUuid());
//    try {
//      try {
//        System.out.println("Agent status:" + node.agentStatus());
//      } catch (Exception e) {
//        // TODO Auto-generated catch block
//        e.printStackTrace();
//      }
//      Thread.sleep(1000 * 10);
//      if (counter % 2 == 0) {
//        AlertAction alert = new DemoAlertAction();
//        AlertMessage alertMessage = new AlertMessage("Demo title", "Demo body");
//        this.getEngine().alert(alertMessage, alert);
//        markSucceeded();
//      } else {
//        markFailed("Action failed");
//      }
//      counter++;
//    } catch (InterruptedException e) {
//      markFailed(e);
//    }
//    System.out.println("\nCompleted: Demo: " + getUuid());
//  }

}