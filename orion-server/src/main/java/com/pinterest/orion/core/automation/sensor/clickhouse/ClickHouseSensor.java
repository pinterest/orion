package com.pinterest.orion.core.automation.sensor.clickhouse;

import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.automation.sensor.Sensor;
import com.pinterest.orion.core.clickhouse.ClickHouseCluster;

public abstract class ClickHouseSensor extends Sensor {

  @Override
  public void observe(Cluster cluster) throws Exception {
    if (logger == null) {
      logger = getLogger(cluster);
    }
    if(cluster instanceof ClickHouseCluster){
      sense((ClickHouseCluster) cluster);
    }
  }
  
  public abstract void sense(ClickHouseCluster cluster) throws Exception;

}
