package com.pinterest.orion.core.automation.sensor.memq;

import com.google.gson.Gson;
import com.pinterest.orion.core.Node;
import com.pinterest.orion.core.memq.MemqCluster;
import com.pinterest.orion.core.utils.memq.zookeeper.MemqZookeeperClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the defensive read behavior of {@link MemqClusterSensor}: cluster attributes are only
 * published when a complete, trustworthy snapshot is read, so transient ZooKeeper failures never
 * overwrite known-good state with empty / partial / cross-cluster data.
 */
public class MemqClusterSensorTest {

  private MemqClusterSensor sensor;
  private MemqCluster cluster;
  private MemqZookeeperClient client;
  private Map<String, Node> nodeMap;

  @Before
  public void setUp() {
    sensor = spy(new MemqClusterSensor());
    cluster = mock(MemqCluster.class);
    client = mock(MemqZookeeperClient.class);
    nodeMap = new HashMap<>();
    when(cluster.getClusterId()).thenReturn("scorpion01");
    when(cluster.getNodeMap()).thenReturn(nodeMap);
  }

  @Test
  public void whenClientNotConnectedSkipsUpdate() throws Exception {
    when(client.isConnected()).thenReturn(false);

    sensor.sense(cluster, client);

    verify(client, never()).getBrokerNames();
    verify(sensor, never()).setAttribute(any(), anyString(), any());
  }

  @Test
  public void whenBrokerListReadFailsSkipsUpdate() throws Exception {
    when(client.isConnected()).thenReturn(true);
    when(client.getBrokerNames()).thenThrow(new RuntimeException("zk read failed"));

    sensor.sense(cluster, client);

    verify(sensor, never()).setAttribute(any(), anyString(), any());
  }

  @Test
  public void whenBrokerDataReadFailsSkipsUpdate() throws Exception {
    when(client.isConnected()).thenReturn(true);
    when(client.getBrokerNames()).thenReturn(Arrays.asList("broker0"));
    when(client.getBrokerData("broker0")).thenThrow(new RuntimeException("partial read"));

    sensor.sense(cluster, client);

    // Incomplete snapshot: must not read topics or publish anything.
    verify(client, never()).getTopics();
    verify(sensor, never()).setAttribute(any(), anyString(), any());
  }

  @Test
  public void whenTopicReadFailsSkipsUpdate() throws Exception {
    when(client.isConnected()).thenReturn(true);
    when(client.getBrokerNames()).thenReturn(Collections.emptyList());
    when(client.getTopics()).thenThrow(new RuntimeException("topic read failed"));

    sensor.sense(cluster, client);

    verify(sensor, never()).setAttribute(any(), anyString(), any());
  }

  @Test
  public void whenNoBrokersPublishesNoBrokerContext() throws Exception {
    when(client.isConnected()).thenReturn(true);
    when(client.getBrokerNames()).thenReturn(Collections.emptyList());
    when(client.getTopics()).thenReturn(Collections.emptyList());

    sensor.sense(cluster, client);

    verify(sensor).setAttribute(cluster, MemqCluster.CLUSTER_CONTEXT, "NO BROKER");
  }

  @Test
  public void whenBrokersPresentPublishesGovernorContext() throws Exception {
    when(client.isConnected()).thenReturn(true);
    when(client.getBrokerNames()).thenReturn(Arrays.asList("broker0"));
    Broker broker = new Broker("10.0.0.1", (short) 9092, "c5.large", "us-east-1a", new HashSet<>());
    when(client.getBrokerData("broker0")).thenReturn(new Gson().toJson(broker));
    when(client.getTopics()).thenReturn(Collections.emptyList());
    when(client.getGovernorIp()).thenReturn("10.0.0.1");

    sensor.sense(cluster, client);

    verify(sensor).setAttribute(cluster, MemqCluster.CLUSTER_CONTEXT, "Governor: 10.0.0.1\n");
  }
}
