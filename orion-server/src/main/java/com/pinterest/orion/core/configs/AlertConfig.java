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
package com.pinterest.orion.core.configs;

import com.pinterest.orion.core.actions.alert.AlertLevel;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.List;

public class AlertConfig extends PluginConfig {
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<AlertLevel> level;

  public void setLevel(List<AlertLevel> level) {
    this.level = level;
  }

  public List<AlertLevel> getLevel() {
    return level;
  }

  @Override
  public void merge(PluginConfig baseConfig) {
    super.merge(baseConfig);
    AlertConfig thatConfig = (AlertConfig) baseConfig;
    if (this.level == null) {
      this.level = thatConfig.level;
    }
  }
}
