package com.pinterest.orion.core.actions.clickhouse;

import java.util.logging.Logger;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

import java.io.File;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.OutputKeys;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.ActionEngine;
import com.pinterest.orion.core.actions.generic.GenericClusterWideAction;
import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.Node;
import com.pinterest.orion.core.clickhouse.ClickHouseNodeInfo;
import com.pinterest.orion.core.clickhouse.ClickHouseCluster;
import com.pinterest.orion.core.actions.aws.S3Utils;

import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.S3Client;


public class PublishAllNodeConfigAction extends GenericClusterWideAction.ClusterAction {
  private static final Logger logger =
    Logger.getLogger(PublishAllNodeConfigAction.class.getCanonicalName());
  
  private static final String SERVERS_TAG = "remote_servers";
  private static final String REPLICA_TAG = "replica";
  private static final String SHARD_TAG = "shard";
  private static final String HOST_TAG = "host";
  private static final String PORT_TAG = "port";

  private static final String SHARD_PLACEHOLDER = "${SHARD}";
  private static final String REPLICA_PLACEHOLDER = "${REPLICA}";

  private Element getClusterSection(Document config, String cluster) {
    NodeList clusters = config.getElementsByTagName(cluster);
    if (clusters.getLength() == 0) {
      Element serversSection = 
        (Element)(config.getElementsByTagName(SERVERS_TAG).item(0));
      Element clusterSection = config.createElement(cluster);
      serversSection.appendChild(clusterSection);
      clusters = config.getElementsByTagName(cluster);
    }
    return (Element)(clusters.item(0));
  }

  private Element getShardSection(
    Document config, Element clusterSection, int shardNum) {
    NodeList shards = clusterSection.getElementsByTagName(SHARD_TAG);
    int shardsLength = shards.getLength();
    for (int i = 0; i < shardNum - shardsLength; ++i) {
      Element shardSection = config.createElement(SHARD_TAG);
      clusterSection.appendChild(shardSection);
    }

    // assuming shard numbers start from 1
    Element shardSection = (Element)
      ((clusterSection.getElementsByTagName(SHARD_TAG)).item(shardNum-1));
    return shardSection;
  }

  private Element getReplicaSection(Document config, String host, int port) {
    Element hostSection = config.createElement(HOST_TAG);
    hostSection.appendChild(config.createTextNode(host));

    Element portSection = config.createElement(PORT_TAG);
    portSection.appendChild(config.createTextNode(Integer.toString(port)));

    Element replicaSection = config.createElement(REPLICA_TAG);
    replicaSection.appendChild(hostSection);
    replicaSection.appendChild(portSection);
    return replicaSection;
  }

  private void addReplicaToConfig(
    Document config,
    int shardNum, 
    int replicaNum, 
    String host, 
    int port, 
    String cluster) throws Exception {
    Element clusterSection = getClusterSection(config, cluster);
    Element shardSection = getShardSection(config, clusterSection, shardNum);
    Element replicaSection = getReplicaSection(config, host, port);
    shardSection.appendChild(replicaSection);
  }

  private String getConfigString(Document config) throws Exception {
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    transformerFactory.setAttribute("indent-number", 4);
    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");

    DOMSource input = new DOMSource(config);
    StringWriter sw = new StringWriter();
    transformer.transform(input, new StreamResult(sw));
    return sw.toString();
  }

  @Override
  public void runAction() throws Exception {
    ActionEngine engine = getEngine();
    ClickHouseCluster cluster = (ClickHouseCluster)engine.getCluster();
    Map<String, Node> nodeMap = cluster.getNodeMap();

    String configTemplatePath = 
      cluster.getAttribute(cluster.CONFIG_TEMPLATE_PATH).getValue();
    String configS3Bucket = 
      cluster.getAttribute(cluster.CONFIG_S3_BUCKET).getValue();

    File configTemplate = new File(configTemplatePath);
    DocumentBuilder docBuilder = 
      DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document config = docBuilder.parse(configTemplate);
    config.getDocumentElement().normalize();

    for (Node node : nodeMap.values()) {
      ClickHouseNodeInfo nodeInfo = (ClickHouseNodeInfo)node.getCurrentNodeInfo();
      String hostname = nodeInfo.getHostname();
      int port = nodeInfo.getServicePort();
      List<String> logicalClusters = nodeInfo.getLogicalClusters();
      if (logicalClusters.isEmpty()) {
        markFailed("Did not find any clusters for node " + hostname);
        return;
      }
      // right now, assume there is only one logical cluster and all
      // nodes belong to that
      String clusterName = logicalClusters.get(0);
      int shardNum = nodeInfo.getShardNum(clusterName);
      int replicaNum = nodeInfo.getReplicaNum(clusterName);

      // add this node to the config as a replica under the right shard
     addReplicaToConfig(config, shardNum, replicaNum, hostname, port, clusterName);
    }

    String configStr = getConfigString(config);
    S3Client s3 = S3Utils.getDefaultS3Client();
    for (Node node : nodeMap.values()) {
      ClickHouseNodeInfo nodeInfo = (ClickHouseNodeInfo)node.getCurrentNodeInfo();
      String clusterName = nodeInfo.getLogicalClusters().get(0);
      int shardNum = nodeInfo.getShardNum(clusterName);
      int replicaNum = nodeInfo.getReplicaNum(clusterName);

      // for the config of each node, sub in the shard and replica number for that node
      String nodeConfig = configStr.replace(SHARD_PLACEHOLDER, Integer.toString(shardNum))
        .replace(REPLICA_PLACEHOLDER, Integer.toString(replicaNum));

      String hostname = nodeInfo.getHostname();
      String key = hostname + "/config.xml";
      try {
        logger.info("Pushing updated config to S3 for node " + hostname);
        S3Utils.putObject(
          s3,
          configS3Bucket,
          key,
          nodeConfig.getBytes(),
          new HashMap<String, String>());
      } catch (S3Exception e) {
        markFailed("Pushing config to S3 for node " + hostname + " failed: " + e);
      }
    }
    s3.close();
  }

  @Override
  public String getName() {
    return "PublishAllNodeConfigAction";
  }
}
