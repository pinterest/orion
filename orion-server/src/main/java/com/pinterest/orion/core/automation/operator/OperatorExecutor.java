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
package com.pinterest.orion.core.automation.operator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.Plugin;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.ActionDispatcher;
import com.pinterest.orion.core.actions.ActionEngine;

public class OperatorExecutor implements ActionDispatcher, Runnable, Plugin {
  private static Logger logger = Logger.getLogger(OperatorExecutor.class.getCanonicalName());
  private static String CONF_OPERATOR_EXEC_INTERVAL_SEC_KEY = "operator_exec_interval";
  private static String AUTOMATION_ACTION_OWNER = "orion";

  private long confOperatorExecIntervalSec = 30;
  private Cluster cluster;
  private ScheduledExecutorService executorThread = Executors.newSingleThreadScheduledExecutor();
  private ActionEngine actionEngine;
  private List<OperatorContainer> operatorContainers;

  private int nextIdx = 0;
  private List<Action> actualList = new ArrayList<>();
  private List<Action> shadowList = new ArrayList<>();

  public OperatorExecutor(Cluster cluster, List<OperatorContainer> operatorContainers, ActionEngine actionEngine){
    this.cluster = cluster;
    this.operatorContainers = operatorContainers;
    this.actionEngine = actionEngine;
  }

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    if(config.containsKey(CONF_OPERATOR_EXEC_INTERVAL_SEC_KEY)){
      confOperatorExecIntervalSec = (Integer) config.get(CONF_OPERATOR_EXEC_INTERVAL_SEC_KEY);
    }
    for(OperatorContainer operatorContainer : operatorContainers){
      operatorContainer.getOperator().setDispatcher(this);
    }
  }

  /* There are 3 stages in the run method:
    1. Evaluate all the operators, collect the actions they dispatch inside the shadow queue
    2. Compare the shadow queue with the actual queue and determine whether a swap is needed
    3. Dispatch the head of the next action in the actual queue and wait until the action is done
    4. Go to 1.

   */
  @Override
  public void run() {
    // maintenance mode
    if(cluster.isUnderMaintenance()) {
      logger.log(Level.INFO, "Cluster " + cluster.getClusterId() + " is under maintenance mode.");
      executorThread.schedule(this, confOperatorExecIntervalSec, TimeUnit.SECONDS);
      return;
    }
    // go through all operators and intercept the dispatch calls
    for(OperatorContainer operatorContainer : operatorContainers){
      try {
        operatorContainer.operate(cluster);
      } catch (Exception e){
        logger.log(Level.SEVERE, "Operator " + operatorContainer.getOperator().getName() + " failed to operate", e);
      }
    }

    // compare the shadow list and actual list and determine where the nextIdx is in the shadow queue
    // then swap actual list with shadow list, and clear shadow list
    commitShadowList();

    // execute the next action in the actual list
    boolean success = false;
    if(nextIdx < actualList.size()){
      Action nextAction = actualList.get(nextIdx);
      nextAction.setOwner(AUTOMATION_ACTION_OWNER);
      try {
        success = actionEngine.dispatchNow(nextAction);
        if (!success) {
          logger.info("Failed to dispatch action " + nextAction.getName() + " since there are existing actions in the ActionEngine");
        } else {
          logger.info("Dispatched action: " + nextAction.getName() + " on " + cluster.getClusterId());
          nextIdx++;
        }
      } catch (Exception e) {
        logger.severe("Failed to dispatch action " + nextAction.getName() + " from operator executor :" + e);
        nextAction.markFailed(e);
      }
    }

    // re-evaluate operators immediately if the actual list is not empty, otherwise run with fix interval
    if(!actualList.isEmpty() && success && nextIdx < actualList.size()){
      executorThread.schedule(this, 0, TimeUnit.SECONDS);
    } else {
      executorThread.schedule(this, confOperatorExecIntervalSec, TimeUnit.SECONDS);
    }
  }

  @Override
  public void dispatch(Action action) throws Exception {
    shadowList.add(action);
  }

  @Override
  public void start() {
    executorThread.schedule(this, confOperatorExecIntervalSec, TimeUnit.SECONDS);
  }

  @Override
  public void stop() {
    executorThread.shutdown();
  }

  /**
   * Compare actual list and shadow list to find out where the nextIdx of the shadowQueue should be
   * 1. start from the beginning of the actual list and find the first element that matches the head
   * of the shadow list
   * 2. Find the longest consecutive sublist in the actual list before nextIdx. Swap all elements in
   * the shadow list with corresponding elements in the actual list. These actions will be seen as
   * done in the shadow list, so we need to persist that execution state. Also use shadowNextIdx to
   * keep track of the first action in the shadow list that hasn't been executed.
   * 3. Swap the actual list and shadow list
   * 4. Set the nextIdx to shadowNextIdx
   * 5. Empty the shadow list for the next run
   */
  void commitShadowList() {
    int shadowNextIdx = 0;
    for(int i = 0, j = 0; i < nextIdx && j < shadowList.size(); i++){
      if(actualList.get(i).equals(shadowList.get(j))){
        // replace the shadow list's element with actual list's element to persist execution state
        shadowList.set(j, actualList.get(i));
        j++;
        shadowNextIdx++;
      } else if(j != 0){
        break;
      }
    }
    List<Action> tmpList = shadowList;
    shadowList = actualList;
    actualList = tmpList;
    nextIdx = shadowNextIdx;
    shadowList.clear();
  }

  @VisibleForTesting
  int getNextIdx() {
    return nextIdx;
  }

  @VisibleForTesting
  void setNextIdx(int nextIdx) {
    this.nextIdx = nextIdx;
  }

  @VisibleForTesting
  List<Action> getActualList() {
    return actualList;
  }

  @VisibleForTesting
  void setActualList(List<Action> actualList) {
    this.actualList = actualList;
  }

  @VisibleForTesting
  List<Action> getShadowList() {
    return shadowList;
  }

  @VisibleForTesting
  void setShadowList(List<Action> shadowList) {
    this.shadowList = shadowList;
  }

  @Override
  public String getName() {
    return cluster.getName() + "-OperatorExecutor";
  }
}
