package com.pinterest.orion.utils;

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
    public abstract String getInstanceIdUsingHostName(String fullHostName, String region);
}
