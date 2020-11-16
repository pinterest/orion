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
package com.pinterest.orion.core.actions.generic;

import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.ActionEngine;
import com.pinterest.orion.core.actions.alert.AlertMessage;
import com.pinterest.orion.utils.OrionConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.stream.Collectors;

public abstract class ConcurrentAction extends GenericClusterWideAction.ClusterAction {
  private String actionName;
  private static final String CONF_MAX_CONCURRENCY_KEY = "maxConcurrency";
  private static final String ATTR_MAX_CONCURRENCY_KEY = CONF_MAX_CONCURRENCY_KEY;
  private static final String CONF_CHECKING_INTERVAL_MS_KEY = "checkingIntervalMs";
  private int maxConcurrency = 3; // default concurrency = 3
  protected long checkingIntervalMs = 30_000;  // default interval = 30 seconds;

  public ConcurrentAction(String actionName) {
    this.actionName = actionName;
  }

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    super.initialize(config);
    if (config.containsKey(CONF_MAX_CONCURRENCY_KEY)) {
      maxConcurrency = Integer.parseInt(config.get(CONF_MAX_CONCURRENCY_KEY).toString());
    }
    if (config.containsKey(CONF_CHECKING_INTERVAL_MS_KEY)) {
      checkingIntervalMs = Long.parseLong(config.get(CONF_CHECKING_INTERVAL_MS_KEY).toString());
    }
  }

  @Override
  public void runAction() throws Exception {
    // override concurrency configs if set in attributes
    if (containsAttribute(ATTR_MAX_CONCURRENCY_KEY)) {
      try {
        Object val = getAttribute(ATTR_MAX_CONCURRENCY_KEY).getValue();
        if (val instanceof String) {
          maxConcurrency = Integer.parseInt((String) val);
        } else if (val instanceof Integer){
          maxConcurrency = (Integer) val;
        }
      } catch (NumberFormatException nfe) {
        logger().log(Level.SEVERE, "Failed to parse attribute: ", nfe);
        markFailed("Failed to parse integer of the attribute " + ATTR_MAX_CONCURRENCY_KEY + ":" + nfe.getMessage());
        return;
      }
    }

    Cluster cluster = getEngine().getCluster();
    Collection<String> actionNodes = getActionNodeIds(cluster);
    if (actionNodes == null) {
      return;
    }

    List<Action> currentActions = new ArrayList<>();
    Set<String> remainingNodes = new HashSet<>(actionNodes);
    Set<String> currentCover = new HashSet<>();

    Map<String, Set<String>> dependencyGraph;
    try {
      dependencyGraph = generateDependencyGraph(cluster, actionNodes);
      dependencyGraph.forEach((k, v) -> logger().fine("Node: " + k + " size: " + v.size() + ", cover: " + v));
    } catch (Exception e) {
      logger().log(Level.SEVERE, "Failed to generate cover map for cluster: " + cluster.getClusterId(), e);
      markFailed(e);
      return;
    }
    while  (!isCancelled()) {
      currentCover.clear();
      if (!processCurrentActionsAndGenerateCover(currentActions, remainingNodes, dependencyGraph, currentCover)) {
        return;
      }

      if (remainingNodes.isEmpty() && currentActions.isEmpty()) {
        break;
      }

      generateChildActions(currentActions, remainingNodes, dependencyGraph, currentCover);
      Thread.sleep(checkingIntervalMs);
    }

    if (isCancelled()) {
      handleCancel(currentActions, remainingNodes);
    }

    try {
      AlertMessage message = new AlertMessage(getName() + " completed on cluster " + cluster.getName(), "",
          this.getOwner());
      getEngine().alert(ActionEngine.AlertLevel.MEDIUM, message);
    } catch (Exception e) {
      getResult().appendErr("\nFailed to send alert");
    }

    markSucceeded();
  }

  /**
   * Iterate through the current actions, removing ones that are done while doing error handling if necessary.
   * Populates the currentCover with the nodes of the current active actions.
   * @param currentActions actions still running from the previous iteration
   * @param remainingNodes nodes that haven't had the action executed yet
   * @param dependencyGraph graph of dependencies between the action nodes
   * @param currentCover set of nodes that are already covered by current actions
   * @return whether the action should terminate (e.g. due to failures)
   * @throws InterruptedException
   * @throws ExecutionException
   */
  protected boolean processCurrentActionsAndGenerateCover(List<Action> currentActions,
                                                          Set<String> remainingNodes,
                                                          Map<String, Set<String>> dependencyGraph,
                                                          Set<String> currentCover)
      throws InterruptedException, ExecutionException {
    Iterator<Action> itr = currentActions.iterator();
    while (itr.hasNext()) {
      Action action = itr.next();
      String nodeId = action.getAttribute(OrionConstants.NODE_ID).getValue();
      if (action.isDone()) {
        action.get();
        itr.remove();
        if (!action.isSuccess() && handleFailure(action, currentActions, remainingNodes)) {
          return false;
        }
      } else {
        currentCover.addAll(dependencyGraph.get(nodeId));
      }
    }
    return true;
  }

  /**
   *
   * @param currentActions list of current actions
   * @param remainingNodes nodes that haven't had the action executed yet
   * @param dependencyGraph graph of dependencies between the action nodes
   * @param currentCover set of nodes that are already covered by current actions
   * @throws Exception
   */
  protected void generateChildActions(List<Action> currentActions, Set<String> remainingNodes,
                                      Map<String, Set<String>> dependencyGraph, Set<String> currentCover)
      throws Exception {
    List<String> sortedRemainingNodes = remainingNodes.stream()
        .sorted() // alphabetical order
        .sorted(Comparator.comparing(n -> dependencyGraph.get(n).size())) // sort by size
        .collect(Collectors.toList());
    int beforeRemainingNodesSize = remainingNodes.size();
    for (String n : sortedRemainingNodes) {
      if (currentActions.size() >= maxConcurrency) {
        break;
      } else if (!currentCover.contains(n)) {
        currentCover.addAll(dependencyGraph.get(n));
        remainingNodes.remove(n);

        Action childAction = getChildAction();
        childAction.copyAttributesFrom(this);
        childAction.setAttribute(OrionConstants.NODE_ID, n);

        getChildren().add(childAction);
        currentActions.add(childAction);

        getEngine().dispatchChild(this, childAction);
      }
    }
    if (remainingNodes.size() != beforeRemainingNodesSize) {
      getResult().appendOut("Remaining nodes: " + remainingNodes);
      logger().info("Remaining nodes: " + remainingNodes + " current nodes: " + getCurrentNodesFromActions(currentActions));
    }
  }

  protected abstract Action getChildAction();

  /**
   * Generate the dependency graph
   * @param cluster the current cluster
   * @param actionNodes the node IDs of the nodes that need the action to be performed
   * @return a adjacency list of each node in actionNodes
   * @throws Exception
   */
  protected abstract Map<String, Set<String>> generateDependencyGraph(Cluster cluster, Collection<String> actionNodes) throws Exception;

  @Override
  public boolean failIfNoNodeIds() {
    return true;
  }


  /**
   * @param failedAction action that failed
   * @param currentActions actions that are currently active (running or done), excluding failedAction
   * @param remainingNodes node ids of nodes that haven't started the actions yet
   * @return whether the current concurrent action should be terminated
   */
  protected boolean handleFailure(Action failedAction, List<Action> currentActions,
                                  Set<String> remainingNodes) {
    String clusterId = getEngine().getCluster().getClusterId();
    List<String> failedNodes = new ArrayList<>();
    List<String> currentNodes = new ArrayList<>();
    failedNodes.add(failedAction.getAttribute(OrionConstants.NODE_ID).getValue());
    // draining current actions
    getEngine().alert(ActionEngine.AlertLevel.MEDIUM, new AlertMessage(
        "Concurrent action failed on " + clusterId + ", draining current actions...",
        "Failed on brokers " + failedNodes + ", draining action on brokers " + getCurrentNodesFromActions(currentActions) + ", remaining brokers: " + remainingNodes,
        getOwner()));
    while (true) {
      currentNodes.clear();
      Iterator<Action> itr = currentActions.iterator();
      while(itr.hasNext()) {
        Action a = itr.next();
        String nodeId = a.getAttribute(OrionConstants.NODE_ID).getValue();
        if (a.isDone()) {
          try {
            itr.remove();
            a.get();
            if(!a.isSuccess()) {
              failedNodes.add(nodeId);
            }
          } catch (Exception e) {
            failedNodes.add(nodeId);
          }
        } else {
          currentNodes.add(nodeId);
        }
      }
      if (currentActions.isEmpty()) {
        break;
      }
      getResult().appendErr("Failed on brokers " + failedNodes + ", draining action on brokers " + currentNodes);
      try {
        Thread.sleep(checkingIntervalMs);
      } catch (InterruptedException ie) {
        logger().log(Level.SEVERE, "interrupted", ie);
        getEngine().alert(ActionEngine.AlertLevel.MEDIUM, new AlertMessage(
            "Concurrent action failed on " + clusterId + ", draining interrupted...",
            "Failed on brokers " + failedNodes + ", was draining action on brokers " + currentNodes,
            getOwner()));
        markFailed("Failed to complete draining due to " + ie.getMessage());
      }
    }
    getEngine().alert(ActionEngine.AlertLevel.MEDIUM, new AlertMessage(
        "Concurrent action failed on " + clusterId + ", draining complete",
        "Failed on brokers: " + failedNodes + ", remaining brokers: " + remainingNodes,
        getOwner()
    ));
    markFailed("Failed to complete action on " + failedNodes + ", remaining nodes that haven't performed the action: " + remainingNodes);
    return true;
  }


  /**
   *
   * @param currentActions actions that are currently active (running or done)
   * @param remainingNodes node ids of nodes that haven't started the actions yet
   */
  protected void handleCancel(List<Action> currentActions, Set<String> remainingNodes) {
    List<String> currentNodes = new ArrayList<>();
    for(Action a : currentActions) {
      a.cancel(true);
      currentNodes.add(a.getAttribute(OrionConstants.NODE_ID).getValue());
    }
    getEngine().alert(ActionEngine.AlertLevel.MEDIUM, new AlertMessage(
        "Concurrent action cancelled on " + getEngine().getCluster().getClusterId(),
        "Concurrent action cancelled with pending nodes: " + currentNodes + ", remaining nodes: " + remainingNodes,
        getOwner()
    ));
    getResult().appendErr("Action cancelled");
  }


  private Set<Object> getCurrentNodesFromActions(List<Action> currentActions) {
    return currentActions.stream().map(a -> a.getAttribute(OrionConstants.NODE_ID).getValue()).collect(
        Collectors.toSet());
  }

  @Override
  public String getName() {
    String name = "Concurrent-" + actionName;
    if (containsAttribute(OrionConstants.NODE_IDS)) {
      List<String> nodeIds = getAttribute(OrionConstants.NODE_IDS).getValue();
      name += " on " + nodeIds.size() + " nodes";
    }
    return name;
  }
}
