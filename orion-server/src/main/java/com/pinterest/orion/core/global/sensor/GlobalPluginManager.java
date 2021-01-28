package com.pinterest.orion.core.global.sensor;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.configs.PluginConfig;

import io.dropwizard.lifecycle.Managed;

public class GlobalPluginManager implements Managed {

  private static final Logger logger = Logger
      .getLogger(GlobalPluginManager.class.getCanonicalName());
  private static final Map<String, GlobalSensor> MAP = new ConcurrentHashMap<>();
  private ScheduledExecutorService es = null;

  public void initialize(List<PluginConfig> globalSensorConfigs) throws PluginConfigurationException {
    if (globalSensorConfigs == null) {
      return;
    }
    es = Executors.newScheduledThreadPool(2);
    try {
      for (PluginConfig pluginConfig : globalSensorConfigs) {
        Class<? extends GlobalSensor> asSubclass = Class.forName(pluginConfig.getClazz())
            .asSubclass(GlobalSensor.class);
        GlobalSensor sensor = asSubclass.newInstance();
        sensor.initialize(pluginConfig.getConfiguration());
        MAP.put(sensor.getName(), sensor);
        logger.info("Initialzing plugin:" + sensor.getName());
      }
    } catch (Exception e) {
      throw new PluginConfigurationException(e);
    }
  }

  @Override
  public void start() throws Exception {
    for (Entry<String, GlobalSensor> entry : MAP.entrySet()) {
      GlobalSensor sensor = entry.getValue();
      es.scheduleAtFixedRate(sensor, sensor.getInterval(), sensor.getInterval(), TimeUnit.SECONDS);
      logger.info(
          "Scheduled sensor:" + sensor.getName() + " every:" + sensor.getInterval() + " seconds");
    }
  }

  @Override
  public void stop() throws Exception {
    if (es != null) {
      es.shutdownNow();
    }
  }

  public static GlobalSensor getSensorInstance(String sensorName) {
    return MAP.get(sensorName);
  }
}
