package com.pinterest.orion.core.actions.memq;

import com.pinterest.orion.core.actions.generic.NodeAction;
import com.pinterest.orion.core.memq.MemqCluster;
import com.pinterest.orion.core.actions.aws.EC2Helper;

public abstract class MemqBrokerReplacementAction extends NodeAction {

    private static final int POST_TERMINATION_CHECK_WAIT_TIME_MS = 10_000; // 10 seconds
    private static final int TERMINATION_CHECK_TIME_INTERVAL_MS = 60_000; // 1 minute
    private static final int TERMINATION_CHECK_TIMEOUT_MS = 1_800_000; // 30 minutes
    private static final int REPLACEMENT_CHECK_TIME_INTERVAL_MS = 60_000; // 1 minute
    private static final int REPLACEMENT_CHECK_TIMEOUT_MS = 3_600_000; // 1 hour

    @Override
    public void runAction() throws Exception {
        if (!initializeNode(false)){
            return;
        }
        // Get the cluster ID and the number of brokers before the replacement.
        String clusterId = node.getCluster().getClusterId();
        int startBrokerCount = getRunningBrokerCount(getClusterBrokerPrefix(clusterId));
        if (startBrokerCount == -1) {
            markFailed("Unable to get running broker count. Please check the orion logs for more information.");
            return;
        } else if (startBrokerCount == 0) {
            markFailed("No running brokers found in cluster " + clusterId);
            return;
        }
        // Get the host name and host ID of the node to be replaced.
        String fullHostName = node.getCurrentNodeInfo().getHostname();
        String region = node.getCluster().getAttribute(MemqCluster.CLUSTER_REGION).getValue();
        String instanceId = getEC2Helper().getInstanceIdUsingHostName(fullHostName, region);
        String hostName = fullHostName.split("\\.")[0];
        getResult().appendOut(String.format(
                "Start replacement for host %s(%s) in cluster %s. Initial broker count: %d.",
                hostName, instanceId, clusterId, startBrokerCount));
        // Replace the host via API.
        if (!getEC2Helper().replaceHost(instanceId, clusterId)) {
            markFailed(String.format("Failed to replace host %s(%s) in cluster %s.",
                    hostName, instanceId, clusterId));
        }
        long startTime = waitForHostTermination(clusterId, instanceId, hostName);
        // Wait for the replacement host to be added to the cluster.
        // If the broker count is >= the initial count, the replacement is successful.
        node.getCluster().getNodeMap().remove(node.getCurrentNodeInfo().getNodeId());
        waitForReplacementHost(clusterId, startBrokerCount, startTime);
        markSucceeded();
    }

    private void waitForReplacementHost(String clusterId, int startBrokerCount, long startTime) throws InterruptedException {
        while (true) {
            int currentBrokerCount = getRunningBrokerCount(getClusterBrokerPrefix(clusterId));
            long elapsedTime = System.currentTimeMillis() - startTime;
            if (currentBrokerCount >= startBrokerCount) {
                getResult().appendOut("Replacement host has been added to the cluster. " +
                        "Current broker count: " + currentBrokerCount);
                break;
            } else if (elapsedTime > getReplacementCheckTimeoutMs()) {
                markFailed(String.format(
                        "Timed out waiting for replacement host to be added to the cluster %s. " +
                                "Current broker count is %d.",
                        clusterId, currentBrokerCount));
                break;
            } else if (currentBrokerCount == -1) {
                markFailed("Unable to get running broker count. Please check the orion logs for more information.");
                break;
            }
            getResult().appendOut(String.format(
                    "Replacement host has not been added to the cluster %s. " +
                            "Current broker count: %d. " +
                            "Target count: %d.",
                    clusterId, currentBrokerCount, startBrokerCount));
            Thread.sleep(getReplacementCheckTimeIntervalMs());
        }
        getResult().appendOut("Successfully replace node " + nodeId + " in cluster " + clusterId);
    }

    private long waitForHostTermination(String clusterId, String instanceId, String hostName) throws InterruptedException {
        // Check if the host is pending termination.
        // The host should be in pending termination status after the API call.
        Thread.sleep(getPostTerminationCheckWaitTimeMs());
        if (!getEC2Helper().isHostPendingTermination(hostName)) {
            markFailed(String.format("Failed post termination check for host %s(%s) in cluster %s.",
                    hostName, instanceId, clusterId));
        }
        getResult().appendOut("Host " + hostName + " is in pending termination status.");
        // Wait for the host to terminate.
        long startTime = System.currentTimeMillis();
        while (true) {
            Thread.sleep(getTerminationCheckTimeIntervalMs());
            long elapsedTime = System.currentTimeMillis() - startTime;
            if (getEC2Helper().isHostTerminated(hostName)) {
                getResult().appendOut(String.format("Host %s(%s) in cluster %s has been terminated.",
                        hostName, instanceId, clusterId));
                break;
            } else if (elapsedTime > getTerminationCheckTimeoutMs()) {
                markFailed(String.format("Timed out waiting for host %s(%s) in cluster %s to terminate.",
                        hostName, instanceId, clusterId));
                break;
            }
            getResult().appendOut(String.format("Host %s(%s) in cluster %s is still terminating after %d ms.",
                    hostName, instanceId, clusterId, elapsedTime));
        }
        return startTime;
    }

    @Override
    public String getName() {
        return "MemqBrokerReplacementAction";
    }

    /**
     * Get the number of running brokers in the cluster.
     * TODO: When memq agent is ready, it will be used to get the count of running brokers.
     * @param  prefix The prefix of the broker host names.
     * @return The number of running brokers in the cluster.
     */
    protected int getRunningBrokerCount(String prefix) {
        return getEC2Helper().getRunningBrokerCount(prefix);
    }

    protected int getPostTerminationCheckWaitTimeMs() {
        return POST_TERMINATION_CHECK_WAIT_TIME_MS;
    }

    protected int getTerminationCheckTimeIntervalMs() {
        return TERMINATION_CHECK_TIME_INTERVAL_MS;
    }

    protected int getTerminationCheckTimeoutMs() {
        return TERMINATION_CHECK_TIMEOUT_MS;
    }

    protected int getReplacementCheckTimeIntervalMs() {
        return REPLACEMENT_CHECK_TIME_INTERVAL_MS;
    }

    protected int getReplacementCheckTimeoutMs() {
        return REPLACEMENT_CHECK_TIMEOUT_MS;
    }

    protected abstract EC2Helper getEC2Helper();

    protected abstract String getClusterBrokerPrefix(String clusterId);
}
