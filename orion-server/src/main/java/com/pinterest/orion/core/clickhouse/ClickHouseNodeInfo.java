package com.pinterest.orion.core.clickhouse;

import com.pinterest.orion.common.NodeInfo;

import java.io.Serializable;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

class ShardReplicaInfo {
  public int shardNum;
  public int shardWeight;
  public int replicaNum;

  ShardReplicaInfo(int shardNum, int shardWeight, int replicaNum) {
    this.shardNum = shardNum;
    this.shardWeight = shardWeight;
    this.replicaNum = replicaNum;
  }

  public String toString() {
    return "{shardNum: " + shardNum + ", shardWeight: " 
      + shardWeight + " replicaNum: " + replicaNum + "}";
  }
}

public class ClickHouseNodeInfo extends NodeInfo implements Serializable {
  Map<String, ShardReplicaInfo> infoByCluster = new HashMap<>();

  public void addShardReplicaInfo(String cluster, int shardNum, int shardWeight, int replicaNum) {
    infoByCluster.put(cluster, new ShardReplicaInfo(shardNum, shardWeight, replicaNum));
  }

  public List<String> getLogicalClusters() {
    return new ArrayList<String>(infoByCluster.keySet());
  }

  public int getShardNum(String cluster) {
    return infoByCluster.get(cluster).shardNum;
  }

  public int getShardWeight(String cluster) {
    return infoByCluster.get(cluster).shardWeight;
  }

  public int getReplicaNum(String cluster) {
    return infoByCluster.get(cluster).replicaNum;
  }

  @Override
  protected String listPropertiesStr() {
    return super.listPropertiesStr() + ", infoByCluster=" + infoByCluster.toString();
  }
}
