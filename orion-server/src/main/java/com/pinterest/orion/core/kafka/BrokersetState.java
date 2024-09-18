package com.pinterest.orion.core.kafka;


import java.util.ArrayList;
import java.util.List;

public class BrokersetState {
    private String brokersetAlias;
    private List<List<Integer>> brokersetRanges = new ArrayList<>();
    private List<String> brokerIds;

    public BrokersetState(String brokersetAlias) {
        this.brokersetAlias = brokersetAlias;
    }

    public BrokersetState(String brokersetAlias, List<List<Integer>> brokersetRanges, List<String> brokerIds) {
        this.brokersetAlias = brokersetAlias;
        this.brokersetRanges = brokersetRanges;
        this.brokerIds = brokerIds;
    }

    public int getSize() {
        return brokerIds.size();
    }

    public String getBrokersetAlias() {
        return brokersetAlias;
    }

    public void setBrokersetAlias(String brokersetAlias) {
        this.brokersetAlias = brokersetAlias;
    }

    public List<List<Integer>> getBrokersetRanges() {
        return brokersetRanges;
    }

    public void addBrokerRange(List<Integer> brokerRange) {
        brokersetRanges.add(brokerRange);
    }

    public void setBrokersetRanges(List<List<Integer>> brokersetRanges) {
        this.brokersetRanges = brokersetRanges;
    }

    public List<String> getBrokerIds() {
        return brokerIds;
    }

    public void setBrokerIds(List<String> brokerIds) {
        this.brokerIds = brokerIds;
    }
}
