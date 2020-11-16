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
package com.pinterest.orion.core.utils;

import java.util.UUID;

public class OrionUUID implements Comparable<OrionUUID> {
  private long timestamp;
  private UUID uuid;

  public OrionUUID() {
    timestamp = System.currentTimeMillis();
    uuid = UUID.randomUUID();
  }

  protected OrionUUID(Long timestamp, UUID uuid) {
    this.timestamp = timestamp;
    this.uuid = uuid;
  }

  public static OrionUUID create(String orionUuidString) throws IllegalArgumentException {
    String[] split = orionUuidString.split("_");
    if(split.length != 2) {
      throw new IllegalArgumentException("Invalid OrionUUID string: Segments separated by '_' != 2");
    }

    long timestamp = Long.parseLong(split[0]);
    UUID uuid = UUID.fromString(split[1]);

    return new OrionUUID(timestamp, uuid);
  }

  public long getTimestamp() {
    return timestamp;
  }

  public UUID getUuid() {
    return uuid;
  }

  @Override
  public int compareTo(OrionUUID o) {
    int ret = Long.compare(o.timestamp, this.timestamp);
    if (ret == 0) {
      return o.uuid.compareTo(this.uuid);
    }
    return ret;
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }

    if(this == obj) {
      return true;
    }

    if(obj instanceof OrionUUID){
      OrionUUID lid = (OrionUUID) obj;
      return this.timestamp == lid.timestamp && this.uuid.equals(lid.uuid);
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return Long.toString(timestamp) + '_' + uuid;
  }
}
