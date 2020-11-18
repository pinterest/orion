package com.pinterest.orion.core.yarn;

import java.util.Properties;

import com.pinterest.orion.common.NodeInfo;

public class YarnNodeManager extends YarnNode {

  private static final long serialVersionUID = 1L;

  public YarnNodeManager(YarnCluster cluster,
                         NodeInfo currentNodeInfo,
                         Properties connectionProps) {
    super(cluster, currentNodeInfo, connectionProps);
  }

}
