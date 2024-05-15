package com.pinterest.orion.core.actions.memq;

import com.pinterest.orion.core.Node;
import com.pinterest.orion.core.actions.generic.NodeDecommissionAction;
import com.pinterest.orion.core.memq.MemqCluster;
import com.pinterest.orion.utils.EC2Helper;
import com.pinterest.orion.utils.TeletraanClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.util.logging.Logger;

public abstract class MemqTeletraanBrokerDecommissionAction extends NodeDecommissionAction {

    private static final int POST_TERMINATION_CHECK_WAIT_TIME_MS = 10_000; // 10 seconds
    private static final int TERMINATION_CHECK_TIME_INTERVAL_MS = 300_000; // 5 minutes
    private static final int TERMINATION_CHECK_TIMEOUT_MS = 1_800_000; // 30 minutes
    private Logger logger = Logger.getLogger(this.getClass().getName());

    @Override
    public boolean decommission(Node node) throws Exception {
        String fullHostName = node.getCurrentNodeInfo().getHostname();
        String region = node.getCluster().getAttribute(MemqCluster.CLUSTER_REGION).getValue();
        String clusterId = node.getCluster().getClusterId();
        String instanceId = getEC2Helper().getInstanceIdUsingHostName(fullHostName, region);
        String hostName = fullHostName.split("\\.")[0];
        // Terminate the host via Teletraan API.
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        if (!getTeletraanClient().terminateHost(httpClient, instanceId, clusterId, null)) {
            markFailed(String.format("Failed to terminate host %s(%s) in cluster %s.",
                    hostName, instanceId, clusterId));
            return false;
        }
        // Check if the host is pending termination.
        // The host should be in pending termination status after the API call.
        Thread.sleep(getPostTerminationCheckWaitTimeMs());
        if (!getTeletraanClient().isInstancePendingTermination(httpClient, hostName, null)) {
            markFailed(String.format("Failed post termination check for host %s(%s) in cluster %s.",
                    hostName, instanceId, clusterId));
            return false;
        }
        getResult().appendOut("Host " + hostName + " is in pending termination status.");
        // Wait for the host to terminate. Check every 5 minutes for 30 minutes.
        // If the host is still not terminated after 30 minutes, mark the action as failed.
        long startTime = System.currentTimeMillis();
        while (true) {
            Thread.sleep(getTerminationCheckTimeIntervalMs());
            long elapsedTime = System.currentTimeMillis() - startTime;
            if (getTeletraanClient().isInstanceTerminated(httpClient, hostName, null)) {
                getResult().appendOut(String.format("Host %s(%s) in cluster %s has been terminated.",
                        hostName, instanceId, clusterId));
                break;
            } else if (elapsedTime > getTerminationCheckTimeoutMs()) {
                markFailed(String.format("Timed out waiting for host %s(%s) in cluster %s to terminate.",
                        hostName, instanceId, clusterId));
                return false;
            }
            getResult().appendOut(String.format("Host %s(%s) in cluster %s is still terminating after %d ms.",
                    hostName, instanceId, clusterId, elapsedTime));
        }
        // Remove the node from the Orion cluster and mark the action as succeeded
        super.decommission(node);
        markSucceeded();
        return true;
    }

    @Override
    public String getName() {
        return "MemqTeletraanBrokerDecommissionAction";
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

    protected abstract EC2Helper getEC2Helper();

    protected abstract TeletraanClient getTeletraanClient();
}
