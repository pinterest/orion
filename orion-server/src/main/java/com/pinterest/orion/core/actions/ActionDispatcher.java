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

/**
 * TaskDispatcher is a instance that dispatches {@link Action}s that were received from plugins (e.g. {@link com.pinterest.doctorkafka.plugins.operator.Operator Operators}
 * , {@link ActionHandler Actions})
 * to actions that have subscribed to the action.
 */
public interface ActionDispatcher {

  /**
   * dispatches an Action to corresponding actions
   * @param action
   * @return 
   * @throws Exception
   */
  void dispatch(Action action) throws Exception;

  /**
   * start dispatching actions
   */

  void start();

  /**
   * stop dispatching actions
   */

  void stop();

}
