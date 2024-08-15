package com.pinterest.orion.core.clickhouse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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

public class ClickHouseCluster extends Cluster {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = Logger.getLogger(ClickHouseNode.class.getName());
  public static final String CLUSTER_REGION = "region";
  public static final String DEFAULT_REGION = "us-east-1";

  public static final String USER = "user";
  private static final String DEFAULT_USER = "default";

  public static final String PASSWORD = "password";
  private static final String DEFAULT_PASSWORD = "";

  public static final String SERVERSET_PATH = "serversetPath";
  public static final String CONFIG_S3_BUCKET = "configS3Bucket";
  public static final String CONFIG_TEMPLATE_PATH = "configTemplatePath";

  private Map<String, Object> config;

  public ClickHouseCluster(String id,
                           String name,
                           List<Sensor> monitors,
                           List<Operator> operators,
                           ActionFactory actionFactory,
                           AlertFactory alertFactory,
                           ActionAuditor auditSink,
                           ClusterStateSink stateSink,
                           CostCalculator costCalculator) {
    super(id, name, "ClickHouse", monitors, operators, actionFactory, alertFactory, auditSink, stateSink,
        costCalculator);
  }

  @Override
  protected void bootstrapClusterInfo(Map<String, Object> config) throws PluginConfigurationException {
    this.config = config;
    setAttribute(CLUSTER_REGION, config.getOrDefault(CLUSTER_REGION, DEFAULT_REGION));
    setAttribute(USER, config.getOrDefault(USER, DEFAULT_USER));
    setAttribute(PASSWORD, config.getOrDefault(PASSWORD, DEFAULT_PASSWORD));
    setAttribute(SERVERSET_PATH, config.get(SERVERSET_PATH));
    setAttribute(CONFIG_S3_BUCKET, config.get(CONFIG_S3_BUCKET));
    setAttribute(CONFIG_TEMPLATE_PATH, config.get(CONFIG_TEMPLATE_PATH));
  }

  @Override
  protected Node getNodeInstance(NodeInfo info) {
    return new ClickHouseNode(this, info, new Properties());
  }

  @Override
  public void addNodeWithoutAgent(NodeInfo info) {
    getNodeMap().put(info.getNodeId(), getNodeInstance(info));
  }

  @Override
  public Logger logger() {
    return logger;
  }
  
  public Map<String, Object> getConfig() {
    return config;
  }

  @Override
  public Map<String, Utilization> getUtilizationMap() {
    return new HashMap<>();
  }

}
