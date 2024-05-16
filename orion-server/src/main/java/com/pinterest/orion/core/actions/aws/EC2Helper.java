package com.pinterest.orion.core.actions.aws;

public abstract class EC2Helper {

    /**
     * Get the number of running brokers with the given prefix
     * @param prefix the prefix of the broker name. ex: "memq-cluster1-"
     * @return the number of running brokers
     */
    public abstract int getRunningBrokerCount(String prefix);

    /**
     * Get the instance id using the full host name
     * @param fullHostName the full host name
     * @param region the region of the host
     * @return the instance id
     */
    public abstract String getHostIdUsingHostName(String fullHostName, String region);

    /**
     * Replace the host with the given instance id
     * @param hostId
     * @param clusterId
     * @return true if the replacement workflow is triggered successfully
     */
    public abstract boolean replaceHost(String hostId, String clusterId);

    /**
     * Terminate the host with the given instance id
     * @param hostId
     * @param clusterId
     * @return true if the termination workflow is triggered successfully
     */
    public abstract boolean terminateHost(String hostId, String clusterId);

    /**
     * Check if the host is pending termination
     * @param hostName
     * @return
     */
    public abstract boolean isHostPendingTermination(String hostName);

    /**
     * Check if the host is terminated
     * @param hostName
     * @return
     */
    public abstract boolean isHostTerminated(String hostName);
}
