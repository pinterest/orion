package com.pinterest.orion.core.automation.sensor.yarn;

import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.automation.sensor.Sensor;
import com.pinterest.orion.core.yarn.YarnCluster;

public abstract class YarnSensor extends Sensor {
  
  @Override
  public void observe(Cluster cluster) throws Exception {
    sense((YarnCluster) cluster);
  }

  public abstract void sense(YarnCluster cluster) throws Exception;
}
