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
import java.util.Date;

public class CmdResult implements Serializable {

  private static final long serialVersionUID = 1L;

  public static enum CmdState {
                               INIT,
                               RUNNING,
                               COMPLETED,
                               CANCELLED
  }

  private CmdState state = CmdState.INIT;
  private short exitCode = -10000;
  private StringBuilder out = new StringBuilder();
  private StringBuilder err = new StringBuilder();
  private String uuid;

  public CmdResult() {
  }

  public CmdResult(String uuid, CmdState state, short exitCode, String out, String err) {
    this.uuid = uuid;
    this.state = state;
    this.exitCode = exitCode;
    this.out = new StringBuilder(out);
    this.err = new StringBuilder(err);
  }

  public CmdResult(String uuid) {
    this.uuid = uuid;
  }

  /**
   * @return the exitCode
   */
  public short getExitCode() {
    return exitCode;
  }

  /**
   * @param exitCode the exitCode to set
   */
  public void setExitCode(short exitCode) {
    this.exitCode = exitCode;
  }

  /**
   * @return the out
   */
  public String getOut() {
    return out == null ? null : out.toString();
  }

  /**
   * @param out the out to set
   */
  public void setOut(String out) {
    if(out == null) {
      this.out = new StringBuilder();
    } else {
      this.out = new StringBuilder(out);
    }
  }

  /**
   * @return the err
   */
  public String getErr() {
    return err == null ? null : err.toString();
  }

  /**
   * @param err the err to set
   */
  public void setErr(String err) {
    if(err == null) {
      this.err = new StringBuilder();
    } else {
      this.err = new StringBuilder(err);
    }
  }

  /**
   * @return the state
   */
  public CmdState getState() {
    return state;
  }

  /**
   * @param state the state to set
   */
  public void setState(CmdState state) {
    this.state = state;
  }

  /**
   * @return is cmd has completed
   */
  public boolean hasCompleted() {
    return state == CmdState.COMPLETED;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public void appendOut(String newOutput) {
    out.append(new Date() + "\t" + newOutput + "\n");
  }

  public void appendErr(String newError) {
    err.append(new Date() + "\t" + newError + "\n");
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "CmdResult [state=" + state + ", exitCode=" + exitCode + ", out=" + out + ", err=" + err
        + "]";
  }

}
