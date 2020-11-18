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
package com.pinterest.orion.core.automation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.Plugin;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.automation.operator.Operator;
import com.pinterest.orion.core.automation.operator.OperatorContainer;
import com.pinterest.orion.core.automation.operator.OperatorExecutor;
import com.pinterest.orion.core.automation.sensor.Sensor;
import com.pinterest.orion.core.automation.sensor.SensorContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AutomationEngine implements Plugin {
  private static final Logger logger = Logger.getLogger(AutomationEngine.class.getCanonicalName());
  private static final int MONITOR_PARALLELISM = Runtime.getRuntime().availableProcessors();

  private Cluster cluster;
  @JsonIgnore
  private Map<String, SensorContainer> sensorMap = new HashMap<>();
  @JsonIgnore
  private Map<String, OperatorContainer> operatorMap = new LinkedHashMap<>();

  private ScheduledExecutorService sensorThreadPool = new ScheduledThreadPoolExecutor(MONITOR_PARALLELISM);
  private OperatorExecutor operatorExecutor;

  public AutomationEngine(Cluster cluster, List<Sensor> sensors, List<Operator> operators) {
    this.cluster = cluster;
    initializeSensors(sensors);
    initializeOperators(operators);
  }

  private void initializeSensors(List<Sensor> sensors) {
    for(Sensor sensor : sensors){
      sensorMap.put(sensor.getSensorIdentifier(), new SensorContainer(sensor, cluster, sensorThreadPool));
    }
  }

  private void initializeOperators(List<Operator> operators) {
    List<OperatorContainer> operatorContainers = new ArrayList<>(operators.size());
    for(Operator operator : operators){
      OperatorContainer container = new OperatorContainer(operator);
      operatorMap.put(operator.getName(), container);
      operatorContainers.add(container);
    }
    this.operatorExecutor = new OperatorExecutor(cluster, operatorContainers, cluster.getActionEngine());
  }

  public Set<Future<?>> triggerSensors(Set<String> postRunSensorsKeys) {
    Set<Future<?>> futures = new HashSet<>();
    for(String sensorKey : postRunSensorsKeys) {
      if(sensorMap.containsKey(sensorKey)){
        logger.log(Level.FINE, "Triggering sensor " + sensorKey);
        futures.add(sensorMap.get(sensorKey).scheduleNow());
      }
    }
    return futures;
  }

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    for(SensorContainer sensorContainer : sensorMap.values()){
      sensorContainer.start();
    }
    operatorExecutor.initialize(config);
    operatorExecutor.start();
  }

  @JsonProperty("sensors")
  public List<SensorContainer> getSensors() {
    return new ArrayList<>(sensorMap.values());
  }

  @JsonProperty("operators")
  public List<OperatorContainer> getOperators() {
    return new ArrayList<>(operatorMap.values());
  }

  @JsonIgnore
  public Map<String, SensorContainer> getSensorMap() {
    return sensorMap;
  }

  @JsonIgnore
  public Map<String, OperatorContainer> getOperatorMap() {
    return operatorMap;
  }

  @Override
  public String getName() {
    return cluster.getName() + "-AutomationEngine";
  }
}
