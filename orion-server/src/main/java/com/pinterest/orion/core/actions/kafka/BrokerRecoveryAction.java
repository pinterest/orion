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
package com.pinterest.orion.core.actions.kafka;

import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.alert.AlertLevel;
import com.pinterest.orion.core.actions.alert.AlertMessage;
import com.pinterest.orion.core.actions.aws.ReplaceEC2InstanceAction;
import com.pinterest.orion.core.actions.generic.NodeAction;
import com.pinterest.orion.core.actions.generic.ServiceStabilityCheckAction;
import com.pinterest.orion.server.OrionServer;
import com.pinterest.orion.utils.OrionConstants;

import io.dropwizard.metrics5.MetricName;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.logging.Logger;

public class BrokerRecoveryAction extends NodeAction {

  private static final Logger
      logger =
      Logger.getLogger(BrokerRecoveryAction.class.getCanonicalName());
  private static final int maxRetries = 3;
  private static final int retryIntervalMilliseconds = 1000;

  public static final String ATTR_TRY_TO_RESTART_KEY = "try_restart";
  public static final String ATTR_NODE_EXISTS_KEY = "node_exists";
  public static final String ATTR_NONEXISTENT_HOST_KEY = "nonexistent_host";
  public static final String CONF_DRY_RUN_REPLACEMENT_KEY = "dry_run";
  public static final String CONF_OVERRIDE_IMAGE_KEY = "override_image";
  private boolean isDryRun = false;
  private String amiOverride = null;

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    super.initialize(config);
    if (config.containsKey(CONF_DRY_RUN_REPLACEMENT_KEY)) {
      isDryRun = Boolean.parseBoolean(config.get(CONF_DRY_RUN_REPLACEMENT_KEY).toString());
    }
    if (config.containsKey(CONF_OVERRIDE_IMAGE_KEY)) {
      amiOverride = config.get(CONF_OVERRIDE_IMAGE_KEY).toString();
    }
  }

  @Override
  public void runAction() {
    boolean nodeExists = true;
    String nodeId = getAttribute(OrionConstants.NODE_ID).getValue();
    String startNote = "BrokerRecoveryAction for " + nodeId + " started.";
    logger.info(startNote);
    getResult().appendOut(startNote);
    if(containsAttribute(ATTR_NODE_EXISTS_KEY)) {
      nodeExists = getAttribute(ATTR_NODE_EXISTS_KEY).getValue();
    }

    // base metric with tag
    MetricName metricPrefix = MetricName.build("broker_recovery_action")
        .tagged(
            "cluster", getEngine().getCluster().getClusterId(),
            "victim", nodeId,
            "dry_run", Boolean.toString(isDryRun));

    if(nodeExists) { // do some final checks only if the host exists
      if (!initializeNode(false)) {
        return;
      }

      nodeId = node.getCurrentNodeInfo().getNodeId();
      String hostname = node.getCurrentNodeInfo().getHostname();
      int port = node.getCurrentNodeInfo().getServicePort();
      boolean tryRestart = false;
      if (containsAttribute(ATTR_TRY_TO_RESTART_KEY)) {
        tryRestart = getAttribute(ATTR_TRY_TO_RESTART_KEY).getValue();
      }

      OrionServer.METRICS.counter(metricPrefix.resolve("start_recovery")).inc();

      if (tryRestart) { // restart the host, then wait for service to stabilize
        OrionServer.METRICS.counter(metricPrefix.resolve("try_restart")).inc();
        if (!isDryRun) {
          // if the restart was successful, check stability before next step
          if (tryRestartBroker(nodeId) && checkServiceStabilityAfterRestart(nodeId)) {
            getEngine().alert(AlertLevel.MEDIUM, new AlertMessage(
                "Broker " + nodeId + " on "+ getEngine().getCluster().getClusterId() + " recovered after restart",
                "Broker recovered after restart",
                "orion"
            ));
            markSucceeded();
            return;
          }
        }
      } else if (isHostReachable(hostname, port)) {
        // try pinging host if restart isn't needed, abort if host is reachable
        OrionServer.METRICS.counter(metricPrefix.resolve("host_reachable")).inc();
        // might want to do some extra checking on service level. However it would need some corner
        // case handling since brokers might have 0 traffic and no readable topics.
        getEngine().alert(AlertLevel.MEDIUM, new AlertMessage(
            "Broker " + nodeId + " on "+ node.getCluster().getClusterId() + " is reachable",
            "Broker is reachable",
            "orion"
        ));
        markFailed("Broker (" + hostname + ":" + port + ") is reachable. Aborting broker recovery action.");
        return;
      }
    }

    OrionServer.METRICS.counter(metricPrefix.resolve("start_replace")).inc();

    getResult().appendOut("Start replacing broker");

    try {
      // if dry run skip dispatching the replacement action and report success, but still set the cooldown flag for the cluster
      if (!isDryRun) {
        if (!replaceBrokerAndWait(nodeExists)) {
          OrionServer.METRICS.counter(metricPrefix.resolve("replace_failed")).inc();
          markFailed("Failed to replace node " + nodeId);
          return;
        }
      }
      OrionServer.METRICS.counter(metricPrefix.resolve("replace_success")).inc();
      markSucceeded();
    } catch (Exception e) {
      markFailed(e);
      return;
    }
  }

  protected boolean replaceBrokerAndWait(boolean nodeExists)
      throws Exception {
    Action replaceAction = newReplaceAction();
    replaceAction.copyAttributeFrom(this, OrionConstants.NODE_ID);
    replaceAction.setAttribute(ReplaceEC2InstanceAction.ATTR_CAUSE_KEY, "Broker is dead");
    replaceAction.setAttribute(ReplaceEC2InstanceAction.ATTR_NODE_EXISTS_KEY, nodeExists);
    if(!nodeExists) {
      replaceAction.copyAttributeFrom(this, ATTR_NONEXISTENT_HOST_KEY, ReplaceEC2InstanceAction.ATTR_HOSTNAME_KEY);
    }
    this.getChildren().add(replaceAction);

    getEngine().dispatchChild(this, replaceAction);
    replaceAction.get();
    return replaceAction.isSuccess();
  }

  protected boolean tryRestartBroker(String nodeId) {
    boolean restartSuccess = false;
    Action restartAction = new KafkaBrokerActions.KafkaBrokerRestartAction();
    restartAction.copyAttributeFrom(this, OrionConstants.NODE_ID);
    logger.info("Trying to restart Kafka service on node " + nodeId + " before replacement...");
    this.getChildren().add(restartAction);
    try {
      getEngine().dispatchChild(this, restartAction);
      restartAction.get();
      if (!restartAction.isSuccess()) {
        logger.warning("Failed recovery after trying to restart service on node " + nodeId
            + ", replacing Broker");
      } else {
        this.getResult()
            .appendOut("Broker recovered after restart. Will wait for service to stabilize.");
        restartSuccess = true;
      }
    } catch (Exception e) {
      logger.warning(
          "Exception happened when restarting Kafka on node " + nodeId + ", replacing broker: "
              + e);
    }
    return restartSuccess;
  }

  protected boolean checkServiceStabilityAfterRestart(String nodeId) {
    Action stabilityCheckAction = new ServiceStabilityCheckAction();
    stabilityCheckAction.copyAttributeFrom(this, OrionConstants.NODE_ID);
    this.getChildren().add(stabilityCheckAction);
    try {
      getEngine().dispatchChild(this, stabilityCheckAction);
      stabilityCheckAction.get();
      if (!stabilityCheckAction.isSuccess()) {
        logger.warning("Service is not stable on the node " + nodeId + ", replacing broker");
      } else {
        this.getResult()
            .appendOut("Service on the node has stablilized. Skipping replacement");
        logger.info("Service on the node has stabilized. Skipping replacement");
        return true;
      }
    } catch (Exception e) {
      logger.warning("The service on the node " + node.getCurrentNodeInfo().getNodeId()
          + " is failing, replacing node: " + e);
    }
    return false;
  }

  @Override
  public String getName() {
    // Different action names are required for BrokerRecoveryAction to be dispatched from same ClusterRecoveryAction.
    String nodeId = "Unknown Node";
    if (containsAttribute(OrionConstants.NODE_ID)) {
      nodeId = getAttribute(OrionConstants.NODE_ID).getValue();
    }
    return String.format(
            "BrokerRecoveryAction for %s %s",
           nodeId,
            (isDryRun ? " - Dry Run" : ""));
  }

  private boolean isHostReachable(String hostname, int port) {
    InetSocketAddress destination = new InetSocketAddress(hostname, port);
    try (Socket socket = new Socket()) {
      int retryCount = 0;
      while (true) {
        try {
          socket.connect(destination, retryIntervalMilliseconds);
          return true;
        } catch (IOException e) { // includes SocketTimeoutException
          if (retryCount == maxRetries) {
            logger.warning("Reached max retries. " + hostname + ":" + port + " is unreachable");
            return false;
          }
          logger.info("Failed to connect to " + hostname + ":" + port + ", retrying " + retryCount + "/" + maxRetries);
          retryCount++;
        }
      }
    } catch (IOException e) {
      logger.warning("Failed to close socket to " + hostname + ":" + port);
    }
    return false;
  }

  protected String getAmiOverride() {
    return amiOverride;
  }

  protected Action newReplaceAction() {
    Action replaceAction = new ReplaceEC2InstanceAction();
    if (getAmiOverride() != null) {
      replaceAction.setAttribute(ReplaceEC2InstanceAction.ATTR_AMI_KEY, getAmiOverride());
    }
    replaceAction.setAttribute(ReplaceEC2InstanceAction.ATTR_SKIP_CLUSTER_HEALTH_CHECK_KEY, true);
    return replaceAction;
  }
}
