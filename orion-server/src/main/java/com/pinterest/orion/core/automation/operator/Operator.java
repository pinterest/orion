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
package com.pinterest.orion.core.automation.operator;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.Plugin;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.ActionDispatcher;

public abstract class Operator implements Plugin {

  private ActionDispatcher dispatcher;
  private String message;

  public abstract void operate(Cluster cluster) throws Exception;

  public final void setDispatcher(ActionDispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

  public final void dispatch(Action action) throws Exception {
    dispatcher.dispatch(action);
  }

  @JsonIgnore
  public void setMessage(String message) {
    this.message = message;
  }

  @JsonIgnore
  public String getMessage() {
    return message;
  }

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
  }
  
}
