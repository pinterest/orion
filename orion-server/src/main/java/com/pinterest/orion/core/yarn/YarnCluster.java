package com.pinterest.orion.core.yarn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.hadoop.yarn.client.api.YarnClient;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import com.pinterest.orion.core.hbase.HBaseCluster;

public class YarnCluster extends Cluster {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = Logger.getLogger(HBaseCluster.class.getName());
  
  private Map<String, Object> config;
  @JsonIgnore
  private transient YarnClient connection;

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
    super(id, name, "YARN", monitors, operators, actionFactory, alertFactory, auditSink, stateSink,
        costCalculator);
  }

  @Override
  protected void bootstrapClusterInfo(Map<String, Object> config) throws PluginConfigurationException {
    this.config = config;
    // core-site.xml
    // yarn-site.xml
    // hdfs-site.xml
  }

  @Override
  protected Node getNodeInstance(NodeInfo info) {
    return new YarnNodeManager(this, info, new Properties());
  }

  @Override
  public void addNodeWithoutAgent(NodeInfo info) {
    getNodeMap().put(info.getNodeId(), new YarnNodeManager(this, info, new Properties()));
  }

  @Override
  public Logger logger() {
    return logger;
  }

  @Override
  public Map<String, Utilization> getUtilizationMap() {
    return new HashMap<>();
  }

  public YarnClient getConnection() {
    return connection;
  }

  public void setConnection(YarnClient connection) {
    this.connection = connection;
  }

}
