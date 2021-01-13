package com.pinterest.orion.core.automation.sensor.memq;

import java.util.HashSet;
import java.util.Set;

public class Broker implements Comparable<Broker> {

  private String brokerIP;
  private short brokerPort;
  private String instanceType;
  private String locality;
  private Set<TopicConfig> assignedTopics = new HashSet<>();
  private int totalNetworkCapacity;

  public Broker() {
  }

  public Broker(String brokerIP,
                short brokerPort,
                String instanceType,
                String locality,
                Set<TopicConfig> assignedTopics) {
    this.brokerIP = brokerIP;
    this.brokerPort = brokerPort;
    this.instanceType = instanceType;
    this.locality = locality;
    this.assignedTopics = assignedTopics;
  }

  private int getUsedNetworkCapacity() {
    return assignedTopics.stream().mapToInt(t -> (int) t.getInputTrafficMB()).sum();
  }

  public int getAvailableCapacity() {
    return totalNetworkCapacity - getUsedNetworkCapacity();
  }

  public int getTotalNetworkCapacity() {
    return totalNetworkCapacity;
  }

  public void setTotalNetworkCapacity(int totalNetworkCapacity) {
    this.totalNetworkCapacity = totalNetworkCapacity;
  }

  public String getBrokerIP() {
    return brokerIP;
  }

  public void setBrokerIP(String brokerIP) {
    this.brokerIP = brokerIP;
  }

  public short getBrokerPort() {
    return brokerPort;
  }

  public void setBrokerPort(short brokerPort) {
    this.brokerPort = brokerPort;
  }

  public String getInstanceType() {
    return instanceType;
  }

  public void setInstanceType(String instanceType) {
    this.instanceType = instanceType;
  }

  public String getLocality() {
    return locality;
  }

  public void setLocality(String locality) {
    this.locality = locality;
  }

  public Set<TopicConfig> getAssignedTopics() {
    return assignedTopics;
  }

  public void setAssignedTopics(Set<TopicConfig> assignedTopics) {
    this.assignedTopics = assignedTopics;
  }

  @Override
  public int hashCode() {
    return brokerIP.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Broker) {
      return ((Broker) obj).getBrokerIP().equals(brokerIP);
    }
    return false;
  }

  @Override
  public int compareTo(Broker o) {
    return brokerIP.compareTo(o.getBrokerIP());
  }

  @Override
  public String toString() {
    return "Broker [brokerIP=" + brokerIP + ", locality=" + locality + ",availableCapacity:"
        + getAvailableCapacity() + "]";
  }

}
