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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pinterest.orion.common.CmdResult;
import com.pinterest.orion.common.CmdResult.CmdState;
import com.pinterest.orion.core.Attribute;
import com.pinterest.orion.core.Context;
import com.pinterest.orion.core.Plugin;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.utils.OrionUUID;

public abstract class Action extends Context implements Plugin, Runnable, Future<Action> {

  private static long postRunSensorUpdateTimeout = 60L;

  public static enum Type {
                           NODE,
                           CLUSTER,
                           ALERT,
  }

  public static enum Status {
                             RUNNING,
                             FAILED,
                             NOT_STARTED,
                             SUCCEEDED,
                             CANCELLED
  }

  private OrionUUID uuid;
  private Status status = Status.NOT_STARTED;
  private long createTime;
  private long completeTime;
  private List<Action> children;
  @JsonIgnore
  private transient ActionEngine engine;
  private CmdResult result;
  private boolean parent = false;

  private Future<?> internalFuture;
  private String owner;
  @JsonIgnore
  private Set<String> postRunSensorKeys;

  public Action() {
    this.createTime = System.currentTimeMillis();
    this.uuid = new OrionUUID();
    this.children = new ArrayList<>();
    this.result = new CmdResult(uuid.toString());
    this.postRunSensorKeys = new HashSet<>();
  }

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {

  }

  @Override
  public final void run() {
    status = Status.RUNNING;
    try {
      runAction();
      if (!postRunSensorKeys.isEmpty()) {
        handlePostRunSensors();
      }
    } catch (Exception e) {
      logger().log(Level.SEVERE, "Action failed due to ", e);
      markFailed(e);
    } finally {
      this.complete();
    }
  }

  // post run of sensors are best effort, should not affect the status of the
  // action
  private void handlePostRunSensors() {
    Set<Future<?>> futures = engine.getCluster().getAutomationEngine()
        .triggerSensors(postRunSensorKeys);
    List<Exception> exceptions = new ArrayList<>();
    long currentTime = System.currentTimeMillis();
    while (!futures.isEmpty()) {
      if ((System.currentTimeMillis() - currentTime) > TimeUnit.SECONDS
          .toMillis(postRunSensorUpdateTimeout)) {
        // timeout, don't care about the sensors that aren't done.
        break;
      }
      for (Iterator<Future<?>> it = futures.iterator(); it.hasNext();) {
        Future<?> f = it.next();
        if (f.isDone()) {
          try {
            f.get();
            it.remove();
          } catch (Exception e) { // best effort for re-triggering sensors, log the errors and move
                                  // on
            exceptions.add(e);
          }
        }
      }
    }
    if (!exceptions.isEmpty()) {
      String postRunExceptions = exceptions.stream().map(Exception::toString)
          .collect(Collectors.joining("\n"));
      result.setOut(
          result.getOut() + "\n[WARNING] Failed to trigger post run sensors: " + postRunExceptions);
    }
  }

  public Attribute getAttribute(Context ctx, String key) {
    Attribute attribute = ctx.getAttribute(key);
    if (attribute != null && attribute.getPublishingSensors() != null) {
      postRunSensorKeys.addAll(attribute.getPublishingSensors());
    }
    return attribute;
  }

  public void copyAttributesFrom(Context ctx) {
    for(Map.Entry<String, Attribute> e : ctx.getAttributes().entrySet()) {
      setAttributeInternal(e.getKey(), e.getValue());
    }
  }

  public void copyAttributeFrom(Context ctx, String key) {
    copyAttributeFrom(ctx, key, key);
  }

  public void copyAttributeFrom(Context ctx, String fromKey, String toKey) {
    Attribute fromAttribute = ctx.getAttribute(fromKey);
    setAttributeInternal(toKey, fromAttribute);
  }

  public abstract void runAction() throws Exception;

  /**
   * Check if two actions are similar enough based on the definition of action. By
   * default this checks if two actions are very close in time. This is MUST be
   * implemented by an Action implementer as it allows automation circuit breaking
   * and prevents the rare chance that the same Action may get scheduled again and
   * again.
   * 
   * @param action
   * @return if two actions are the same
   */
  public boolean isSameAs(Action action) {
    if (containsAttribute("tsThreshold") && this.getClass().equals(action.getClass())) {
      long tsDiff = getCreateTime() - action.getCompleteTime();
      Long threshold = getAttribute(this, "tsThreshold").getValue();
      if (tsDiff < threshold) {
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public void complete() {
    result.setState(CmdState.COMPLETED);
    this.completeTime = System.currentTimeMillis();
    if(parent) {
      this.engine.logActionToActionAuditor(this);
    }
  }

  public void markSucceeded() {
    status = Status.SUCCEEDED;
  }

  public long getCreateTime() {
    return createTime;
  }

  public long getCompleteTime() {
    return completeTime;
  }

  /**
   * @return the children
   */
  public List<Action> getChildren() {
    return children;
  }

  /**
   * @param children the children to set
   */
  public void setChildren(List<Action> children) {
    this.children = children;
  }

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public boolean isSuccess() {
    return status == Status.SUCCEEDED;
  }

  @Override
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public boolean isDone() {
    return status == Status.SUCCEEDED || status == Status.FAILED;
  }

  @Override
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public boolean isCancelled() {
    return status == Status.CANCELLED;
  }

  public ActionEngine getEngine() {
    return engine;
  }

  public void setEngine(ActionEngine engine) {
    this.engine = engine;
  }

  public CmdResult getResult() {
    return result;
  }

  @Override
  public Action get() throws InterruptedException, ExecutionException {
    internalFuture.get();
    return this;
  }

  @Override
  public Action get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
                                                 TimeoutException {
    internalFuture.get(timeout, unit);
    return this;
  }

  @JsonIgnore
  public void setParent(boolean isParent) {
    this.parent = isParent;
  }

  @JsonIgnore
  public OrionUUID getUuid() {
    return uuid;
  }

  @JsonProperty("uuid")
  public String getUuidString() {
    return uuid.toString();
  }

  @JsonProperty("uuid")
  public void setUuidString(String uuidString) {
    this.uuid = OrionUUID.create(uuidString);
  }

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public boolean isSubmitted() {
    return internalFuture != null;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    if (internalFuture != null) {
      internalFuture.cancel(mayInterruptIfRunning);
    }
    status = Status.CANCELLED;
    result.setState(CmdState.CANCELLED);
    return true;
  }

  public void markFailed(Exception e) {
    status = Status.FAILED;
    result.appendErr(e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
  }

  public void markFailed(String msg) {
    status = Status.FAILED;
    result.appendErr(msg);
  }

  public void setResult(CmdResult result) {
    this.result = result;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public abstract Type getActionType();

  public void setInternalFuture(Future<?> internalFuture) {
    this.internalFuture = internalFuture;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof Action)) {
      return false;
    }

    return (((Action) obj).getName()).equals(this.getName()) && super.equals(obj);
  }

  /**
   * @param uuid the uuid to set
   */
  protected void setUuid(OrionUUID uuid) {
    this.uuid = uuid;
  }

  /**
   * @param createTime the createTime to set
   */
  protected void setCreateTime(long createTime) {
    this.createTime = createTime;
  }

  /**
   * @param completeTime the completeTime to set
   */
  protected void setCompleteTime(long completeTime) {
    this.completeTime = completeTime;
  }

  public Logger logger() {
    return Logger.getLogger(this.getClass().getName());
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Action [uuid=" + uuid + ", status=" + status + ", createTime=" + createTime
        + ", completeTime=" + completeTime + ", children=" + children + ", result=" + result
        + ", owner=" + owner + ", attributes=" + getAttributes() + "]";
  }

}
