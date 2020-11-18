package com.pinterest.orion.core.configs;

public class ActionConfig extends PluginConfig {
  private EndpointConfig endpoint;

  public EndpointConfig getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(EndpointConfig endpoint) {
    this.endpoint = endpoint;
  }

  public boolean isEndpointEnabled() {
    return this.endpoint != null && this.endpoint.isEnabled();
  }

  @Override
  public void merge(PluginConfig baseConfig) {
    if (baseConfig instanceof ActionConfig) {
      ActionConfig thatConfig = (ActionConfig) baseConfig;
      super.merge(thatConfig);
      if (this.endpoint == null) {
        this.endpoint = thatConfig.endpoint;
      } else if (thatConfig.endpoint != null ){
        this.endpoint.merge(thatConfig.endpoint);
      }
    }
  }
}
