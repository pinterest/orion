package com.pinterest.orion.core.yarn;

import java.util.Properties;

import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.core.Node;

public abstract class YarnNode extends Node {

  private static final long serialVersionUID = 1L;

  public YarnNode(YarnCluster cluster, NodeInfo currentNodeInfo, Properties connectionProps) {
    super(cluster, currentNodeInfo, connectionProps);
  }

}
