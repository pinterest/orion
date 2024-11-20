package com.pinterest.orion.core.utils.memq.zookeeper;

import com.pinterest.orion.core.memq.MemqCluster;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.List;

public class MemqZookeeperClient {
    public static final String BROKERS = "/brokers";
    public static final String TOPICS = "/topics";
    public static final String GOVERNOR = "/governor";
    private boolean refreshZkClientOnException = true;
    private String zkUrl;
    private MemqCluster cluster;
    private CuratorFramework zkClient;

    public MemqZookeeperClient(MemqCluster cluster) throws Exception {
        this.zkUrl = cluster.getAttribute(MemqCluster.ZK_CONNECTION_STRING).getValue();
        this.cluster = cluster;
        if (cluster.getZkClient() != null) {
            this.zkClient = cluster.getZkClient();
        } else {
            refreshZkClient();
        }
    }

    public void enableRefreshZkClientOnException() {
        this.refreshZkClientOnException = true;
    }

    public void disableRefreshZkClientOnException() {
        this.refreshZkClientOnException = false;
    }

    /**
     * Create a new Zookeeper client using the connection string provided in the cluster configuration.
     * @return CuratorFramework
     * @throws Exception
     */
    private CuratorFramework createZkClient() throws Exception {
        CuratorFramework curator = CuratorFrameworkFactory.newClient(
            zkUrl,
            new ExponentialBackoffRetry(1000, 3)
        );
        curator.start();
        curator.blockUntilConnected();
        return curator;
    }

    /**
     * Refresh the Zookeeper client by creating a new one.
     * The new client is then set in the cluster object.
     * @throws Exception
     */
    public void refreshZkClient() throws Exception {
        this.zkClient = createZkClient();
        cluster.setZkClient(this.zkClient);
    }

    /**
     * Get the children of a node in Zookeeper.
     * When an exception occurs, the Zookeeper client is refreshed and the children are fetched again if the refreshZkClientOnException flag is set.
     * @param path The path of the node.
     * @return List of children node names.
     * @throws Exception
     */
    private List<String> getChildNodes(String path) throws Exception {
        try {
            return zkClient.getChildren().forPath(path);
        } catch (Exception e) {
            if (refreshZkClientOnException) {
                refreshZkClient();
                return zkClient.getChildren().forPath(path);
            } else {
                throw e;
            }
        }
    }

    /**
     * Get the data of a node in Zookeeper.
     * When an exception occurs, the Zookeeper client is refreshed and the data is fetched again if the refreshZkClientOnException flag is set.
     * @param path The path of the node.
     * @return The data of the node as a json string
     * @throws Exception
     */
    private String getNodeData(String path) throws Exception {
        try {
            return new String(zkClient.getData().forPath(path));
        } catch (Exception e) {
            if (refreshZkClientOnException) {
                refreshZkClient();
                return new String(zkClient.getData().forPath(path));
            } else {
                throw e;
            }
        }
    }

    /**
     * Get the names of the brokers in Zookeeper.
     * @return List of broker names.
     * @throws Exception
     */
    public List<String> getBrokerNames() throws Exception {
        return getChildNodes(BROKERS);
    }

    /**
     * Get the data of a broker in Zookeeper.
     * @param brokerName The name of the broker.
     * @return The data of the broker as a json string.
     * @throws Exception
     */
    public String getBrokerData(String brokerName) throws Exception {
        return getNodeData(BROKERS + "/" + brokerName);
    }

    /**
     * Get the names of the topics in Zookeeper.
     * @return List of topic names.
     * @throws Exception
     */
    public List<String> getTopics() throws Exception {
        return getChildNodes(TOPICS);
    }

    /**
     * Get the data of a topic in Zookeeper.
     * @param topicName The name of the topic.
     * @return The data of the topic as a json string.
     * @throws Exception
     */
    public String getTopicData(String topicName) throws Exception {
        return getNodeData(TOPICS + "/" + topicName);
    }

    /**
     * Get IP address the governor in Zookeeper.
     * In memq zookeeper, the governor is a node at the path "/governor". Its data is the IP address of the governor.
     * @return The IP address of the governor.
     * @throws Exception
     */
    public String getGovernorIp() throws Exception {
        try {
            return getNodeData(GOVERNOR);
        } catch (Exception e) {
            return null;
        }
    }
}
