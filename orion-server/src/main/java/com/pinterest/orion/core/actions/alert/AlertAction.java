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
package com.pinterest.orion.core.actions.alert;

import com.pinterest.orion.core.actions.Action;

public abstract class AlertAction extends Action {
  private transient AlertMessage message;

  @Override
  public final boolean cancel(boolean mayInterruptIfRunning) {
    return super.cancel(mayInterruptIfRunning);
  }

  public final void setMessage(AlertMessage message){
    this.message = message;
  }

  public final void runAction() {
    alert(message);
  }

  public abstract void alert(AlertMessage message);

  @Override
  public Type getActionType() {
    return Type.ALERT;
  }

  @Override
  public String getOwner() {
    if(message != null){
      return message.getOwner();
    }
    return super.getOwner();
  }
}
