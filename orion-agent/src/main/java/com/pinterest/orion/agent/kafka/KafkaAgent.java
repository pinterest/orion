/*******************************************************************************
 * Copyright 2020 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.pinterest.orion.agent.kafka;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

import com.pinterest.orion.agent.metrics.MetricsRetriever;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.yaml.snakeyaml.Yaml;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pinterest.orion.agent.BaseAgent;
import com.pinterest.orion.agent.OrionAgentConfig;
import com.pinterest.orion.agent.metrics.JMXMetricRetreiverTask;
import com.pinterest.orion.agent.metrics.MetricDefinition;
import com.pinterest.orion.agent.metrics.MetricRetrieverTask;
import com.pinterest.orion.agent.utils.MetadataUtils;
import com.pinterest.orion.agent.utils.MetricUtils;
import com.pinterest.orion.agent.utils.OrionCmd;
import com.pinterest.orion.common.CmdResult;
import com.pinterest.orion.common.Metric;
import com.pinterest.orion.common.Metrics;
import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.common.StatusInfo;
import com.pinterest.orion.common.StatusType;
import com.pinterest.orion.utils.NetworkUtils;
import com.pinterest.orion.utils.OrionConstants;

public class KafkaAgent extends BaseAgent {

  public static final String KAFKA_SERVER_PROPERTIES = "kafkaServerProperties";
  public static final String KAFKAMIRROR_CONFIG_DIRECTORY = "mirrorConfigDirectory";
  private static final Logger logger = Logger.getLogger(KafkaAgent.class.getCanonicalName());
  private final static String[] KAFKA_START_SERVICE_CMD = new String[] { "sudo", "service", "kafka",
      "start" };
  private final static String[] KAFKA_STOP_SERVICE_CMD = new String[] { "sudo", "service", "kafka",
      "stop" };
  private final static String[] KAFKA_RESTART_SERVICE_CMD = new String[] { "sudo", "service",
      "kafka", "restart" };
  private final static String[] KAFKA_UPDATE_CONFIGS_CMD = new String[] { "sudo", "pwrapper",
      "--enable" };
  private static final String[] KAFKA_PROBE_NETSTAT_CMD = new String[] { "netstat", "-atW" };
  private static long KAFKA_CLIENT_TIMEOUT_SEC = 3L;
  private static final Gson GSON = new Gson();

  private volatile Properties kafkaBrokerProperties = new Properties();
  private boolean isMirrorAgent = false;
  private AdminClient adminClient;
  private AdminClient heartbeatAdminClient;
  private int port;
  private List<MetricDefinition> metricDefinitions;
  private JMXConnector jmxConnector = null;
  private MBeanServerConnection mbs = null;

  public KafkaAgent(OrionAgentConfig config) throws IOException {
    super(config);
    if (config.getAgentConfigs().containsKey(KAFKAMIRROR_CONFIG_DIRECTORY)) {
      // is mirror agent
      isMirrorAgent = true;
    } else {
      kafkaBrokerProperties.load(new FileInputStream(new File(config.getAgentConfigs()
              .getOrDefault(KAFKA_SERVER_PROPERTIES, "/etc/kafka/server.properties").toString())));
    }
  }

  @Override
  public void initialize() throws Exception {
    super.initialize();
    port = Integer.parseInt(kafkaBrokerProperties.getProperty("jmx.port", "9999"));
    metricDefinitions = GSON.fromJson(
            new String(Files.readAllBytes(
                    new File(config.getMetricsFilepath()).toPath())),
            new TypeToken<List<MetricDefinition>>(){}.getType());
  }

  @Override
  public void initializeHeartbeat() throws InterruptedException {
    if (heartbeatAdminClient != null) {
      heartbeatAdminClient.close(KAFKA_CLIENT_TIMEOUT_SEC, TimeUnit.SECONDS);
    }
    Properties props = new Properties();
    props.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    heartbeatAdminClient = AdminClient.create(props);
  }

  @Override
  public void initializeMetricsPoll() throws InterruptedException {
    // need to re-create the adminClient each time since the bootstrap server is
    // only used when
    // initializing
    if (!isMirrorAgent) {
      initializeAdminClient();
    }
  }

  private void initializeAdminClient() {
    if (adminClient != null) {
      adminClient.close(KAFKA_CLIENT_TIMEOUT_SEC, TimeUnit.SECONDS);
    }
    Properties props = new Properties();
    props.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    adminClient = AdminClient.create(props);
  }

  protected Map<String, String> getKafkaServiceInfo() {
    Map<String, String> kafkaInfo = new HashMap<>();
    kafkaInfo.put("zookeeper.connect", kafkaBrokerProperties.getProperty("zookeeper.connect"));
    kafkaInfo.put("inter.broker.protocol.version",
        kafkaBrokerProperties.getProperty("inter.broker.protocol.version"));
    kafkaInfo.put("log.message.format.version",
        kafkaBrokerProperties.getProperty("log.message.format.version"));
    return kafkaInfo;
  }

  @Override
  public NodeInfo getNodeInfo() throws Exception {
    NodeInfo info = new NodeInfo();
    info.setHostname(config.getHostname());
    // TODO should we use something else?
    info.setClusterId(config.getDefaultClusterId());
    Map<String, String> metadata = new HashMap<>();
    try {
      metadata.putAll(MetadataUtils.getEC2Metadata());
    } catch (Exception e) {
    }
    info.setEnvironment(metadata);
    info.setIp(config.getIp());
    info.setServicePort(getKafkaPlainTextListenerPort(kafkaBrokerProperties));
    info.setRack(kafkaBrokerProperties.getProperty("broker.rack", "n/a"));
    info.setNodeId(getNodeId());
    info.setNodeType(metadata.get(OrionConstants.INSTANCE_TYPE));
    // TODO add service level info + populate all kafka broker configs here
    info.setServiceInfo(getKafkaServiceInfo());

    // TODO add proper agent configs
    Map<String, String> agentSettings = new HashMap<>();
    agentSettings.put("version", config.getVersion());
    info.setAgentSettings(agentSettings);
    info.setLocaltime(System.currentTimeMillis());
    return info;
  }

  private String getNodeId() {
    return kafkaBrokerProperties.getProperty("broker.id", "n/a");
  }

  public static int getKafkaPlainTextListenerPort(Properties kafkaBrokerProperties) {
    String property = kafkaBrokerProperties.getProperty("listeners");
    if (property != null) {
      Pattern compile = Pattern.compile("(.*PLAINTEXT\\://\\:(?<port>\\d+).*)");
      Matcher matcher = compile.matcher(property);
      if (matcher.matches()) {
        property = matcher.group("port");
      } else {
        property = null;
      }
    }
    if (property == null) {
      property = "-1";
    }
    return Integer.parseInt(property);
  }

  @Override
  public StatusInfo getServiceStatus() throws Exception {
    StatusInfo status = new StatusInfo();
    if (isUpgrading()) {
      status.setStatusType(StatusType.UPGRADE);
      return status;
    }

    try {
      Future<Long> kafkaUptimeFuture = getKafkaUptime();
      DescribeClusterResult result = heartbeatAdminClient.describeCluster();
      result.clusterId().get(KAFKA_CLIENT_TIMEOUT_SEC, TimeUnit.SECONDS);
      status.setUptime(kafkaUptimeFuture.get(3, TimeUnit.SECONDS));
      logger.info("Kafka uptime: " + kafkaUptimeFuture.get());
      status.setStatusType(StatusType.OK);
    } catch (Exception e) {
      if (heartbeatAdminClient != null) {
        heartbeatAdminClient.close(KAFKA_CLIENT_TIMEOUT_SEC, TimeUnit.SECONDS);
        heartbeatAdminClient = null; // need to reset to null such that service metrics collection will fail
      }
      logger.severe("Error when getting Kafka status: " + e);
      status.setStatusType(StatusType.ERROR);
      status.setUptime(-1);
      status.setReason(e.toString());
    }
    return status;
  }

  protected Future<Long> getKafkaUptime() {
    return CompletableFuture.supplyAsync(() -> {
      try {
        OrionCmd cmd = new OrionCmd(new CmdResult(), config.getCmdLogDirectory(), "kafkauptime",
            KafkaAgent::parseKafkaUptimeLine, null, "ps", "-u", "kafka", "-o", "etimes=,cmd=");
        cmd.exec();
        return cmd.get();
      } catch (Exception e) {
        throw new CompletionException(e);
      }
    }).thenApply(result -> {
      String out = result.getOut();
      if (out != null) {
        return Long.parseLong(out) * 1000;
      }
      return -1L;
    }).exceptionally(e -> {
      logger.severe("Could not get status of Kafka: " + e);
      return -1L;
    });
  }

  protected static String parseKafkaUptimeLine(String line) {
    String[] row = line.trim().split(" ", 2);
    if (row.length == 2 && row[1].contains("kafka.Kafka")) {
      return row[0];
    }
    return null;
  }

  @Override
  public StatusInfo getAgentStatus() throws Exception {
    StatusInfo statusInfo = new StatusInfo(StatusType.OK);
    statusInfo.setUptime(ManagementFactory.getRuntimeMXBean().getUptime());
    return statusInfo;
  }

  @Override
  public OrionCmd startService() throws Exception {
    OrionCmd cmd = new OrionCmd(getCurrentCommand().getResult(), config.getCmdLogDirectory(),
        UUID.randomUUID().toString(), KAFKA_START_SERVICE_CMD);
    return cmd.exec();
  }

  @Override
  public OrionCmd stopService() throws Exception {
    OrionCmd cmd = new OrionCmd(getCurrentCommand().getResult(), config.getCmdLogDirectory(),
        UUID.randomUUID().toString(), KAFKA_STOP_SERVICE_CMD);
    return cmd.exec();
  }

  @Override
  public OrionCmd restartService() throws Exception {
    OrionCmd cmd = new OrionCmd(getCurrentCommand().getResult(), config.getCmdLogDirectory(),
        UUID.randomUUID().toString(), KAFKA_RESTART_SERVICE_CMD);
    return cmd.exec();
  }

  @Override
  public OrionCmd updateConfigs() throws Exception {
    OrionCmd cmd = new OrionCmd(getCurrentCommand().getResult(), config.getCmdLogDirectory(),
        UUID.randomUUID().toString(), KAFKA_UPDATE_CONFIGS_CMD);
    return cmd.exec();
  }

  @Override
  public OrionCmd upgradeAgent() throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public OrionCmd upgradeService() throws Exception {
    // TODO Auto-generated method stub
    setUpgrading(true);
    return null;
  }

  @Override
  public Metrics getServiceMetrics() throws Exception {
    Metrics metrics = super.getServiceMetrics();

    // use new metrics system

    Set<MetricRetrieverTask> metricRetrieverTasks = getMetricsTasks();
    logger.info("Retrieving " + metricRetrieverTasks.size() + " metrics");
    List<Future<Metric>> metricFutures = new ArrayList<>();
    for (MetricRetrieverTask task: metricRetrieverTasks) {
      metricFutures.add(MetricsRetriever.getMetric(task));
    }
    for (Future<Metric> metricFuture : metricFutures) {
      Metric metric = metricFuture.get(10, TimeUnit.SECONDS);
      metrics.addToMetrics(metric);
    }
    return metrics;
  }

  public static void main(String[] args) throws Exception {
    OrionAgentConfig config = new Yaml().loadAs(new FileInputStream(args[0]), OrionAgentConfig.class);
    KafkaAgent agent = new KafkaAgent(config);
    agent.initialize();
    agent.initializeMetricsPoll();
    Metrics metrics = agent.getServiceMetrics();
    for (Metric m: metrics.getMetrics()) {
      System.out.println("metric: " + m.toString());
    }
  }

  @Override
  public List<String> getEntityValues(String entity) {
    switch (entity) {
      case "topic":
        if (isMirrorAgent) {
          return MetricUtils.getTopicsList(config.getAgentConfigs().get(KAFKAMIRROR_CONFIG_DIRECTORY).toString());
        }
        return MetricUtils.getTopicsList(adminClient, KAFKA_CLIENT_TIMEOUT_SEC, getNodeId());
      case "host":
        return Collections.singletonList(config.getHostname());
    }
    return null;
  }

  @Override
  public void addToTasksFromDefinition(Set<MetricRetrieverTask> tasks, MetricDefinition newDef) throws Exception {
    switch (newDef.getMetricsSource()) {
      case "jmx":
        boolean success = getOrCreateJMXServerConnection();
        if (success) {
          tasks.add(new JMXMetricRetreiverTask(newDef, mbs));
        }
        break;
    }
  }

  private Set<MetricRetrieverTask> getMetricsTasks() throws Exception {
    Map<String, List<String>> entityValueMap = new HashMap<>();
    return MetricUtils.getTasksFromMetricDefinitionTemplates(entityValueMap, metricDefinitions, this);
  }

  private boolean getOrCreateJMXServerConnection() throws Exception {
    if (!NetworkUtils.checkPort(port)) {
      logger.warning(
              "JMX connection to Kafka broker is not available, kafka metrics won't be captured");
      return false;
    } else if (jmxConnector == null || mbs == null) {
      jmxConnector = MetricUtils.getJMXConnector("localhost", port);
      if (jmxConnector != null) {
        mbs = jmxConnector.getMBeanServerConnection();
      } else {
        throw new Exception("Failed to create JMX server connection");
      }
    }
    return true;
  }

  @Override
  public OrionCmd probeNetstat() throws Exception {
    OrionCmd cmd = new OrionCmd(getCurrentCommand().getResult(), config.getCmdLogDirectory(),
        UUID.randomUUID().toString(), KAFKA_PROBE_NETSTAT_CMD);
    return cmd.exec();
  }

}