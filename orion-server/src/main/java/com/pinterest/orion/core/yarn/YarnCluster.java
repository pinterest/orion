package com.pinterest.orion.core.yarn;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.ClusterStateSink;
import com.pinterest.orion.core.CostCalculator;
import com.pinterest.orion.core.Node;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.Utilization;
import com.pinterest.orion.core.actions.ActionFactory;
import com.pinterest.orion.core.actions.alert.AlertFactory;
import com.pinterest.orion.core.actions.audit.ActionAuditor;
import com.pinterest.orion.core.automation.operator.Operator;
import com.pinterest.orion.core.automation.sensor.Sensor;

public class YarnCluster extends Cluster {

  private static final long serialVersionUID = 1L;

  public YarnCluster(String id,
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
    // TODO Auto-generated constructor stub
  }

  @Override
  protected void bootstrapClusterInfo(Map<String, Object> config) throws PluginConfigurationException {
    // TODO Auto-generated method stub
    
  }

  @Override
  protected Node getNodeInstance(NodeInfo info) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void addNodeWithoutAgent(NodeInfo info) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Logger logger() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Map<String, Utilization> getUtilizationMap() {
    // TODO Auto-generated method stub
    return null;
  }

}
