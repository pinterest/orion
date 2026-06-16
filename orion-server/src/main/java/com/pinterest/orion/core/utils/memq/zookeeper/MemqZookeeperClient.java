package com.pinterest.orion.core.utils.memq.zookeeper;

import com.pinterest.orion.core.memq.MemqCluster;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;

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
     *
     * The chroot suffix is applied as a Curator namespace rather than as a
     * ZooKeeper connection-string chroot. A connection-string chroot is dropped by Curator's
     * EnsembleTracker whenever the ensemble emits a dynamic reconfiguration event, which silently
     * re-points reads at the root of the (shared) ensemble and leaks data across clusters. A Curator
     * namespace is applied client-side and survives reconfiguration/session loss, keeping every read
     * scoped to this cluster.
     * @return CuratorFramework
     * @throws Exception
     */
    private CuratorFramework createZkClient() throws Exception {
        CuratorFramework curator = CuratorFrameworkFactory.builder()
            .connectString(parseConnectString(zkUrl))
            .retryPolicy(new ExponentialBackoffRetry(1000, 3))
            .namespace(parseNamespace(zkUrl))
            .build();
        curator.start();
        curator.blockUntilConnected();
        return curator;
    }

    /**
     * Extract the host:port list from a zk connection string, dropping any chroot suffix.
     * e.g. "h1:2181,h2:2181/memq/cluster01" -> "h1:2181,h2:2181"
     */
    static String parseConnectString(String zkUrl) {
        int slashIndex = zkUrl.indexOf('/');
        return slashIndex >= 0 ? zkUrl.substring(0, slashIndex) : zkUrl;
    }

    /**
     * Extract the chroot path from a zk connection string as a Curator namespace (no leading slash),
     * or null when there is no chroot. e.g. "h1:2181/memq/cluster01" -> "memq/cluster01"
     */
    static String parseNamespace(String zkUrl) {
        int slashIndex = zkUrl.indexOf('/');
        if (slashIndex < 0) {
            return null;
        }
        String namespace = zkUrl.substring(slashIndex + 1);
        return namespace.isEmpty() ? null : namespace;
    }

    /**
     * Refresh the Zookeeper client by creating a new one.
     * The new client is then set in the cluster object and the previous client is closed to avoid
     * leaking ZooKeeper connections/sessions across refreshes.
     * @throws Exception
     */
    public void refreshZkClient() throws Exception {
        CuratorFramework oldClient = this.zkClient;
        this.zkClient = createZkClient();
        cluster.setZkClient(this.zkClient);
        if (oldClient != null) {
            CloseableUtils.closeQuietly(oldClient);
        }
    }

    /**
     * @return true if the underlying client currently has a live connection to ZooKeeper.
     */
    public boolean isConnected() {
        return zkClient != null && zkClient.getZookeeperClient().isConnected();
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
