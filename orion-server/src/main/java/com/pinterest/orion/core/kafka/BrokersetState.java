package com.pinterest.orion.core.kafka;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BrokersetState {
    /**
     * The brokersetAlias is the alias of the brokerset.
     */
    private String brokersetAlias;
    /**
     * The instanceType is the type of the brokerset.
     */
    private String instanceType;
    /**
     * The brokersetRanges are the ranges of brokerset that are in the brokerset.
     * The brokersetRanges are obtained from the brokerset configuration file.
     */
    private List<List<Integer>> brokersetRanges = new ArrayList<>();
    /**
     * The brokerIds are the ids of running broker that are in the brokerset.
     * The brokerIds are obtained from the cluster state.
     */
    private List<String> brokerIds = new ArrayList<>();
    /**
     * Save the broker status in printable format (string).
     */
    private Map<String, Double> rawBrokersetStatus;
    /**
     * Save the broker status in raw format (number).
     */
    private Map<String, String> brokersetStatus;
    /**
     * The constructor of BrokersetState.
     * @param brokersetAlias
     */
    public BrokersetState(String brokersetAlias) {
        this.brokersetAlias = brokersetAlias;
    }
    /**
     * The constructor of BrokersetState.
     * @param brokersetAlias
     * @param brokersetRanges
     * @param brokerIds
     */
    public BrokersetState(String brokersetAlias, List<List<Integer>> brokersetRanges, List<String> brokerIds) {
        this.brokersetAlias = brokersetAlias;
        this.brokersetRanges = brokersetRanges;
        this.brokerIds = brokerIds;
    }
    /**
     * The method is used to get the size of brokerset.
     * The size of brokerset is the number of brokers in the brokerset.
     * It may not be equal to the sum of brokersetRanges if some brokers are not running.
     * @return the size of brokerset.
     */
    public int getSize() {
        return brokerIds.size();
    }
    /**
     * The method is used to get the alias of brokerset.
     * @return the alias of brokerset.
     */
    public String getBrokersetAlias() {
        return brokersetAlias;
    }
    /**
     * The method is used to set the alias of brokerset.
     * @param brokersetAlias
     */
    public void setBrokersetAlias(String brokersetAlias) {
        this.brokersetAlias = brokersetAlias;
    }
    /**
     * The method is used to get the ranges of brokerset.
     * @return the ranges of brokerset.
     */
    public List<List<Integer>> getBrokersetRanges() {
        return brokersetRanges;
    }
    /**
     * The method is used to add the range of brokerset.
     * Each range is a list of two integers.
     * @param brokerRange
     * @throws IllegalArgumentException
     */
    public void addBrokerRange(List<Integer> brokerRange) throws IllegalArgumentException {
        if (brokerRange.size() != 2) {
            throw new IllegalArgumentException(
                "The size of brokerRange should be 2 for brokerset " + brokersetAlias + ".");
        }
        brokersetRanges.add(brokerRange);
    }
    /**
     * The method is used to set the ranges of brokerset.
     * @param brokersetRanges
     * @throws IllegalArgumentException
     */
    public void setBrokersetRanges(List<List<Integer>> brokersetRanges) throws IllegalArgumentException {
        for (List<Integer> brokerRange : brokersetRanges) {
            if (brokerRange.size() != 2) {
                throw new IllegalArgumentException(
                    "The size of brokerRange should be 2 for brokerset " + brokersetAlias + ".");
            }
        }
        this.brokersetRanges = brokersetRanges;
    }
    /**
     * The method is used to get the ids of brokers in the brokerset.
     * @return brokerIds
     */
    public List<String> getBrokerIds() {
        return brokerIds;
    }
    /**
     * The method is used to set the ids of brokers in the brokerset.
     * @param brokerIds
     */
    public void setBrokerIds(List<String> brokerIds) {
        this.brokerIds = brokerIds;
    }
    public void setRawBrokersetStatus(Map<String, Double> rawBrokersetStatus) {
        this.rawBrokersetStatus = rawBrokersetStatus;
    }
    public Map<String, Double> getRawBrokersetStatus() {
        return rawBrokersetStatus;
    }
    public void setBrokersetStatus(Map<String, String> brokersetStatus) {
        this.brokersetStatus = brokersetStatus;
    }
    public Map<String, String> getBrokersetStatus() {
        return brokersetStatus;
    }
    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }
    public String getInstanceType() {
        return instanceType;
    }
}
