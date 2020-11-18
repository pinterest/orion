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

public class ServiceStabilityCheckAction extends NodeAction {

  public static final String ATTR_DURATION_SECONDS_KEY = "duration";
  private long duration = 300_000; // 5 minutes

  @Override
  public void runAction() throws Exception {
    if (containsAttribute(ATTR_DURATION_SECONDS_KEY)) {
      duration = getAttribute(ATTR_DURATION_SECONDS_KEY).getValue();
      duration *= 1000;
    }

    long now = System.currentTimeMillis();
    long prevUptime = 0;
    while (true) {
      if (!initializeNode(true)) {
        markFailed("Agent doesn't exist, host is likely down");
        return;
      }
      long curUptime = node.serviceStatus().getUptime();
      if (curUptime < prevUptime) { // includes -1 cases
        markFailed("Agent reported service uptime is smaller than previous reported uptime,"
            + "service likely failing - prev uptime: " + prevUptime + " current uptime: "
            + curUptime);
        return;
      }
      prevUptime = curUptime;
      long time = System.currentTimeMillis();
      if (time - now > duration) {
        getResult().appendOut("Service has been stable for " + duration + " seconds");
        break;
      }

      getResult().appendOut(
          "Node is currently stable after " + (time - now) + " seconds. Still waiting for " + (
              duration - (time - now)) + " seconds.");
      Thread.sleep(30_000); // might want to make it configurable
    }
    markSucceeded();
  }

  @Override
  public String getName() {
    return "ServiceStabilityCheck";
  }
}
