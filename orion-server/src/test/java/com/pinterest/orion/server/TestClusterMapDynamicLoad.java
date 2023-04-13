package com.pinterest.orion.server;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.demo.DemoCluster;

public class TestClusterMapDynamicLoad {

  @Test
  public void testClassLoad() {
    Map<String, Class<? extends Cluster>> test = ClusterTypeMap.clusterTypeMap;
    assertEquals(DemoCluster.class, test.get("demo"));
  }

}
