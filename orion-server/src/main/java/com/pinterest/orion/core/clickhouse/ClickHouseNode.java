package com.pinterest.orion.core.clickhouse;

import java.util.Properties;

import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.Node;

public class ClickHouseNode extends Node {

  private static final long serialVersionUID = 1L;

  public ClickHouseNode(Cluster cluster, NodeInfo currentNodeInfo, Properties connectionProps) {
    super(cluster, currentNodeInfo, connectionProps);
  }

}
