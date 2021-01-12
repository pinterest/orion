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
package com.pinterest.orion.common;

import java.io.Serializable;

public class Value implements Serializable {

  private static final long serialVersionUID = 1L;
  private MetricType type;
  private String name;
  private double value;
  private boolean fp;

  public Value() {
  }
  
  public Value(MetricType type, String name, double value) {
    this.type = type;
    this.name = name;
    this.value = value;
  }

  public Value(MetricType type, String name, double value, boolean fp) {
    this.type = type;
    this.name = name;
    this.value = value;
    this.fp = fp;
  }

  /**
   * @return the type
   */
  public MetricType getType() {
    return type;
  }

  /**
   * @param type the type to set
   */
  public void setType(MetricType type) {
    this.type = type;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return the value
   */
  public double getValue() {
    return value;
  }

  public long getValueAsLong() {
    return ((Double) value).longValue();
  }

  /**
   * @param value the value to set
   */
  public void setValue(double value) {
    this.value = value;
  }

  /**
   * @return the fp
   */
  public boolean isFp() {
    return fp;
  }

  /**
   * @param fp the fp to set
   */
  public void setFp(boolean fp) {
    this.fp = fp;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "Value [type=" + type + ", name=" + name + ", value=" + value + ", fp=" + fp + "]";
  }

}
