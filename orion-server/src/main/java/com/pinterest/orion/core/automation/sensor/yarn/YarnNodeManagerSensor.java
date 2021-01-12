package com.pinterest.orion.core.automation.sensor.yarn;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.yarn.api.records.NodeReport;
import org.apache.hadoop.yarn.api.records.NodeState;
import org.apache.hadoop.yarn.client.api.YarnClient;

import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.core.actions.ActionFactory;
import com.pinterest.orion.core.actions.alert.AlertFactory;
import com.pinterest.orion.core.yarn.YarnCluster;

public class YarnNodeManagerSensor extends YarnSensor {

  @Override
  public String getName() {
    return "yarnclustersensor";
  }

  @Override
  public void sense(YarnCluster cluster) throws Exception {
    if (cluster.getConnection() == null) {
      YarnClient yarnClient = YarnClient.createYarnClient();
      yarnClient.init(cluster.getConfiguration());
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
    
    System.err.println("Cluster info loaded");

  }

  public static void main(String[] args) throws Exception {
    YarnCluster cluster = new YarnCluster("test", "test", Arrays.asList(), Arrays.asList(), new ActionFactory(),
        new AlertFactory(), null, null, null);
    Map<String, Object> config = new HashMap<>();
    config.put("coresite.file", args[0]);
    config.put("yarnsite.file", args[1]);
    cluster.initialize(config);
    YarnNodeManagerSensor sensor = new YarnNodeManagerSensor();
    sensor.observe(cluster);
  }
}
