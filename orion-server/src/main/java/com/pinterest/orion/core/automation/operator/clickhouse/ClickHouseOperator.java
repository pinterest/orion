package com.pinterest.orion.core.automation.operator.clickhouse;

import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.automation.operator.Operator;
import com.pinterest.orion.core.clickhouse.ClickHouseCluster;

public abstract class ClickHouseOperator extends Operator {

  @Override
  public final void operate(Cluster cluster) throws Exception {
    if(cluster instanceof ClickHouseCluster){
      operate((ClickHouseCluster) cluster);
    }
  }

  public abstract void operate(ClickHouseCluster cluster) throws Exception;

}
