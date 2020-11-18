package com.pinterest.orion.core.configs;

public class EndpointConfig {
  private boolean enabled = false;
  private String displayName;
  private String label;
  private String icon;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public void merge(EndpointConfig baseConfig) {
    if (this.enabled) {
      if (this.displayName == null) {
        this.displayName = baseConfig.displayName;
      }
      if (this.label == null) {
        this.label = baseConfig.label;
      }
      if (this.icon == null) {
        this.icon = baseConfig.icon;
      }
    }
  }
}
