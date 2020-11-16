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

import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.actions.Action;

public class DemoAction extends Action {
  private static Logger logger = Logger.getLogger(DemoAction.class.getName());

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    super.initialize(config);
    logger.log(Level.INFO, "Config: " + config);
  }

  @Override
  public String getName() {
    return "DemoAction";
  }

  @Override
  public void runAction() throws Exception {
    int random = new Random().nextInt(30_000);
    getResult().appendOut("Random: " + random);
    Thread.sleep(5000 + random);

//    if (random > 25_000) {
//      markFailed("Failed");
//      return;
//    }
    markSucceeded();
  }

  @Override
  public Type getActionType() {
    return Type.CLUSTER;
  }

}