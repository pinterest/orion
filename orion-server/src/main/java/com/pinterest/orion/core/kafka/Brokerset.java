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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.gson.stream.MalformedJsonException;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.pinterest.orion.core.kafka.Brokerset.BrokersetRange.BrokersetRangeDeserializer;

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

  public Brokerset applyBrokerOverrides(BrokersetRangeMap map) {
    List<BrokersetRange> ranges = entries;
    List<BrokersetRange> outRanges = new ArrayList<>();
    for (BrokersetRange range : ranges) {
      outRanges.addAll(range.applyMapping(map));
    }
    return new Brokerset(brokersetAlias, outRanges, partitions);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Brokerset [brokersetAlias=" + brokersetAlias + "]";
  }

  @JsonDeserialize(using = BrokersetRangeDeserializer.class)
  public static class BrokersetRange implements Iterable<Integer> {

    private int startBrokerIdx;
    private int endBrokerIdx;

    public BrokersetRange(int startBrokerIdx, int endBrokerIdx) {
      if (startBrokerIdx > endBrokerIdx) {
        throw new IllegalArgumentException("Brokerset Ranges must come in pairs where 'startBrokerIdx' <= 'endBrokerIdx'\n" +
                "There is a broker set that includes range startBrokerIdx: " + startBrokerIdx + " and endBrokerIdx: " + endBrokerIdx);
      }
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
     * @return the endBrokerIdx
     */
    public int getEndBrokerIdx() {
      return endBrokerIdx;
    }

    public Boolean overlaps(BrokersetRange other) {
      return this.endBrokerIdx >= other.startBrokerIdx && this.startBrokerIdx <= other.endBrokerIdx;
    }
    public Boolean overlaps(Integer value) {
      return this.startBrokerIdx <= value && this.endBrokerIdx >= value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      BrokersetRange range = (BrokersetRange) o;
      return startBrokerIdx == range.startBrokerIdx && endBrokerIdx == range.endBrokerIdx;
    }

    @Override
    public int hashCode() {
      return Objects.hash(startBrokerIdx, endBrokerIdx);
    }

    public List<BrokersetRange> applyMapping(BrokersetRangeMap map) {
      List<BrokersetRange> outRanges = new ArrayList<>();
      if (map.overlaps(this)) {
        BrokersetRange inRange = map.getInRange();
        int start = startBrokerIdx;
        int end = endBrokerIdx;
        int outStart = -1;
        BrokersetRange outRange = map.getOutRange();
        if (!inRange.overlaps(start)) {
          outRanges.add(new BrokersetRange(start, inRange.getStartBrokerIdx()-1));
          outStart = outRange.startBrokerIdx;
        } else {
          outStart = outRange.startBrokerIdx + (start - inRange.startBrokerIdx);
        }
        if (inRange.overlaps(end)) {
          int outEnd = outRange.endBrokerIdx - (inRange.endBrokerIdx - end);
          outRanges.add(new BrokersetRange(outStart, outEnd));
        } else {
          outRanges.add(new BrokersetRange(outStart, outRange.getEndBrokerIdx()));
          outRanges.add(new BrokersetRange(inRange.getEndBrokerIdx()+1, end));
        }
      }
      else {
        outRanges.add(this);
      }
      return outRanges;
    }
    public static class BrokersetRangeDeserializer extends JsonDeserializer<BrokersetRange> {
      @Override
      public BrokersetRange deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
              throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        int startBrokerIdx;
        int endBrokerIdx;
        try {
          startBrokerIdx = node.get("startBrokerIdx").asInt();
          endBrokerIdx = node.get("endBrokerIdx").asInt();
        } catch (NullPointerException e) {
          throw new MalformedJsonException("There was a problem parsing a brokerset\n" +
                  "Brokerset range map should take the form\n" +
                  "{\n" +
                  "  \"startBrokerIdx\": X,\n" +
                  "  \"endBrokerIdx\": Y\n" +
                  "}");
        }
        return new BrokersetRange(startBrokerIdx, endBrokerIdx);
      }
    }
  }

  @JsonDeserialize(using = BrokersetRangeMapDeserializer.class)
  public static class BrokersetRangeMap {
    /**
     * Maps a range from: startBrokerIn -> startBrokerOut and endBrokerIn -> endBrokerOut.
     * With the intent to replace all broker ranges on this cluster that include nodes between startIn and endIn.
     */

    private BrokersetRange in;
    private BrokersetRange out;

    public BrokersetRangeMap(int startBrokerIn, int endBrokerIn, int startBrokerOut, int endBrokerOut) {
      this.in = new BrokersetRange(startBrokerIn, endBrokerIn);
      this.out = new BrokersetRange(startBrokerOut, endBrokerOut);
    }

    public BrokersetRange getInRange() {
      return in;
    }
    public BrokersetRange getOutRange() {
      return out;
    }

    public Boolean overlaps(BrokersetRange range) {
      return range.overlaps(in);
    }
  }

  public static class BrokersetRangeMapDeserializer extends JsonDeserializer<BrokersetRangeMap> {
    @Override
    public BrokersetRangeMap deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
      JsonNode node = jsonParser.getCodec().readTree(jsonParser);
      int startBrokerIn;
      int endBrokerIn;
      int startBrokerOut;
      int endBrokerOut;
      try {
        startBrokerIn = node.get("startBrokerIn").asInt();
        endBrokerIn = node.get("endBrokerIn").asInt();
        startBrokerOut = node.get("startBrokerOut").asInt();
        endBrokerOut = node.get("endBrokerOut").asInt();
      } catch (NullPointerException e) {
        throw new MalformedJsonException("There was a problem parsing the Brokerset Override\n" +
                "Brokerset range map should take the form\n" +
                "{\n" +
                "  \"startBrokerIn\": W,\n" +
                "  \"endBrokerIn\": X,\n" +
                "  \"startBrokerOut\": Y,\n" +
                "  \"endBrokerOut\": Z\n" +
                "}");
      }

      if (Math.abs(endBrokerIn - startBrokerIn) != Math.abs(endBrokerOut - startBrokerOut)) {
        throw new MalformedJsonException("The override map file included mismatched ranges.\n" +
                "the range between startBrokerIn and endBrokerIn should be " +
                "the same as the range between startBrokerOut and endBrokerOut");
      }
      return new BrokersetRangeMap(startBrokerIn, endBrokerIn, startBrokerOut, endBrokerOut);
    }
  }
}
