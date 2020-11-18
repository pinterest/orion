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
package com.pinterest.orion.core;


import java.util.Map;

/**
 * Plugin is the base class of all DoctorKafka plugins.
 * All configuration will be applied to the plugins right after the plugin has been initialized
 */

public interface Plugin {

  /**
   * Initialize the plugin instance.
   * @param config
   * @throws PluginConfigurationException
   */
  void initialize(Map<String, Object> config) throws PluginConfigurationException;


  /**
   *
   * @return the display name of the plugin
   */
  String getName();
}
