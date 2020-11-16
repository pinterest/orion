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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.pinterest.orion.core.Cluster;

public class OperatorContainer {
  private static Logger logger = Logger.getLogger(OperatorContainer.class.getName());
  private Operator operator;
  private volatile boolean previousSuccess = true;
  private volatile String previousOutput = "";
  private volatile Exception previousError;

  public OperatorContainer(Operator operator) {
    this.operator = operator;
  }

  public void operate(Cluster cluster) {
    try {
      operator.setMessage("");
      operator.operate(cluster);
      previousSuccess = true;
      previousError = null;
    } catch (Exception e) {
      previousSuccess = false;
      previousError = e;
      logger.log(Level.SEVERE, "Operator " + operator.getName() + " failed", e);
    }
    previousOutput = operator.getMessage();
  }

  public Operator getOperator() {
    return operator;
  }

  public boolean isPreviousSuccess() {
    return previousSuccess;
  }

  public String getPreviousOutput() {
    return previousOutput;
  }

  public Exception getPreviousError() {
    return previousError;
  }
}
