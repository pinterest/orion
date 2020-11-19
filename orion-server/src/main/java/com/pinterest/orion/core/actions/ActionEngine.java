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
package com.pinterest.orion.core.actions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.actions.alert.Alert;
import com.pinterest.orion.core.actions.alert.AlertFactory;
import com.pinterest.orion.core.actions.alert.AlertLevel;
import com.pinterest.orion.core.actions.alert.AlertMessage;
import com.pinterest.orion.core.actions.audit.ActionAuditor;
import com.pinterest.orion.core.actions.schema.ActionSchema;
import com.pinterest.orion.core.utils.OrionUUID;

public class ActionEngine implements ActionDispatcher, Runnable {
  private static final int TASK_EXPIRE = 86400_000;

  private static final Logger LOG = Logger.getLogger(ActionEngine.class.getCanonicalName());
  @JsonIgnore
  private transient ScheduledExecutorService selfRunner;
  @JsonIgnore
  protected transient ExecutorService actionExecutors;
  @JsonIgnore
  protected transient ExecutorService childActionExecutors;
  @JsonIgnore
  protected transient ExecutorService alertExecutors;
  private SortedMap<OrionUUID, Action> trackedActionsMap;
  @JsonIgnore
  private SortedMap<OrionUUID, AlertMessage> alertsMap;
  @JsonIgnore
  private transient Cluster cluster;
  @JsonIgnore
  private ActionAuditor actionAuditor;
  @JsonIgnore
  private ActionFactory actionFactory;
  @JsonIgnore
  private AlertFactory alertFactory;

  private final AtomicInteger activeCounter = new AtomicInteger();

  public ActionEngine(Cluster cluster,
                      ActionFactory actionFactory,
                      AlertFactory alertFactory,
                      ActionAuditor actionAuditor) {
    this.cluster = cluster;
    this.actionFactory = actionFactory;
    this.alertFactory = alertFactory;
    this.actionAuditor = actionAuditor;
  }

  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    trackedActionsMap = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
    alertsMap = new ConcurrentSkipListMap<>(Comparator.reverseOrder());

    actionExecutors = new ControlledActionExecutor(1, 1, 0, TimeUnit.NANOSECONDS,
        new LinkedBlockingQueue<>(), activeCounter);
    childActionExecutors = Executors.newCachedThreadPool();
    alertExecutors = Executors.newCachedThreadPool();
    selfRunner = Executors.newSingleThreadScheduledExecutor();
  }

  @Override
  public void dispatch(Action action) {
    validateParentAction(action);
    initializeAction(action);
    action.setParent(true);
    synchronized (activeCounter) {
      dispatchInternal(action);
    }
  }

  public boolean dispatchNow(Action action) {
    validateParentAction(action);
    initializeAction(action);
    action.setParent(true);
    if (activeCounter.get() == 0) {
      synchronized (activeCounter) {
        if (activeCounter.get() == 0) {
          dispatchInternal(action);
          return true;
        }
      }
    }
    return false;
  }

  private void dispatchInternal(Action action) {
    alert(AlertLevel.MEDIUM,
        new AlertMessage("Action triggered on cluster " + cluster.getClusterId(),
            "Action " + action.getName() + " scheduled on cluster " + cluster.getClusterId(),
            action.getOwner()));
    Future<?> future = actionExecutors.submit(action);
    action.setInternalFuture(future);
    trackedActionsMap.put(action.getUuid(), action);
    activeCounter.incrementAndGet();
  }

  public void dispatchChild(Action parent, Action child) throws Exception {
    initializeAction(child);
    child.setOwner(parent.getOwner());
    child.setParent(false);
    Future<?> future = childActionExecutors.submit(child);
    child.setInternalFuture(future);
  }

  public void alert(Alert alert, AlertMessage message) {
    this.alertsMap.put(message.getUuid(), message);
    alert.setMessage(message);
    initializeAction(alert);
    alertExecutors.submit(alert);
  }

  protected void alert(AlertMessage message,
                       List<Alert> alerts) throws PluginConfigurationException {
    this.alertsMap.put(message.getUuid(), message);
    for (Alert alert : alerts) {
      alert.setMessage(message);
      alertExecutors.submit(alert);
    }
  }

  public void alert(AlertLevel level, AlertMessage message) {
    try {
      alert(message, alertFactory.getAlertInstancesOfLevel(this.cluster.getClusterId(), level));
    } catch (Exception e) {
      // TODO log metric and page
      LOG.log(Level.SEVERE, "Failed to send alert", e);
    }
  }

  private void validateParentAction(Action action) {
    if (!actionFactory.isActionEnabledOnCluster(cluster.getClusterId(),
        action.getClass().getName())) {
      alert(AlertLevel.MEDIUM,
          new AlertMessage("Action blocked on cluster " + cluster.getClusterId(),
              "Action " + action.getName() + " is blocked on cluster " + cluster.getClusterId(),
              action.getOwner()));
      throw new IllegalArgumentException(
          action.getClass().getName() + " is not allowed for cluster:" + cluster.getClusterId());
    }
    validateRequiredFields(action);
  }

  private void validateRequiredFields(Action action) {
    if (action.getOwner() == null) {
      throw new RejectedExecutionException("Missing required field owner");
    }
  }

  private void initializeAction(Action action) throws IllegalArgumentException {
    try {
      Map<String, Object> config = getActionConfiguration(action.getClass());
      action.initialize(config);
      if (action.getEngine() == null) {
        action.setEngine(this);
      }
    } catch (PluginConfigurationException pce) {
      LOG.log(Level.SEVERE, "Failed to initialize action", pce);
      throw new IllegalArgumentException(
          "Failed to initialize action " + action.getClass().getName(), pce);
    }
  }

  public void logActionToActionAuditor(Action action) {
    if (actionAuditor != null) {
      actionAuditor.logAction(this.cluster, action);
    }
  }

  @Override
  public void start() {
    selfRunner.scheduleWithFixedDelay(this, 60, 60, TimeUnit.SECONDS);
  }

  @Override
  public void stop() {
    selfRunner.shutdownNow();
  }

  @Override
  public void run() {
    for (Iterator<Entry<OrionUUID, Action>> iterator = trackedActionsMap.entrySet()
        .iterator(); iterator.hasNext();) {
      Entry<OrionUUID, Action> entry = iterator.next();
      if (entry.getValue().isDone() && entry.getValue().getCompleteTime() > 0
          && (System.currentTimeMillis() - entry.getValue().getCompleteTime()) > TASK_EXPIRE) {
        iterator.remove();
        LOG.info(
            "Remove old action:" + entry.getValue() + " since it was completed over 24 hrs ago");
        // TODO backup the action to Kafka for auditing?
      }
    }
  }

  public Map<String, Object> getActionConfiguration(Class<? extends Action> actionClass) {
    return actionFactory.getActionConfiguration(cluster.getClusterId(), actionClass);
  }

  public Cluster getCluster() {
    return cluster;
  }

  @JsonIgnore
  public Map<OrionUUID, Action> getTrackedActionsMap() {
    return trackedActionsMap;
  }

  public List<Action> getTrackedActionsList() {
    return new ArrayList<>(trackedActionsMap.values());
  }

  @JsonIgnore
  public List<Action> getRunningActionsList() {
    return trackedActionsMap.values().stream().filter(action -> !action.isDone())
        .collect(Collectors.toList());
  }

  public Action getAction(String orionUuidString) throws Exception {
    return this.getAction(OrionUUID.create(orionUuidString));
  }

  public Action getAction(OrionUUID uuid) {
    return trackedActionsMap.get(uuid);
  }

  public List<AlertMessage> getAlertsList() {
    return new ArrayList<>(this.alertsMap.values());
  }

  public AlertMessage getAlert(String orionUuidString) throws IllegalArgumentException {
    return getAlert(OrionUUID.create(orionUuidString));
  }

  public AlertMessage getAlert(OrionUUID uuid) {
    return alertsMap.get(uuid);
  }

  public List<ActionSchema> getActionSchemas() throws Exception {
    return actionFactory.getEnabledActionSchemas(this.cluster.getClusterId());
  }

  private static class ControlledActionExecutor extends ThreadPoolExecutor {

    private final AtomicInteger activeCounter;

    public ControlledActionExecutor(int corePoolSize,
                                    int maximumPoolSize,
                                    long keepAliveTime,
                                    TimeUnit unit,
                                    BlockingQueue<Runnable> workQueue,
                                    AtomicInteger activeCounter) {
      super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
      this.activeCounter = activeCounter;
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
      super.afterExecute(r, t);
      activeCounter.getAndDecrement();
    }
  }
}
