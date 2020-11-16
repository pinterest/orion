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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.pinterest.orion.core.actions.Action;

public class TestAction {

  @Test
  public void testActionSimilarity() {
    class Act2 extends Action {

      @Override
      public String getName() {
        return "act1";
      }

      @Override
      public void runAction() throws Exception {
      }

      @Override
      public Type getActionType() {
        return Type.CLUSTER;
      }
      
    }
    class Act1 extends Action {

      @Override
      public String getName() {
        return "act1";
      }

      @Override
      public void runAction() throws Exception {
      }

      @Override
      public Type getActionType() {
        return Type.CLUSTER;
      }
      
    }
    
    Action act1 = new Act1();
    Action act2 = new Act2();
    act1.setAttribute("tsThreshold", 1000L);
    act2.setAttribute("tsThreshold", 1000L);
    act1.setCompleteTime(System.currentTimeMillis());
    
    assertFalse(act1.isSameAs(act2));
    
    act2 = new Act1();
    act2.setAttribute("tsThreshold", 1000L);
    act2.setCompleteTime(System.currentTimeMillis());
    assertTrue(act1.isSameAs(act2));
  }
  
}
