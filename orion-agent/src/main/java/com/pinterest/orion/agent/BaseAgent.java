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
package com.pinterest.orion.agent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pinterest.orion.agent.metrics.JMXMetricRetreiverTask;
import com.pinterest.orion.agent.metrics.MetricDefinition;
import com.pinterest.orion.agent.metrics.MetricRetrieverTask;
import com.pinterest.orion.agent.metrics.MetricsRetriever;
import com.pinterest.orion.agent.utils.MetricUtils;
import com.pinterest.orion.agent.utils.OrionCmd;
import com.pinterest.orion.common.CmdResult.CmdState;
import com.pinterest.orion.common.Metric;
import com.pinterest.orion.common.MetricType;
import com.pinterest.orion.common.Metrics;
import com.pinterest.orion.common.NodeCmd;
import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.common.StatusInfo;
import com.pinterest.orion.common.Value;
import com.pinterest.orion.configs.StatsConfiguration;
import com.pinterest.orion.metrics.OpenTsdbStatsPusher;
import com.pinterest.orion.metrics.StatsPusher;
import com.pinterest.orion.utils.NetworkUtils;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

public abstract class BaseAgent {

  private static final Logger LOG = Logger.getLogger(BaseAgent.class.getCanonicalName());
  private static final String PROC_NET_DEV = "/proc/net/dev";
  private boolean isUpgrading = false;
  protected OrionAgentConfig config;
  private NodeCmd currentCommand;
  private volatile long metricsIterationCount;
  private JMXConnector jmxConnector = null;
  protected MBeanServerConnection mbs = null;
  private List<MetricDefinition> metricDefinitions;

  public BaseAgent(OrionAgentConfig config) {
    this.config = config;
  }

  public OrionAgentConfig getConfig() {
    return config;
  }

  public boolean isUpgrading() {
    return isUpgrading;
  }

  public void setUpgrading(boolean upgrading) {
    isUpgrading = upgrading;
  }

  public void initializeHeartbeat() throws InterruptedException {
  }

  public void initializeMetricsPoll() throws Exception {
    if (jmxConnector != null) {
      try {
        getLogger().info("Closing JMX connector");
        jmxConnector.close();
      } catch (IOException e) {
        getLogger().warning("Encountered error when trying to close JMX connector: " + e);
      }
    }
    jmxConnector = getJMXServerConnection();
    if (jmxConnector != null) {
      mbs = jmxConnector.getMBeanServerConnection();
      getLogger().info("Initialized JMX connector and MBeans server connection");
    } else {
      throw new Exception("Failed to initialize JMX connector");
    }
  }

  public void endMetricsPoll() throws InterruptedException, IOException {
  }

  public void addToTasksFromDefinition(Set<MetricRetrieverTask> tasks, MetricDefinition newDef) throws Exception {
    switch (newDef.getMetricsSource()) {
      case "jmx":
        if (mbs != null) {
          tasks.add(new JMXMetricRetreiverTask(newDef, mbs));
        }
        break;
    }
  }

  public Metrics getServiceMetrics() throws Exception {
    Metrics metrics = getSystemNetworkStats();
    // use new metrics system

    Set<MetricRetrieverTask> metricRetrieverTasks = getMetricsTasks();
    getLogger().info("Retrieving " + metricRetrieverTasks.size() + " metrics");
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

  private Set<MetricRetrieverTask> getMetricsTasks() throws Exception {
    Map<String, List<String>> entityValueMap = new HashMap<>();
    return MetricUtils.getTasksFromMetricDefinitionTemplates(entityValueMap, metricDefinitions, this);
  }

  protected abstract int getMetricsPort();

  protected abstract Logger getLogger();

  protected JMXConnector getJMXServerConnection() throws Exception {
    if (!NetworkUtils.checkPort(getMetricsPort())) {
      getLogger().warning(
              "JMX connection on port " + getMetricsPort() + " to broker is not available, JMX metrics won't be captured");
      return null;
    }
    return MetricUtils.getJMXConnector("localhost", getMetricsPort());
  }

  public abstract List<String> getEntityValues(String entity);

  public Metrics getSystemNetworkStats() {
    Metrics networkMetrics = new Metrics();
    try {
      File file = new File(PROC_NET_DEV);
      if (file.exists()) {
        List<String> lines = Files.readAllLines(file.toPath());
        // ignore first two lines since they are header
        for (int i = 2; i < lines.size(); i++) {
          String line = lines.get(i);
          String[] splits = line.split("\\s+");
          String deviceName = splits[1].replace(":", "");

          List<Value> values = new ArrayList<>();
          Metric metric = new Metric("network", ImmutableMap.of("device", deviceName),
                                     System.currentTimeMillis(), values, Collections.singleton("heartbeat"));

          metric.addToValues(new Value(MetricType.COUNTER, "rxbytes", Long.parseLong(splits[2])));
          metric.addToValues(new Value(MetricType.COUNTER, "rxpackets", Long.parseLong(splits[3])));

          metric.addToValues(new Value(MetricType.COUNTER, "txbytes", Long.parseLong(splits[10])));
          metric
              .addToValues(new Value(MetricType.COUNTER, "txpackets", Long.parseLong(splits[11])));
          networkMetrics.addToMetrics(metric);
        }
      } else {
        Metric metric = new Metric("network", ImmutableMap.of("device", "missing"),
            System.currentTimeMillis(), new ArrayList<>(), Collections.singleton("heartbeat"));
        metric
            .addToValues(new Value(MetricType.COUNTER, "iterationcount", metricsIterationCount++));
        networkMetrics.addToMetrics(metric);
      }
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Failed to fetch system network stats", e);
    }
    return networkMetrics;
  }

  public abstract StatusInfo getAgentStatus() throws Exception;

  public abstract OrionCmd startService() throws Exception;

  public abstract OrionCmd stopService() throws Exception;

  public abstract OrionCmd restartService() throws Exception;

  public abstract OrionCmd updateConfigs() throws Exception;

  public abstract OrionCmd upgradeAgent() throws Exception;

  public abstract OrionCmd upgradeService() throws Exception;

  public abstract StatusInfo getServiceStatus() throws Exception;

  public abstract NodeInfo getNodeInfo() throws Exception;

  /**
   * @return the currentCommand
   */
  public NodeCmd getCurrentCommand() {
    return currentCommand;
  }

  /**
   * @param currentCommand the currentCommand to set
   */
  public void setCurrentCommand(NodeCmd currentCommand) {
    this.currentCommand = currentCommand;
  }

  public OrionCmd executeCurrentCmd() throws RequestException {
    OrionCmd cmd = null;
      if (currentCommand != null) {
        try {
          OrionAgent.TSDB_METRICS.counter("cmd.type." + currentCommand.getCmdString()).inc();
        switch (currentCommand.getCmdString()) {
          case NodeCmd.START_CMD:
            cmd = startService();
            break;
          case NodeCmd.STOP_CMD:
            cmd = stopService();
            break;
          case NodeCmd.RESTART_CMD:
            cmd = restartService();
            break;
          case NodeCmd.UPDATE_CONFIG_CMD:
            cmd = updateConfigs();
            break;
          case NodeCmd.PROBE_NETSTAT:
            cmd = probeNetstat();
            break;
          default:
            OrionAgent.TSDB_METRICS.counter("cmd.unsupported_cmd").inc();
            // unsupported command
            currentCommand.getResult().setState(CmdState.COMPLETED);
            currentCommand.getResult().setErr("Unsupported command " + currentCommand.getCmdString());
            break;
        }
      } catch (Exception e) {
         currentCommand.getResult().setState(CmdState.COMPLETED);
         currentCommand.getResult()
             .setErr("Exception when executing cmd " + currentCommand.getCmdString() + ": " + e);
       }
    }

    return cmd;
  }

  public abstract OrionCmd probeNetstat() throws Exception;

  public void initialize() throws Exception {
    initializeMetrics(config.getStatsConfigs());
    NetworkUtils.initSSLConnectionFactory(config.getConnectionConfigs());
    metricDefinitions = new Gson().fromJson(
            new String(Files.readAllBytes(
                    new File(config.getMetricsFilepath()).toPath())),
            new TypeToken<List<MetricDefinition>>(){}.getType());
  }

  protected void initializeMetrics(StatsConfiguration metricsConfigs) throws Exception {
    StatsPusher metricsPusher = new OpenTsdbStatsPusher();
    if (metricsConfigs != null && metricsConfigs.isEnabled()) {
      String hostname = metricsConfigs.getDestinationHostname();
      int port = metricsConfigs.getDestinationPort();
      int pushInterval = metricsConfigs.getPushInterval();
      metricsPusher.configure(config.getHostname(), config.getMetricsPrefix(), hostname, port, pushInterval);
      LOG.info("Starting metrics pusher with hostname " + config.getHostname() + " and metrics prefix " + config.getMetricsPrefix());
      metricsPusher.start();
    }
  }

}
