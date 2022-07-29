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
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

import com.pinterest.orion.agent.metrics.MetricsRetriever;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.Node;
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

  private volatile Properties kafkaBrokerProperties = new Properties();
  private AdminClient adminClient;
  private int jmxPort;
  private String localBootstrapServerAddress;

  public KafkaAgent(OrionAgentConfig config) throws IOException {
    super(config);
    kafkaBrokerProperties.load(new FileInputStream(new File(config.getAgentConfigs()
            .getOrDefault(KAFKA_SERVER_PROPERTIES, "/etc/kafka/server.properties").toString())));
    localBootstrapServerAddress = "localhost:" + getKafkaPlainTextListenerPort(kafkaBrokerProperties);
  }

  @Override
  public void initialize() throws Exception {
    super.initialize();
    jmxPort = Integer.parseInt(kafkaBrokerProperties.getProperty("jmx.port", "9999"));
  }

  @Override
  public void initializeHeartbeat() throws InterruptedException {
  }

  @Override
  public void initializeMetricsPoll() throws Exception {
    // need to re-create the adminClient each time since the bootstrap server is
    // only used when
    // initializing
    super.initializeMetricsPoll();
    initializeAdminClient();
  }

  @Override
  protected int getMetricsPort() {
    return jmxPort;
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  private void initializeAdminClient() {
    try {
      if (adminClient != null) {
        adminClient.listTopics().names().get(KAFKA_CLIENT_TIMEOUT_SEC, TimeUnit.SECONDS);
        return; // do not re-initialize
      }
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      adminClient.close(KAFKA_CLIENT_TIMEOUT_SEC, TimeUnit.SECONDS);
    }
    Properties props = new Properties();
    props.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, localBootstrapServerAddress);
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

    AdminClient heartbeatAdminClient = null;
    try {
      Properties props = new Properties();
      props.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, localBootstrapServerAddress);
      heartbeatAdminClient = AdminClient.create(props);
      Future<Long> kafkaUptimeFuture = getKafkaUptime();
      DescribeClusterResult result = heartbeatAdminClient.describeCluster();
      result.clusterId().get(KAFKA_CLIENT_TIMEOUT_SEC, TimeUnit.SECONDS);
      status.setUptime(kafkaUptimeFuture.get(3, TimeUnit.SECONDS));
      logger.info("Kafka uptime: " + kafkaUptimeFuture.get());
      status.setStatusType(StatusType.OK);
    } catch (Exception e) {
      logger.severe("Error when getting Kafka status: " + e);
      status.setStatusType(StatusType.ERROR);
      status.setUptime(-1);
      status.setReason(e.toString());
    } finally {
      if (heartbeatAdminClient != null) {
        heartbeatAdminClient.close(KAFKA_CLIENT_TIMEOUT_SEC, TimeUnit.SECONDS);
      }
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

  private List<String> getTopicsList(AdminClient adminClient, long timeout, String nodeId) {
    try {
      if (adminClient == null) {
        throw new Exception("adminClient is null");
      }
      Set<String> availableTopics = new HashSet<>();
      Set<String> topics = adminClient.listTopics().names().get(timeout,
              TimeUnit.SECONDS);
      DescribeTopicsResult describeTopics = adminClient.describeTopics(topics);
      Map<String, TopicDescription> map = describeTopics.all().get(timeout,
              TimeUnit.SECONDS);
      map.values().stream().forEach(td -> {
        Optional<Node> entry = td.partitions().stream().flatMap(p -> p.replicas().stream())
                .filter(n -> n.idString().equals(nodeId)).findFirst();
        if (entry.isPresent()) {
          availableTopics.add(td.name());
        }
      });
      return new ArrayList<>(availableTopics);
    } catch (Exception e) {
      logger.severe("Unable to fetch topics list due to error: " + e);
    }
    return Collections.emptyList();
  }

  @Override
  public List<String> getEntityValues(String entity) {
    switch (entity) {
      case "topic":
        return getTopicsList(adminClient, KAFKA_CLIENT_TIMEOUT_SEC, getNodeId());
      case "host":
        return Collections.singletonList(config.getHostname());
    }
    return null;
  }

  @Override
  public OrionCmd probeNetstat() throws Exception {
    OrionCmd cmd = new OrionCmd(getCurrentCommand().getResult(), config.getCmdLogDirectory(),
        UUID.randomUUID().toString(), KAFKA_PROBE_NETSTAT_CMD);
    return cmd.exec();
  }

}