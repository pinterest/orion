package com.pinterest.orion.core.automation.operator.clickhouse;

import java.util.logging.Logger;

import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.automation.operator.Operator;
import com.pinterest.orion.core.clickhouse.ClickHouseCluster;
import com.pinterest.orion.core.actions.clickhouse.PublishAllNodeConfigAction;

public class PublishConfigOperator extends ClickHouseOperator {
  private static final Logger logger = 
    Logger.getLogger(PublishConfigOperator.class.getCanonicalName());

  @Override
  public void operate(ClickHouseCluster cluster) throws Exception {
    logger.info("Initializing PublishAllNodeConfigAction for ClickHouse cluster...");
    PublishAllNodeConfigAction action = new PublishAllNodeConfigAction();
    dispatch(action);
  }

  @Override
  public String getName() {
    return "PublishConfigOperator";
  }
}
