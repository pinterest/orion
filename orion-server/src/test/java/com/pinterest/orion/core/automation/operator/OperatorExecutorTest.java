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

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.pinterest.orion.core.actions.Action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OperatorExecutorTest {
  private static Action[] actionList;
  private static Action[] shadowActionList;

  @BeforeClass
  public static void init() {
    actionList = new Action[10];
    shadowActionList = new Action[10];
    for (int i = 0; i < 10; i++){
      actionList[i] = new TestAction(Integer.toString(i));
      shadowActionList[i] = new TestAction(Integer.toString(i));
    }
  }

  @Test
  public void testUniqueActionUUID() {
    for(int i = 0; i < actionList.length; i++){
      assertEquals(shadowActionList[i],actionList[i]);
      assertNotEquals(shadowActionList[i].getUuid(), actionList[i].getUuid());
    }
  }

  @Test
  public void testCommitShadowListEqualLength() {
    List<Action> actualList;
    List<Action> shadowList;
    List<Action> resActualList;
    List<Action> emptyList = new ArrayList<>();
    OperatorExecutor executor = new OperatorExecutor(null, null, null);

    actualList = Arrays.asList(actionList[0], actionList[1], actionList[2]);
    shadowList = Arrays.asList(shadowActionList[0], shadowActionList[1], shadowActionList[2]);
    executor.setActualList(new ArrayList<>(actualList));
    executor.setShadowList(new ArrayList<>(shadowList));
    executor.setNextIdx(0);
    executor.commitShadowList();
    assertEquals(0, executor.getNextIdx());
    assertEquals(emptyList, executor.getShadowList());
    resActualList = executor.getActualList();
    assertEquals(shadowList, resActualList);
    for(int i = 0; i < resActualList.size(); i++){
      assertEquals(shadowList.get(i).getUuid(), resActualList.get(i).getUuid());
      assertNotEquals(actualList.get(i).getUuid(), resActualList.get(i).getUuid());
    }

    executor.setActualList(new ArrayList<>(actualList));
    executor.setShadowList(new ArrayList<>(shadowList));
    executor.setNextIdx(1);
    executor.commitShadowList();
    assertEquals(1, executor.getNextIdx());
    assertEquals(emptyList, executor.getShadowList());
    resActualList = executor.getActualList();
    assertEquals(shadowList, resActualList);
    assertNotEquals(shadowList.get(0).getUuid(), resActualList.get(0).getUuid());
    assertEquals(actualList.get(0).getUuid(), resActualList.get(0).getUuid());
    for(int i = 1; i < resActualList.size() - 1; i++){
      assertEquals(shadowList.get(i).getUuid(), resActualList.get(i).getUuid());
      assertNotEquals(actualList.get(i).getUuid(), resActualList.get(i).getUuid());
    }

    executor.setActualList(new ArrayList<>(actualList));
    executor.setShadowList(new ArrayList<>(shadowList));
    executor.setNextIdx(2);
    executor.commitShadowList();
    assertEquals(2, executor.getNextIdx());
    assertEquals(emptyList, executor.getShadowList());
    resActualList = executor.getActualList();
    assertEquals(shadowList, resActualList);
    for(int i = 0; i < 2; i++){
      assertNotEquals(shadowList.get(i).getUuid(), resActualList.get(i).getUuid());
      assertEquals(actualList.get(i).getUuid(), resActualList.get(i).getUuid());
    }
    assertEquals(shadowList.get(2).getUuid(), resActualList.get(2).getUuid());
    assertNotEquals(actualList.get(2).getUuid(), resActualList.get(2).getUuid());
  }

  @Test
  public void testCommitShadowListAny() {
    List<Action> actualList;
    List<Action> shadowList;
    List<Action> resActualList;
    List<Action> emptyList = new ArrayList<>();
    OperatorExecutor executor = new OperatorExecutor(null, null, null);

    // original: shadow = [1, 3], actual = [0, 1, 2]
    actualList = Arrays.asList(actionList[0], actionList[1], actionList[2]);
    shadowList = Arrays.asList(shadowActionList[1], shadowActionList[3]);

    // nextIdx = 0
    executor.setActualList(new ArrayList<>(actualList));
    executor.setShadowList(new ArrayList<>(shadowList));
    executor.setNextIdx(0);
    executor.commitShadowList(); // result: shadow = [], actual = [1,3], nextIdx = 0
    assertEquals(0, executor.getNextIdx());
    assertEquals(emptyList, executor.getShadowList());
    resActualList = executor.getActualList();
    assertEquals(shadowList, resActualList);
    assertEquals(shadowActionList[1].getUuid(), resActualList.get(0).getUuid());
    assertEquals(shadowActionList[3].getUuid(), resActualList.get(1).getUuid());

    // nextIdx = 2
    executor.setActualList(new ArrayList<>(actualList));
    executor.setShadowList(new ArrayList<>(shadowList));
    executor.setNextIdx(2);
    executor.commitShadowList(); // result: shadow = [], actual = [1,3], nextIdx = 1
    assertEquals(1, executor.getNextIdx());
    assertEquals(emptyList, executor.getShadowList());
    resActualList = executor.getActualList();
    assertEquals(shadowList, resActualList);
    assertEquals(actionList[1].getUuid(), resActualList.get(0).getUuid());
    assertEquals(shadowActionList[3].getUuid(), resActualList.get(1).getUuid());

    // original: shadow = [1, 2, 4], actual = [0, 1, 2]
    actualList = Arrays.asList(actionList[0], actionList[1], actionList[2]);
    shadowList = Arrays.asList(shadowActionList[1], shadowActionList[2], shadowActionList[4]);

    // nextIdx = 2
    executor.setActualList(new ArrayList<>(actualList));
    executor.setShadowList(new ArrayList<>(shadowList));
    executor.setNextIdx(2);
    executor.commitShadowList(); // result: shadow = [], actual = [1,2,4], nextIdx = 2
    assertEquals(1, executor.getNextIdx());
    assertEquals(emptyList, executor.getShadowList());
    resActualList = executor.getActualList();
    assertEquals(shadowList, resActualList);
    assertEquals(actionList[1].getUuid(), resActualList.get(0).getUuid());
    assertEquals(shadowActionList[2].getUuid(), resActualList.get(1).getUuid());
    assertEquals(shadowActionList[4].getUuid(), resActualList.get(2).getUuid());

    // nextIdx = 3
    executor.setActualList(new ArrayList<>(actualList));
    executor.setShadowList(new ArrayList<>(shadowList));
    executor.setNextIdx(3);
    executor.commitShadowList(); // result: shadow = [], actual = [1,2,4], nextIdx = 2
    assertEquals(2, executor.getNextIdx());
    assertEquals(emptyList, executor.getShadowList());
    resActualList = executor.getActualList();
    assertEquals(shadowList, resActualList);
    assertEquals(actionList[1].getUuid(), resActualList.get(0).getUuid());
    assertEquals(actionList[2].getUuid(), resActualList.get(1).getUuid());
    assertEquals(shadowActionList[4].getUuid(), resActualList.get(2).getUuid());

    // original: shadow = [1, 4, 5], actual = [0, 1, 2, 4, 9]
    actualList = Arrays.asList(actionList[0], actionList[1], actionList[2], actionList[4], actionList[9]);
    shadowList = Arrays.asList(shadowActionList[1], shadowActionList[4], shadowActionList[5]);

    // nextIdx = 2
    executor.setActualList(new ArrayList<>(actualList));
    executor.setShadowList(new ArrayList<>(shadowList));
    executor.setNextIdx(2);
    executor.commitShadowList(); // result: shadow = [], actual = [1,4,5], nextIdx = 1
    assertEquals(1, executor.getNextIdx());
    assertEquals(emptyList, executor.getShadowList());
    resActualList = executor.getActualList();
    assertEquals(shadowList, resActualList);
    assertEquals(actionList[1].getUuid(), resActualList.get(0).getUuid());
    assertEquals(shadowActionList[4].getUuid(), resActualList.get(1).getUuid());
    assertEquals(shadowActionList[5].getUuid(), resActualList.get(2).getUuid());

    // nextIdx = 4
    executor.setActualList(new ArrayList<>(actualList));
    executor.setShadowList(new ArrayList<>(shadowList));
    executor.setNextIdx(4);
    executor.commitShadowList(); // result: shadow = [], actual = [1,4,5], nextIdx = 1
    assertEquals(1, executor.getNextIdx());
    assertEquals(emptyList, executor.getShadowList());
    resActualList = executor.getActualList();
    assertEquals(shadowList, resActualList);
    assertEquals(actionList[1].getUuid(), resActualList.get(0).getUuid());
    assertEquals(shadowActionList[4].getUuid(), resActualList.get(1).getUuid());
    assertEquals(shadowActionList[5].getUuid(), resActualList.get(2).getUuid());
  }

  private static class TestAction extends Action {

    private String name;
    public TestAction(String name){
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public void runAction() {
      // TODO Auto-generated method stub
      
    }
  }
}