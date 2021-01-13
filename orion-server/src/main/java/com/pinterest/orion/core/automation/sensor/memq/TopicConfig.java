package com.pinterest.orion.core.automation.sensor.memq;

import java.util.Properties;

public class TopicConfig implements Comparable<TopicConfig> {

  private long topicOrder;

  /**
   * Slot size
   */
  private int bufferSize;

  private int ringBufferSize;

  private int batchMilliSeconds = 60_000;

  private int batchSizeMB = 100;

  private int outputParallelism = 2;

  private int maxDispatchCount = 100;

  private String topic;

  private Properties outputHandlerConfig = new Properties();

  private int tickFrequencyMillis = 1000;

  private boolean enableBucketing2Processor = false;

  /**
   * file or s3
   */
  private String outputHandler;

  private volatile double inputTrafficMB = 0.0;

  public TopicConfig() {
  }

  public TopicConfig(TopicConfig config) {
    this.topicOrder = config.topicOrder;
    this.topic = config.topic;
    this.outputHandler = config.outputHandler;
    this.bufferSize = config.bufferSize;
    this.outputParallelism = config.outputParallelism;
    this.maxDispatchCount = config.maxDispatchCount;
    this.tickFrequencyMillis = config.tickFrequencyMillis;
    this.batchMilliSeconds = config.batchMilliSeconds;
    this.outputHandlerConfig = config.outputHandlerConfig;
    this.ringBufferSize = config.ringBufferSize;
    this.batchSizeMB = config.batchSizeMB;
    this.enableBucketing2Processor = config.enableBucketing2Processor;
  }

  public TopicConfig(int topicOrder,
                     int bufferSize,
                     int ringBufferSize,
                     String topic,
                     int batchSizeMB,
                     double inputTrafficMB) {
    super();
    this.topicOrder = topicOrder;
    this.bufferSize = bufferSize;
    this.ringBufferSize = ringBufferSize;
    this.topic = topic;
    this.inputTrafficMB = inputTrafficMB;
    this.batchSizeMB = batchSizeMB;
  }

  public TopicConfig(String topic, String outputHandler) {
    this.topic = topic;
    this.outputHandler = outputHandler;
  }

  @Override
  public int compareTo(TopicConfig o) {
    return Long.compare(topicOrder, o.getTopicOrder());
  }

  public long getTopicOrder() {
    return topicOrder;
  }

  public void setTopicOrder(long topicOrder) {
    this.topicOrder = topicOrder;
  }

  /**
   * @return the bufferSize
   */
  public int getBufferSize() {
    return bufferSize;
  }

  /**
   * @param bufferSize the bufferSize to set
   */
  public void setBufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
  }

  /**
   * @return the ringBufferSize
   */
  public int getRingBufferSize() {
    return ringBufferSize;
  }

  /**
   * @param ringBufferSize the ringBufferSize to set
   */
  public void setRingBufferSize(int ringBufferSize) {
    this.ringBufferSize = ringBufferSize;
  }

  /**
   * @return the batchMilliSeconds
   */
  public int getBatchMilliSeconds() {
    return batchMilliSeconds;
  }

  /**
   * @param batchMilliSeconds the batchMilliSeconds to set
   */
  public void setBatchMilliSeconds(int batchMilliSeconds) {
    this.batchMilliSeconds = batchMilliSeconds;
  }

  /**
   * @return the batchSizeMB
   */
  public int getBatchSizeMB() {
    return batchSizeMB;
  }

  /**
   * @param batchSizeMB the batchSizeMB to set
   */
  public void setBatchSizeMB(int batchSizeMB) {
    this.batchSizeMB = batchSizeMB;
  }

  public int getMaxDispatchCount() {
    return maxDispatchCount;
  }

  public void setMaxDispatchCount(int maxDispatchCount) {
    this.maxDispatchCount = maxDispatchCount;
  }

  /**
   * @return the topic
   */
  public String getTopic() {
    return topic;
  }

  /**
   * @param topic the topic to set
   */
  public void setTopic(String topic) {
    this.topic = topic;
  }

  /**
   * @return the outputHandler
   */
  public String getOutputHandler() {
    return outputHandler;
  }

  /**
   * @param outputHandler the outputHandler to set
   */
  public void setOutputHandler(String outputHandler) {
    this.outputHandler = outputHandler;
  }

  /**
   * @return the outputHandlerConfig
   */
  public Properties getOutputHandlerConfig() {
    return outputHandlerConfig;
  }

  /**
   * @param outputHandlerConfig the outputHandlerConfig to set
   */
  public void setOutputHandlerConfig(Properties outputHandlerConfig) {
    this.outputHandlerConfig = outputHandlerConfig;
  }

  /**
   * @return the outputParallelism
   */
  public int getOutputParallelism() {
    return outputParallelism;
  }

  /**
   * @param outputParallelism the outputParallelism to set
   */
  public void setOutputParallelism(int outputParallelism) {
    this.outputParallelism = outputParallelism;
  }

  /**
   * @return the inputTrafficMB
   */
  public double getInputTrafficMB() {
    return inputTrafficMB;
  }

  /**
   * @param inputTrafficMB the inputTrafficMB to set
   */
  public void setInputTrafficMB(double inputTrafficMB) {
    this.inputTrafficMB = inputTrafficMB;
  }

  public int getTickFrequencyMillis() {
    return tickFrequencyMillis;
  }

  public void setTickFrequencyMillis(int tickFrequencyMillis) {
    this.tickFrequencyMillis = tickFrequencyMillis;
  }

  public boolean isEnableBucketing2Processor() {
    return enableBucketing2Processor;
  }

  public void setEnableBucketing2Processor(boolean enableBucketing2Processor) {
    this.enableBucketing2Processor = enableBucketing2Processor;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TopicConfig) {
      return ((TopicConfig) obj).getTopic().equals(topic);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return topic.hashCode();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "TopicConfig [bufferSize=" + bufferSize + ", ringBufferSize=" + ringBufferSize
        + ", batchMilliSeconds=" + batchMilliSeconds + ", batchSizeMB=" + batchSizeMB
        + ", outputParallelism=" + outputParallelism + ", topic=" + topic + ", outputHandlerConfig="
        + outputHandlerConfig + ", outputHandler=" + outputHandler + "]";
  }

}
