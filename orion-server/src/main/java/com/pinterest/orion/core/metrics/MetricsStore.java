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

import java.util.List;
import java.util.Map;

import com.pinterest.orion.common.AgentHeartbeat;

public interface MetricsStore {

  public void init(Map<String, Object> config) throws Exception;

  public void publishMetrics(AgentHeartbeat heartbeat) throws Exception;

  public List<SeriesOutput> getMetrics(String seriesPattern,
                                 String valueFieldPattern,
                                 Map<String, String> tags,
                                 long startTs,
                                 long endTs) throws Exception;

}
