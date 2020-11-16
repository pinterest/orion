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
package com.pinterest.orion.core.actions;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.hbase.HBaseCluster;

public class ScriptedAction extends Action {

  private String scriptname = "";
  private ScriptEngine scriptEngine;
  private File scriptPath;

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    super.initialize(config);
    if (!config.containsKey("scriptname")) {
      throw new PluginConfigurationException("scriptname is required for ScriptedSensors");
    }
    scriptname = config.get("scriptname").toString();
    if (!config.containsKey("scriptengine")) {
      throw new PluginConfigurationException("scriptengine must be specified");
    }
    String scriptEngineConfig = config.get("scriptengine").toString();
    scriptEngine = new ScriptEngineManager().getEngineByName(scriptEngineConfig);

    if (!config.containsKey("scriptpath")) {
      throw new PluginConfigurationException("Missing script path");
    }
    scriptPath = new File(config.get("scriptpath").toString());
    if (!scriptPath.exists()) {
      throw new PluginConfigurationException(
          "Script path:'" + scriptPath.getAbsolutePath() + "' is invalid");
    }
  }

  @Override
  public String getName() {
    return scriptname;
  }

  @Override
  public void runAction() throws Exception {
    Bindings binding = scriptEngine.createBindings();
    binding.put("actionEngine", getEngine());
    binding.put("engine", getEngine());
    binding.put("wrapperAction", this);
    scriptEngine.eval(new FileReader(scriptPath), binding);
    scriptEngine.eval("action.runAction()", binding);
  }

  @Override
  public Type getActionType() {
    return Type.CLUSTER;
  }

  public static void main(String[] args) throws Exception {
    ScriptedAction scriptedSensor = new ScriptedAction();
    Map<String, Object> config = new HashMap<>();
    config.put("scriptname", "test");
    config.put("scriptengine", "jython");
    config.put("scriptpath", "src/main/python/actions/hbase/simple_example.py");
    scriptedSensor.initialize(config);
    scriptedSensor.setEngine(new ActionEngine(new HBaseCluster("test", "test",
        Arrays.asList(), Arrays.asList(), null, null, null, null), null, null));
    scriptedSensor.runAction();
  }

}
