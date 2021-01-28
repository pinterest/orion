package com.pinterest.orion.core.global.sensor;

import java.util.Map;

import com.pinterest.orion.core.Context;
import com.pinterest.orion.core.Plugin;
import com.pinterest.orion.core.PluginConfigurationException;

public abstract class GlobalSensor extends Context implements Plugin, Runnable {

  public void run() {
    try {
      observe();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    setAttribute("_configs", config);
    initializeSensor(config);
  }

  public abstract void initializeSensor(Map<String, Object> config) throws PluginConfigurationException;

  public abstract void observe() throws Exception;

  public abstract int getInterval();

}
