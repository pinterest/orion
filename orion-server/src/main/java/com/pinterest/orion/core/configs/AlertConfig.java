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
