package com.pinterest.orion.core.automation.sensor.clickhouse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ExecutionException;

import com.clickhouse.client.ClickHouseClient;
import com.clickhouse.client.ClickHouseCredentials;
import com.clickhouse.client.ClickHouseException;
import com.clickhouse.client.ClickHouseNode;
import com.clickhouse.client.ClickHouseProtocol;
import com.clickhouse.client.ClickHouseResponse;
import com.clickhouse.data.ClickHouseFormat;
import com.clickhouse.data.ClickHouseRecord;

import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.core.Node;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.clickhouse.ClickHouseCluster;
import com.pinterest.orion.core.clickhouse.ClickHouseNodeInfo;
import com.pinterest.orion.core.actions.clickhouse.PublishAllNodeConfigAction;

public class ClickHouseClusterSensor extends ClickHouseSensor {

  public static final String CLUSTER_COL = "cluster";
  public static final String SHARD_NUM_COL = "shard_num";
  public static final String SHARD_WEIGHT_COL = "shard_weight";
  public static final String REPLICA_NUM_COL = "replica_num";
  public static final String PORT_COL = "port";
  public static final String HOST_NAME_COL = "host_name";

  public static final String CLUSTERS_QUERY = "SELECT * FROM system.clusters WHERE is_local=1";

  private Map<String, Object> config;

  @Override
  public String getName() {
    return "clickhouseclustersensor";
  }

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    super.initialize(config);
  }

  private void addNodesFromServerSet(ClickHouseCluster cluster, String serversetPath) throws Exception {
    List<String> lines = Files.readAllLines(new File(serversetPath).toPath());
    for (String serverStr : lines) {
      ClickHouseNodeInfo nodeInfo = new ClickHouseNodeInfo();
      nodeInfo.setNodeId(serverStr);
      nodeInfo.setClusterId(cluster.getClusterId());

      String[] splits = serverStr.split(":");
      String ip = splits[0];
      int port = Integer.parseInt(splits[1]);
      nodeInfo.setIp(ip);
      nodeInfo.setServicePort(port);

      queryShardReplicaInfoFromNode(cluster, nodeInfo, ip, port);

      Node node = cluster.getNodeMap().get(serverStr);
      if (node == null) {
        logger.info("Adding new node with info " + nodeInfo);
      } else {
        logger.info("Updating node; existing info " + node.getCurrentNodeInfo() 
          + ", new info " + nodeInfo);
      }
      cluster.addNodeWithoutAgent(nodeInfo);
    }
  }

  @Override
  public void sense(ClickHouseCluster cluster) throws Exception {
    String serversetPath = cluster.getAttribute(cluster.SERVERSET_PATH).getValue();
    addNodesFromServerSet(cluster, serversetPath);
  }

  private void queryShardReplicaInfo(
    ClickHouseNode server, ClickHouseNodeInfo nodeInfo) throws ClickHouseException {
    try (ClickHouseClient client = ClickHouseClient.newInstance(server.getProtocol());
      // each node stores the clusters info in the table system.clusters
      // here we query the shard/replica info each node stores for itself
      // TODO: build an auditor action to make sure each node stores
      // the consistent info about all the other nodes
      ClickHouseResponse response = client.read(server)
                                          .format(ClickHouseFormat.RowBinaryWithNamesAndTypes)
                                          .query(CLUSTERS_QUERY).execute().get()) {
        for (ClickHouseRecord r : response.records()) {
          // note that within each cluster, there can be smaller
          // logical clusters, e.g. created using `Replicated`
          String cluster = r.getValue(CLUSTER_COL).asString();
          int shard = r.getValue(SHARD_NUM_COL).asInteger();
          int shardWeight = r.getValue(SHARD_WEIGHT_COL).asInteger();
          int replicaNum = r.getValue(REPLICA_NUM_COL).asInteger();
          int servicePort = r.getValue(PORT_COL).asInteger();
          String hostName = r.getValue(HOST_NAME_COL).asString();

          nodeInfo.setHostname(hostName);
          nodeInfo.setServicePort(servicePort);
          nodeInfo.addShardReplicaInfo(cluster, shard, shardWeight, replicaNum);        
        }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw ClickHouseException.forCancellation(e, server);
    } catch (ExecutionException e) {
      throw ClickHouseException.of(e, server);
    }
  }

  private void queryShardReplicaInfoFromNode(
    ClickHouseCluster cluster, ClickHouseNodeInfo nodeInfo, String host, int port)
      throws ClickHouseException {
    String user = cluster.getAttribute(cluster.USER).getValue();
    String password = cluster.getAttribute(cluster.PASSWORD).getValue();

    ClickHouseNode server = ClickHouseNode.builder()
      .host(host)
      .port(ClickHouseProtocol.HTTP, port)
      .database("system").credentials(
        ClickHouseCredentials.fromUserAndPassword(user, password)
      ).build();
    queryShardReplicaInfo(server, nodeInfo);
  }
}
