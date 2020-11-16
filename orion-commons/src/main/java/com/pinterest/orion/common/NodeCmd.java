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
package com.pinterest.orion.common;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pinterest.orion.common.CmdResult;
import com.pinterest.orion.common.CmdResult.CmdState;

public class NodeCmd extends CompletableFuture<CmdResult> implements Serializable {

  public static final String RESTART_CMD = "restartService";
  public static final String START_CMD = "startService";
  public static final String STOP_CMD = "stopService";
  public static final String UPDATE_CONFIG_CMD = "updateConfigs";
  public static final String PROBE_NETSTAT = "probeNetstat";
  private static final long serialVersionUID = 1L;
  private String uuid;
  private long timestamp;
  private String cmdString;
  private CmdResult result;

  public NodeCmd() {
    this.timestamp = System.currentTimeMillis();
  }

  public NodeCmd(String uuid, String cmdString) {
    this();
    this.uuid = uuid;
    this.result = new CmdResult(uuid);
    this.cmdString = cmdString;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeCmd) {
      return uuid.equals(((NodeCmd) obj).getUuid());
    }
    return false;
  }

  /**
   * @return the uuid
   */
  public String getUuid() {
    return uuid;
  }

  /**
   * @param uuid the uuid to set
   */
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  /**
   * @return the timestamp
   */
  public long getTimestamp() {
    return timestamp;
  }

  /**
   * @param timestamp the timestamp to set
   */
  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  /**
   * @return the cmdString
   */
  public String getCmdString() {
    return cmdString;
  }

  /**
   * @param cmdString the cmdString to set
   */
  public void setCmdString(String cmdString) {
    this.cmdString = cmdString;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

  @JsonIgnore
  @Override
  public boolean isCancelled() {
    return result != null && result.getState() == CmdState.CANCELLED;
  }

  @JsonIgnore
  @Override
  public boolean isDone() {
    return result != null && result.getState() == CmdState.COMPLETED;
  }

  public CmdResult getResult() {
    return result;
  }

  @JsonIgnore
  @Override
  public CmdResult get() throws InterruptedException, ExecutionException {
    try {
      return get(1000, TimeUnit.DAYS);
    } catch (TimeoutException e) {
      throw new InterruptedException();
    }
  }

  @JsonIgnore
  @Override
  public CmdResult get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
                                                    TimeoutException {
    long ts = System.currentTimeMillis();
    long endTime = ts + unit.toMillis(timeout);
    while (ts < endTime) {
      if (result != null
          && (result.getState() == CmdState.COMPLETED || result.getState() == CmdState.CANCELLED)) {
        return result;
      }
      Thread.sleep(1000);
      ts = System.currentTimeMillis();
    }
    throw new TimeoutException();
  }

  @JsonIgnore
  @Override
  public boolean isCompletedExceptionally() {
    return super.isCompletedExceptionally();
  }

  @JsonIgnore
  @Override
  public CmdResult getNow(CmdResult valueIfAbsent) {
    return super.getNow(valueIfAbsent);
  }

  @JsonIgnore
  @Override
  public int getNumberOfDependents() {
    return super.getNumberOfDependents();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "NodeCmd [uuid=" + uuid + ", timestamp=" + timestamp + ", cmdString=" + cmdString
        + ", result=" + result + "]";
  }

}
