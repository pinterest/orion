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
package com.pinterest.orion.core.actions.kafka;

import static org.junit.Assert.*;

import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.Node;
import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.ActionEngine;
import com.pinterest.orion.core.actions.kafka.ConcurrentKafkaAction;
import com.pinterest.orion.core.kafka.KafkaBroker;
import com.pinterest.orion.core.kafka.KafkaCluster;
import com.pinterest.orion.core.kafka.KafkaTopicDescription;
import com.pinterest.orion.core.kafka.KafkaTopicPartitionInfo;
import com.pinterest.orion.utils.OrionConstants;
import com.google.common.collect.Sets;
import org.apache.kafka.common.TopicPartitionInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Testing Concurrent Kafka Actions
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class TestConcurrentKafkaAction {

  private static final String[] TEST_TOPICS = new String[]{"testTopic1", "testTopic2"};
  private static int MAX_NODE_COUNT = 6;

  /*
    A simple 6 broker topology with a single topic with 3 partitions

    Topic Partition assignment:
    P0: 0, 1, 3                     0 - 1 - 2
    P1: 1, 2, 4                      \ / \ /
    P2: 3, 4, 5                       3 - 4   (the dependency graph)
                                       \ /
                                        5
   */
  private static int[][] simpleAssignment = new int[][]{
      new int[]{0, 1, 3},
      new int[]{1, 2, 4},
      new int[]{3, 4, 5}
  };

  /*
    A 6 broker topology with a single topic with 3 partitions, leaving broker 0 with 0 assignments

    Topic Partition assignment:
    P0: 1, 3, 4                     0   1 - 2
    P1: 1, 2, 4                        / \ /
    P2: 3, 4, 5                       3 - 4   (the dependency graph)
                                       \ /
                                        5
   */
  private static int[][] isolatedNodeAssignment = new int[][] {
      new int[]{1, 2, 4},
      new int[]{1, 3, 4},
      new int[]{3, 4, 5}
  };

  /*
    A topology similar to the above one, but leaving broker 2, and when used together with the above one,
    should form a dependency graph same as the simpleAssignment case
   */
  private static int[][] multiTopicAdditionalAssignment = new int[][] {
      new int[]{0, 1, 3},
      new int[]{1, 3, 4},
      new int[]{3, 4, 5}
  };

  @Mock
  KafkaCluster cluster;

  ActionEngine engine;

  org.apache.kafka.common.Node[] kafkaNodes = new org.apache.kafka.common.Node[MAX_NODE_COUNT];

  @Before
  public void init() throws ExecutionException, InterruptedException {
    Map<String, Node> nodeMap = new HashMap<>();
    for (int i = 0; i < MAX_NODE_COUNT; i++) {
      String idString = Integer.toString(i);
      NodeInfo nodeInfo = new NodeInfo();
      kafkaNodes[i] = new org.apache.kafka.common.Node(i, idString, 9092);
      nodeInfo.setNodeId(idString);
      KafkaBroker broker = new KafkaBroker(cluster, nodeInfo, null);
      nodeMap.put(idString, broker);
    }

    Mockito.when(cluster.getNodeMap()).thenReturn(nodeMap);
    assertNotNull(cluster);
    engine = new TestActionEngine(cluster);
  }

  private KafkaTopicDescription getTopicAssignments(String topic, int[][] assignments){
    Map<Integer, KafkaTopicPartitionInfo> topicPartitionInfoMap = new HashMap<>();
    for (int i = 0; i < assignments.length; i++) {
      int[] partitionAssignment = assignments[i];
      int leader = partitionAssignment[0];
      List<org.apache.kafka.common.Node>
          nodeList = Arrays.stream(partitionAssignment).mapToObj(id -> kafkaNodes[id]).collect(
          Collectors.toList());
      KafkaTopicPartitionInfo ktpi = new KafkaTopicPartitionInfo(new TopicPartitionInfo(i, kafkaNodes[leader], nodeList, nodeList));
      topicPartitionInfoMap.put(i, ktpi);
    }


    return new KafkaTopicDescription(topic, false, topicPartitionInfoMap);
  }

  private void setSingleTopic() throws Exception {
    Map<String, KafkaTopicDescription> descriptions = new HashMap<>();
    descriptions.put(TEST_TOPICS[0], getTopicAssignments(TEST_TOPICS[0], simpleAssignment));
    Mockito.when(cluster.getTopicDescriptionFromKafka()).thenReturn(descriptions);
  }

  private void setIsolatedNodeSingleTopic() throws Exception {
    Map<String, KafkaTopicDescription> descriptions = new HashMap<>();
    descriptions.put(TEST_TOPICS[0], getTopicAssignments(TEST_TOPICS[0], isolatedNodeAssignment));
    Mockito.when(cluster.getTopicDescriptionFromKafka()).thenReturn(descriptions);
  }

  private void setMultiTopics() throws Exception {
    Map<String, KafkaTopicDescription> descriptions = new HashMap<>();
    descriptions.put(TEST_TOPICS[0], getTopicAssignments(TEST_TOPICS[0], isolatedNodeAssignment));
    descriptions.put(TEST_TOPICS[1], getTopicAssignments(TEST_TOPICS[1], multiTopicAdditionalAssignment));
    Mockito.when(cluster.getTopicDescriptionFromKafka()).thenReturn(descriptions);
  }

  @Test
  public void testSimpleGenerateDependencyGraph() throws Exception {
    setSingleTopic();

    ConcurrentKafkaAction action = new TestConcurrentNoOpAction();
    Map<String, Set<String>> dependencyGraph = action.generateDependencyGraph(cluster, cluster.getNodeMap().keySet());
    assertEquals(Sets.newHashSet("0", "1", "3"), dependencyGraph.get("0"));
    assertEquals(Sets.newHashSet("0", "1", "2", "3", "4"), dependencyGraph.get("1"));
    assertEquals(Sets.newHashSet("1", "2", "4"), dependencyGraph.get("2"));
    assertEquals(Sets.newHashSet("0", "1", "3", "4", "5"), dependencyGraph.get("3"));
    assertEquals(Sets.newHashSet("1", "2", "3", "4", "5"), dependencyGraph.get("4"));
    assertEquals(Sets.newHashSet("3", "4", "5"), dependencyGraph.get("5"));
  }

  @Test
  public void testIsolatedNodeGenerateDependencyGraph() throws Exception {
    setIsolatedNodeSingleTopic();

    ConcurrentKafkaAction action = new TestConcurrentNoOpAction();
    Map<String, Set<String>> dependencyGraph = action.generateDependencyGraph(cluster, cluster.getNodeMap().keySet());
    assertEquals(Sets.newHashSet("0"), dependencyGraph.get("0"));
    assertEquals(Sets.newHashSet("1", "2", "3", "4"), dependencyGraph.get("1"));
    assertEquals(Sets.newHashSet("1", "2", "4"), dependencyGraph.get("2"));
    assertEquals(Sets.newHashSet("1", "3", "4", "5"), dependencyGraph.get("3"));
    assertEquals(Sets.newHashSet("1", "2", "3", "4", "5"), dependencyGraph.get("4"));
    assertEquals(Sets.newHashSet("3", "4", "5"), dependencyGraph.get("5"));
  }

  @Test
  public void testMultiTopicGenerateDependencyGraph() throws Exception {
    setMultiTopics();

    ConcurrentKafkaAction action = new TestConcurrentNoOpAction();
    Map<String, Set<String>> dependencyGraph = action.generateDependencyGraph(cluster, cluster.getNodeMap().keySet());
    assertEquals(Sets.newHashSet("0", "1", "3"), dependencyGraph.get("0"));
    assertEquals(Sets.newHashSet("0", "1", "2", "3", "4"), dependencyGraph.get("1"));
    assertEquals(Sets.newHashSet("1", "2", "4"), dependencyGraph.get("2"));
    assertEquals(Sets.newHashSet("0", "1", "3", "4", "5"), dependencyGraph.get("3"));
    assertEquals(Sets.newHashSet("1", "2", "3", "4", "5"), dependencyGraph.get("4"));
    assertEquals(Sets.newHashSet("3", "4", "5"), dependencyGraph.get("5"));
  }

  @Test
  public void testSimpleAction() throws Exception {
    setSingleTopic();

    ConcurrentKafkaAction action = new TestConcurrentNoOpAction();
    Map<String, Set<String>> dependencyGraph = action.generateDependencyGraph(cluster, cluster.getNodeMap().keySet());
    action.initialize(Collections.singletonMap("checkingIntervalMs", 10_000));
    engine.dispatch(action);

    long startTs = System.currentTimeMillis();
    while(System.currentTimeMillis() - startTs < 41_000) {
      checkIfActionsAreDependent(action.getChildren(), dependencyGraph);
      Thread.sleep(1000);
    }
    assertEquals(Action.Status.SUCCEEDED, action.getStatus());
    for(Action child : action.getChildren()) {
      assertEquals(Action.Status.SUCCEEDED, child.getStatus());
    }
  }

  @Test
  public void testIsolatedNodeAction() throws Exception {
    setIsolatedNodeSingleTopic();

    ConcurrentKafkaAction action = new TestConcurrentNoOpAction();
    Map<String, Set<String>> dependencyGraph = action.generateDependencyGraph(cluster, cluster.getNodeMap().keySet());
    action.initialize(Collections.singletonMap("checkingIntervalMs", 10_000));
    engine.dispatch(action);

    long startTs = System.currentTimeMillis();
    while(System.currentTimeMillis() - startTs < 41_000) {
      checkIfActionsAreDependent(action.getChildren(), dependencyGraph);
      Thread.sleep(1000);
    }
    assertEquals(Action.Status.SUCCEEDED, action.getStatus());
    for(Action child : action.getChildren()) {
      assertEquals(Action.Status.SUCCEEDED, child.getStatus());
    }
  }

  @Test
  public void testMultiTopicAction() throws Exception {
    setMultiTopics();

    ConcurrentKafkaAction action = new TestConcurrentNoOpAction();
    Map<String, Set<String>> dependencyGraph = action.generateDependencyGraph(cluster, cluster.getNodeMap().keySet());
    action.initialize(Collections.singletonMap("checkingIntervalMs", 10_000));
    engine.dispatch(action);

    long startTs = System.currentTimeMillis();
    while(System.currentTimeMillis() - startTs < 41_000) {
      checkIfActionsAreDependent(action.getChildren(), dependencyGraph);
      Thread.sleep(1000);
    }
    assertEquals(Action.Status.SUCCEEDED, action.getStatus());
    for(Action child : action.getChildren()) {
      assertEquals(Action.Status.SUCCEEDED, child.getStatus());
    }
  }

  @Test
  public void testFailedActionWithNoDraining() throws Exception {
    setSingleTopic();

    Set<String> failIds = Sets.newHashSet("1");
    Set<String> remainingIds = Sets.newHashSet("3", "4");

    ConcurrentKafkaAction action = new TestConcurrentFailableNoOpAction();
    action.setAttribute("failIds", failIds);
    Map<String, Set<String>> dependencyGraph = action.generateDependencyGraph(cluster, cluster.getNodeMap().keySet());
    action.initialize(Collections.singletonMap("checkingIntervalMs", 10_000));
    engine.dispatch(action);

    long startTs = System.currentTimeMillis();
    while(System.currentTimeMillis() - startTs < 21_000) { // will fail on the second iteration
      checkIfActionsAreDependent(action.getChildren(), dependencyGraph);
      Thread.sleep(1000);
    }
    assertEquals(Action.Status.FAILED, action.getStatus());
    for(Action child : action.getChildren()) {
      String nodeId = child.getAttribute(OrionConstants.NODE_ID).getValue();
      if (failIds.contains(nodeId)) {
        assertEquals(Action.Status.FAILED, child.getStatus());
      } else if (remainingIds.contains(nodeId)){
        fail("node " + nodeId + " should not have started action since it is in remaining nodes: " + failIds);
      } else {
        assertEquals(Action.Status.SUCCEEDED, child.getStatus());
      }
    }
  }

  @Test
  public void testFailedActionWithDraining() throws Exception {
    setSingleTopic();

    Set<String> drainIds = Sets.newHashSet("2", "5");
    Set<String> failIds = Sets.newHashSet("0"); // 2, 5 should succeed after draining
    Set<String> remainingIds = Sets.newHashSet("1", "3", "4");

    TestConcurrentFailableNoOpAction action = new TestConcurrentFailableNoOpAction();
    action.setAttribute("failIds", failIds);
    Map<String, Set<String>> dependencyGraph = action.generateDependencyGraph(cluster, cluster.getNodeMap().keySet());
    action.initialize(Collections.singletonMap("checkingIntervalMs", 5_000)); // shortening for draining to happen
    engine.dispatch(action);

    long startTs = System.currentTimeMillis();
    while(System.currentTimeMillis() - startTs < 21_000) { // will fail on the first iteration, but will be draining
      checkIfActionsAreDependent(action.getChildren(), dependencyGraph);
      Thread.sleep(1000);
    }
    assertEquals(drainIds, action.currentNodes);
    assertEquals(Action.Status.FAILED, action.getStatus());
    for(Action child : action.getChildren()) {
      String nodeId = child.getAttribute(OrionConstants.NODE_ID).getValue();
      if (failIds.contains(nodeId)) {
        assertEquals(Action.Status.FAILED, child.getStatus());
      } else if (remainingIds.contains(nodeId)){
        fail("node " + nodeId + " should not have started action since it is in remaining nodes: " + failIds);
      } else {
        assertEquals(Action.Status.SUCCEEDED, child.getStatus());
      }
    }
  }

  @Test
  public void testMultipleTopic() throws Exception {
    setMultiTopics();


  }

  private boolean checkIfActionsAreDependent(List<Action> actions, Map<String, Set<String>> dependencyGraph) {
    Set<String> currentActiveNodes = new HashSet<>();
    for (Action action : actions) {
      if (action.getStatus().equals(Action.Status.RUNNING)) {
        String nodeId = action.getAttribute(OrionConstants.NODE_ID).getValue();
        currentActiveNodes.add(nodeId);
      }
    }

    if (currentActiveNodes.isEmpty()) {
      return false;
    }

    Set<String> cover = new HashSet<>();
    for (String node : currentActiveNodes) {
      if (cover.contains(node)) {
        return true;
      } else {
        cover.addAll(dependencyGraph.get(node));
      }
    }

    return false;
  }

  private static class TestNoOpAction extends Action {

    @Override
    public void runAction() throws Exception {
      Thread.sleep(9_000);
      markSucceeded();
    }

    @Override
    public Type getActionType() {
      return null;
    }

    @Override
    public String getName() {
      return null;
    }
  }

  private static class TestFailableNoOpAction extends Action {

    @Override
    public void runAction() throws Exception {
      if (containsAttribute("failIds")) {
        Set<String> failIds = getAttribute("failIds").getValue();
        String nodeId = getAttribute(OrionConstants.NODE_ID).getValue();
        if (failIds.contains(nodeId)) {
          Thread.sleep(4_000);
          markFailed("Failed");
          return;
        }
      }
      Thread.sleep(9_000);
      markSucceeded();
    }

    @Override
    public Type getActionType() {
      return null;
    }

    @Override
    public String getName() {
      return null;
    }
  }

  private static class TestConcurrentNoOpAction extends ConcurrentKafkaAction {

    TestConcurrentNoOpAction() {
      super("noop");
    }

    @Override
    protected Action getChildAction() {
      return new TestNoOpAction();
    }

    @Override
    public boolean failIfNoNodeIds() {
      return false;
    }
  }

  private static class TestConcurrentFailableNoOpAction extends ConcurrentKafkaAction {
    private Set<String> currentNodes = new HashSet<>();

    TestConcurrentFailableNoOpAction() {
      super("failable-noop");
    }

    @Override
    protected boolean handleFailure(Action failedAction, List<Action> currentActions,
                                    Set<String> remainingNodes) {
      currentNodes = currentActions.stream().map(a -> a.getAttribute(OrionConstants.NODE_ID).getValue().toString()).collect(Collectors.toSet());
      return super.handleFailure(failedAction, currentActions, remainingNodes);
    }

    @Override
    protected Action getChildAction() {
      Action action = new TestFailableNoOpAction();
      action.copyAttributeFrom(this, "failIds");
      return action;
    }

    @Override
    public boolean failIfNoNodeIds() {
      return false;
    }
  }

  private static class TestActionEngine extends ActionEngine {
    TestActionEngine(Cluster cluster) {
      super(cluster, null, null);
      actionExecutors = new ThreadPoolExecutor(1, 1, 0, TimeUnit.NANOSECONDS,
          new LinkedBlockingQueue<>());
      childActionExecutors = new ThreadPoolExecutor(SUBTASK_PARALLELISM, SUBTASK_PARALLELISM, 0,
          TimeUnit.NANOSECONDS, new LinkedBlockingQueue<>());
    }


    @Override
    public void dispatch(Action action) {
      action.setParent(true);
      action.setEngine(this);
      Future<?> future = actionExecutors.submit(action);
      action.setInternalFuture(future);
    }

    // skips config initialization since there won't be any for tests
    public void dispatchChild(Action parent, Action child) throws Exception {
      child.setOwner(parent.getOwner());
      child.setEngine(this);
      child.setParent(false);
      Future<?> future = childActionExecutors.submit(child);
      child.setInternalFuture(future);
    }
  }
}