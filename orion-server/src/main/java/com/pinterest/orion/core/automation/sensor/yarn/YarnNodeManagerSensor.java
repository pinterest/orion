package com.pinterest.orion.core.automation.sensor.yarn;

import java.util.List;

import org.apache.hadoop.yarn.api.records.NodeReport;
import org.apache.hadoop.yarn.api.records.NodeState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.impl.YarnClientImpl;

import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.core.yarn.YarnCluster;

public class YarnNodeManagerSensor extends YarnSensor {

  @Override
  public String getName() {
    return "yarnclustersensor";
  }

  @Override
  public void sense(YarnCluster cluster) throws Exception {
    if (cluster.getConnection() == null) {
      YarnClient yarnClient = new YarnClientImpl();
      yarnClient.start();
      cluster.setConnection(yarnClient);
    }
    YarnClient yarn = cluster.getConnection();

    List<NodeReport> nodeReports = yarn.getNodeReports(NodeState.RUNNING, NodeState.DECOMMISSIONED,
        NodeState.LOST, NodeState.UNHEALTHY, NodeState.REBOOTED);
    setAttribute(cluster, "allNodes", nodeReports);

    nodeReports.stream().forEach(r -> {
      NodeInfo info = new NodeInfo();

      info.setClusterId(cluster.getClusterId());
      info.setNodeId(r.getNodeId().toString());
      info.setHostname(r.getNodeId().getHost());
      info.setServicePort(r.getNodeId().getPort());

      cluster.addNodeWithoutAgent(info);
    });
    
  }

}
