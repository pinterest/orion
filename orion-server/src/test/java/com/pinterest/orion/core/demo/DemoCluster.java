package com.pinterest.orion.core.demo;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.ClusterStateSink;
import com.pinterest.orion.core.ClusterType;
import com.pinterest.orion.core.CostCalculator;
import com.pinterest.orion.core.Node;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.Utilization;
import com.pinterest.orion.core.actions.ActionFactory;
import com.pinterest.orion.core.actions.alert.AlertFactory;
import com.pinterest.orion.core.actions.audit.ActionAuditor;
import com.pinterest.orion.core.automation.operator.Operator;
import com.pinterest.orion.core.automation.sensor.Sensor;

@ClusterType(name = "demo", priority = 1)
public class DemoCluster extends Cluster {

  private static final long serialVersionUID = 1L;

  public DemoCluster(String id,
                     String name,
                     String type,
                     List<Sensor> monitors,
                     List<Operator> operators,
                     ActionFactory actionFactory,
                     AlertFactory alertFactory,
                     ActionAuditor auditSink,
                     ClusterStateSink stateSink,
                     CostCalculator costCalculator) {
    super(id, name, type, monitors, operators, actionFactory, alertFactory, auditSink, stateSink,
        costCalculator);
  }

  @Override
  protected void bootstrapClusterInfo(Map<String, Object> config) throws PluginConfigurationException {

  }

  @Override
  protected Node getNodeInstance(NodeInfo info) {
    return null;
  }

  @Override
  public void addNodeWithoutAgent(NodeInfo info) {
  }

  @Override
  public Logger logger() {
    return null;
  }

  @Override
  public Map<String, Utilization> getUtilizationMap() {
    return null;
  }

}
