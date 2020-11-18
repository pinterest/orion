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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.AuthenticationException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinterest.orion.agent.utils.MetricUtils;
import com.pinterest.orion.agent.utils.OrionCmd;
import com.pinterest.orion.common.AgentHeartbeat;
import com.pinterest.orion.common.CmdResult.CmdState;
import com.pinterest.orion.common.Metrics;
import com.pinterest.orion.common.NodeCmd;
import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.utils.NetworkUtils;

import io.dropwizard.metrics5.MetricName;

public class HeartbeatService implements Runnable {

  private static final Logger logger = Logger.getLogger(HeartbeatService.class.getName());
  private BaseAgent agent;
  private ExecutorService es;

  public HeartbeatService(BaseAgent agent) {
    this.agent = agent;
    es = Executors.newCachedThreadPool();
  }

  public void start() {
    es.submit(this);
  }

  @Override
  public void run() {
    try {
      registerWithServer(agent.getConfig());
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Registration loop failed", e);
    }
  }

  protected void registerWithServer(OrionAgentConfig config) throws Exception {
    String orionServerUrl = config.getOrionServerUrl();
    ObjectMapper objectMapper = new ObjectMapper();
    long lastServiceMetricPollTime = 0;
    OrionAgent.TSDB_METRICS.gauge(
      MetricName.build("ts").tagged("version", agent.getConfig().getVersion()),
      () -> System::currentTimeMillis
    );

    while (true) {
      try {
        logger.info("Initializing heartbeat");
        NodeInfo nodeInfo = agent.getNodeInfo();
        AgentHeartbeat heartbeat = new AgentHeartbeat();
        agent.initializeHeartbeat();
        heartbeat.setAgentStatus(agent.getAgentStatus());
        if(config.isEnableServiceMetrics()){
          long now = System.currentTimeMillis();
          if((now - lastServiceMetricPollTime) > config.getMetricsPollInterval()){
            logger.info("Collecting service metrics");
            try {
              heartbeat.setMetrics(MetricUtils.convertRegistryToMetrics(OrionAgent.HEARTBEAT_METRICS));
            } catch (Exception e) {
              OrionAgent.TSDB_METRICS.counter("metrics.failed").inc();
              logger.log(Level.SEVERE, "Failed to fetch metrics: ", e);
              heartbeat.setMetrics(new Metrics());
              heartbeat.setContainsMetrics(true);
            }
            lastServiceMetricPollTime = now;
          }
        }
        heartbeat.setServiceStatus(agent.getServiceStatus());
        heartbeat.setReadOnly(config.isReadOnly());
        if (agent.getCurrentCommand() != null) {
          heartbeat.setCurrentCmdResult(agent.getCurrentCommand().getResult());
        }
        nodeInfo.setLocaltime(System.currentTimeMillis());
        heartbeat.setNodeInfo(nodeInfo);

        String heartbeatJson = objectMapper.writeValueAsString(heartbeat);
        logger.info("Attempting to register with the server: " + heartbeatJson);
        String response = NetworkUtils.makePutRequest(orionServerUrl, heartbeatJson.getBytes());

        if ( !config.isReadOnly() && response != null) {
          NodeCmd cmd = objectMapper.readValue(response.getBytes(), NodeCmd.class);
          if (agent.getCurrentCommand() == null || (!cmd.equals(agent.getCurrentCommand())
              && agent.getCurrentCommand().getResult().hasCompleted())) {
            if (cmd.getResult().getState() == CmdState.COMPLETED) {
              agent.setCurrentCommand(cmd);
              logger.info("Server sent a stale command, skipping execution");
              OrionAgent.TSDB_METRICS.counter("cmd.stalecommand").inc();
            } else {
              logger.info("Server requesting new command to run:" + cmd);
              OrionAgent.TSDB_METRICS.counter("cmd.running").inc();
              agent.setCurrentCommand(cmd);
              OrionCmd localCmd = agent.executeCurrentCmd();
              if (localCmd != null) {
                es.submit(() -> {
                  try {
                    localCmd.get(100, TimeUnit.SECONDS);
                  } catch (Exception e) {
                    localCmd.getResult().setState(CmdState.COMPLETED);
                    localCmd.getResult().setErr("Command timed out");
                    OrionAgent.TSDB_METRICS.counter("cmd.timeout").inc();
                  }
                });
              }
            }
          }
        } else if (config.isReadOnly()){
          logger.info("Agent is in read-only mode");
        } else {
          logger.info("Empty response from server, no commands assigned");
        }
      } catch (Exception e) {
        if (e instanceof AuthenticationException) {
          logger.log(Level.WARNING,
              "Looks like the certificate might have expired, restarting agent");
          OrionAgent.TSDB_METRICS.counter("connection.authfailed").inc();
          System.exit(0);
        } else {
          logger.log(Level.SEVERE, "Failed to connect to server:" + orionServerUrl + ", retrying in 10s", e);
          OrionAgent.TSDB_METRICS.counter("connection.failed").inc();
        }
      }
      Thread.sleep(agent.getConfig().getPollInterval());
    }
  }
}
