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
package com.pinterest.orion.core.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeriesOutput {

  private String measurementName;
  private String valueFieldName;
  private boolean isFp;
  private Map<String, String> tags;
  private List<DataPoint> dataPoints;

  public SeriesOutput() {
    tags = new HashMap<>();
    dataPoints = new ArrayList<>();
  }

  public SeriesOutput(String measurementName,
                      String valueFieldName,
                      boolean isFp,
                      Map<String, String> tags,
                      List<DataPoint> dataPoints) {
    this.measurementName = measurementName;
    this.valueFieldName = valueFieldName;
    this.isFp = isFp;
    this.tags = tags;
    this.dataPoints = dataPoints;
  }

  /**
   * @return the measurementName
   */
  public String getMeasurementName() {
    return measurementName;
  }

  /**
   * @param measurementName the measurementName to set
   */
  public void setMeasurementName(String measurementName) {
    this.measurementName = measurementName;
  }

  /**
   * @return the valueFieldName
   */
  public String getValueFieldName() {
    return valueFieldName;
  }

  /**
   * @param valueFieldName the valueFieldName to set
   */
  public void setValueFieldName(String valueFieldName) {
    this.valueFieldName = valueFieldName;
  }

  /**
   * @return the isFp
   */
  public boolean isFp() {
    return isFp;
  }

  /**
   * @param isFp the isFp to set
   */
  public void setFp(boolean isFp) {
    this.isFp = isFp;
  }

  /**
   * @return the tags
   */
  public Map<String, String> getTags() {
    return tags;
  }

  /**
   * @param tags the tags to set
   */
  public void setTags(Map<String, String> tags) {
    this.tags = tags;
  }

  /**
   * @return the dataPoints
   */
  public List<DataPoint> getDataPoints() {
    return dataPoints;
  }

  /**
   * @param dataPoints the dataPoints to set
   */
  public void setDataPoints(List<DataPoint> dataPoints) {
    this.dataPoints = dataPoints;
  }

}