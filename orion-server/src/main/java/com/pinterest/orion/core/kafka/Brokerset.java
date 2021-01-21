/*******************************************************************************
 * Copyright 2020 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.pinterest.orion.core.kafka;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Brokerset implements Iterable<Integer>, Serializable {

  public static final String CAPACITY_BASED = "capacity";
  public static final String PARTITION_BASED = "partition";

  private static final long serialVersionUID = 1L;
  private String brokersetAlias;
  private List<BrokersetRange> entries;
  private int partitions;   // will be final after all brokersets.json are refactored

  public Brokerset() {
  }
  
  public Brokerset(String brokersetAlias, List<BrokersetRange> entries, int partitions) {
    this.brokersetAlias = brokersetAlias;
    this.entries = entries;
    this.partitions = partitions;
  }

  @Override
  public Iterator<Integer> iterator() {
    if (entries.isEmpty()) {
      return IntStream.empty().boxed().iterator();
    }

    Stream<Integer> stream = entries.get(0).stream();
    for (int i = 1; i < entries.size(); i++) {
      stream = Stream.concat(stream, entries.get(i).stream());
    }
    return stream.iterator();
  }

  public int getSize() {
    int size = entries.stream().mapToInt(e -> e.getSize()).sum();
    return size;
  }

  public String getBrokersetAlias() {
    return brokersetAlias;
  }
  
  public List<BrokersetRange> getEntries() {
    return entries;
  }

  public int getPartitions() { return partitions; }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Brokerset) {
      return brokersetAlias.equals(((Brokerset) obj).getBrokersetAlias());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return brokersetAlias.hashCode();
  }

  public void setBrokersetAlias(String brokersetAlias) {
    this.brokersetAlias = brokersetAlias;
  }

  public void setEntries(List<BrokersetRange> entries) {
    this.entries = entries;
  }

  public void setPartitions(int partitions) {
    this.partitions = partitions;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Brokerset [brokersetAlias=" + brokersetAlias + "]";
  }

  public static class BrokersetRange implements Iterable<Integer> {

    private int startBrokerIdx;
    private int endBrokerIdx;
    
    public BrokersetRange() {
    }

    public BrokersetRange(int startBrokerIdx, int endBrokerIdx) {
      this.startBrokerIdx = startBrokerIdx;
      this.endBrokerIdx = endBrokerIdx;
    }

    @Override
    public Iterator<Integer> iterator() {
      return stream().iterator();
    }

    public Stream<Integer> stream() {
      return IntStream.rangeClosed(startBrokerIdx, endBrokerIdx).boxed();
    }

    public int getSize() {
      return (int) stream().count();
    }

    /**
     * @return the startBrokerIdx
     */
    public int getStartBrokerIdx() {
      return startBrokerIdx;
    }

    /**
     * @param startBrokerIdx the startBrokerIdx to set
     */
    public void setStartBrokerIdx(int startBrokerIdx) {
      this.startBrokerIdx = startBrokerIdx;
    }

    /**
     * @return the endBrokerIdx
     */
    public int getEndBrokerIdx() {
      return endBrokerIdx;
    }

    /**
     * @param endBrokerIdx the endBrokerIdx to set
     */
    public void setEndBrokerIdx(int endBrokerIdx) {
      this.endBrokerIdx = endBrokerIdx;
    }

  }
}
