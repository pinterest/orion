package com.pinterest.orion.core.actions.memq;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.kafka.clients.admin.AdminClient;

import com.google.gson.Gson;
import com.pinterest.orion.core.Attribute;
import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.ActionEngine;
import com.pinterest.orion.core.actions.ActionFactory;
import com.pinterest.orion.core.actions.kafka.AssignmentCreateKafkaTopicAction;
import com.pinterest.orion.core.automation.operator.kafka.BrokersetTopicOperator;
import com.pinterest.orion.core.automation.sensor.kafka.KafkaClusterInfoSensor;
import com.pinterest.orion.core.automation.sensor.memq.MemqClusterSensor;
import com.pinterest.orion.core.automation.sensor.memq.MemqTopicSensor;
import com.pinterest.orion.core.automation.sensor.memq.TopicConfig;
import com.pinterest.orion.core.kafka.Brokerset;
import com.pinterest.orion.core.kafka.TopicAssignment;
import com.pinterest.orion.core.memq.MemqCluster;

public class MemqTopicCreationAction extends Action {

  public static final String ATTR_TOPIC_NAME_KEY = "topic";
  public static final String ATTR_TOPIC_CONFIG_KEY = "topicConfig";
  private static final String[] REQUIRED_ARG_KEYS = new String[] { ATTR_TOPIC_NAME_KEY,
      ATTR_TOPIC_CONFIG_KEY };

  @Override
  public String getName() {
    return "Create Topic " + getAttribute(this, ATTR_TOPIC_NAME_KEY).getValue().toString();
  }

  @Override
  public void runAction() throws Exception {
    checkRequiredArgs(REQUIRED_ARG_KEYS);

    MemqCluster cluster = (MemqCluster) getEngine().getCluster();
    if (!cluster.containsAttribute(KafkaClusterInfoSensor.ATTR_BROKERSET_KEY)) {
      markFailed("Missing brokerset information");
      return;
    }
    
    String topicName = getAttribute(ATTR_TOPIC_NAME_KEY).getValue();
    TopicConfig config = getAttribute(ATTR_TOPIC_CONFIG_KEY).getValue();
    
    if (cluster.getZkClient().checkExists().forPath(MemqClusterSensor.TOPICS + "/" + topicName)!=null) {
      markFailed("Topic already exists deployment failed");
      return;
    }
    
    Attribute brokersetMapAttr = cluster.getAttribute(KafkaClusterInfoSensor.ATTR_BROKERSET_KEY);
    Map<String, Brokerset> brokersetMap = brokersetMapAttr.getValue();

    // create
    Properties outputHandlerConfig = config.getOutputHandlerConfig();
    String notificationTopic = outputHandlerConfig.getProperty(MemqTopicSensor.NOTIFICATION_TOPIC);
    String notificationServerset = outputHandlerConfig
        .getProperty(MemqTopicSensor.NOTIFICATION_SERVERSET);
    String notificationBrokerset = outputHandlerConfig.getProperty(MemqCluster.NOTIFICATION_BROKERSET);
    int stride = Integer.parseInt(outputHandlerConfig.getProperty(MemqCluster.NOTIFICATION_STRIDE, "0"));

    TopicAssignment assignment = new TopicAssignment();
    assignment.setBrokerset(notificationBrokerset);
    assignment.setTopicName(notificationTopic);
    assignment.setStride(stride);
    assignment.setReplicationFactor(6);
    Map<String, String> notificationTopicConfigs = new HashMap<>();
    notificationTopicConfigs.put("min.insync.replicas", "2");
    notificationTopicConfigs.put("segment.ms", "3600000");
    notificationTopicConfigs.put("segment.bytes", "1048576");
    assignment.setConfig(notificationTopicConfigs);

    Brokerset brokerset = brokersetMap.get(notificationBrokerset);
    if (brokerset == null) {
      markFailed("Brokerset:" + notificationBrokerset + " is not present");
      return;
    }

    AssignmentCreateKafkaTopicAction createIdealBalancedTopicAction = BrokersetTopicOperator
        .createIdealBalancedTopicAction(brokerset, assignment, new HashSet<>(), 30);

    MemqNotificationTopicCreationAction action = new MemqNotificationTopicCreationAction(cluster,
        notificationServerset, createIdealBalancedTopicAction);
    getChildren().add(action);
    getEngine().dispatchChild(this, action);
    action.get(100, TimeUnit.SECONDS);

    Gson gson = new Gson();
    cluster.getZkClient().create().forPath(MemqClusterSensor.TOPICS + "/" + topicName,
        gson.toJson(config).getBytes());
    markSucceeded();
  }

  public class MemqNotificationTopicCreationAction extends Action {

    private MemqCluster cluster;
    private String notificationServerset;
    private AssignmentCreateKafkaTopicAction createIdealBalancedTopicAction;

    public MemqNotificationTopicCreationAction(MemqCluster cluster,
                                               String notificationServerset,
                                               AssignmentCreateKafkaTopicAction createIdealBalancedTopicAction) {
      this.cluster = cluster;
      this.notificationServerset = notificationServerset;
      this.createIdealBalancedTopicAction = createIdealBalancedTopicAction;
    }

    @Override
    public String getName() {
      return "Create Notification Topic "
          + getAttribute(this, ATTR_TOPIC_NAME_KEY).getValue().toString();
    }

    @Override
    public void runAction() throws Exception {
      AdminClient adminClient = cluster.getReadClusterClientMap().get(notificationServerset);
      if (adminClient == null) {
        adminClient = MemqTopicSensor.initializeAdminClient(notificationServerset);
        cluster.getReadClusterClientMap().put(notificationServerset,
            adminClient);
      }
      createIdealBalancedTopicAction.run("", adminClient);
      if (createIdealBalancedTopicAction.isSuccess()) {
        markSucceeded();
      } else {
        markFailed("Failed to execute notification topic creation");
      }
    }

  }

  public static void main(String[] args) throws Exception {
    MemqTopicCreationAction action = new MemqTopicCreationAction();
    action.setAttribute(ATTR_TOPIC_NAME_KEY, "test");
    TopicConfig topicConfig = new TopicConfig();
    topicConfig.setTopic("test");
    Properties outputHandlerConfig = new Properties();
    outputHandlerConfig.setProperty(MemqCluster.NOTIFICATION_BROKERSET, "Static_B24_P24_0");
    outputHandlerConfig.setProperty(MemqTopicSensor.NOTIFICATION_TOPIC, "memq_notification");
    outputHandlerConfig.setProperty(MemqTopicSensor.NOTIFICATION_SERVERSET,
        "/var/serverset/discovery.testkafka.prod");
    topicConfig.setOutputHandlerConfig(outputHandlerConfig);
    action.setAttribute(ATTR_TOPIC_CONFIG_KEY, topicConfig);

    ActionFactory factory = new ActionFactory();

    MemqCluster cluster = new MemqCluster("test", "Test", Arrays.asList(), Arrays.asList(), factory,
        null, null, null, null);
    Map<String, Object> config = new HashMap<>();
    Map<String, String> map = new HashMap<>();
    map.put("/var/serverset/discovery.testkafka.prod", "/tmp/testkafka");
    config.put(MemqCluster.NOTIFICATION_CLUSTER_CONFIG, map);
    cluster.initialize(config);
    
    
    String zkUrl = "localhost:2181";
    CuratorFramework zkClient = CuratorFrameworkFactory.newClient(zkUrl,
        new ExponentialBackoffRetry(1000, 3));
    zkClient.start();
    
    cluster.setZkClient(zkClient);
    Map<String, Brokerset> brokersetMap = new HashMap<>();
    Brokerset value = new Brokerset("Static_B24_P24_0",
        Arrays.asList(new Brokerset.BrokersetRange(1, 7)), 24);
    brokersetMap.put("Static_B24_P24_0", value);
    cluster.setAttribute(KafkaClusterInfoSensor.ATTR_BROKERSET_KEY, brokersetMap);

    ActionEngine engine = new ActionEngine(cluster, factory, null, null);
    engine.initialize(new HashMap<>());
    action.setEngine(engine);
    action.run();

    System.out.println(action.isSuccess() + " " + action.getResult());
  }

}