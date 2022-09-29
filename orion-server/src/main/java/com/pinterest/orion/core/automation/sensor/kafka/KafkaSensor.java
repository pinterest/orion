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
package com.pinterest.orion.core.automation.sensor.kafka;

import com.pinterest.orion.core.Attribute;
import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.automation.sensor.Sensor;
import com.pinterest.orion.core.kafka.KafkaCluster;

import java.util.HashMap;
import java.util.Map;

public abstract class KafkaSensor extends Sensor {

  @Override
  public final void observe(Cluster cluster) throws Exception {
    if (logger == null) {
      logger = getLogger(cluster);
    }
    if(cluster instanceof KafkaCluster){
      sense((KafkaCluster) cluster);
    }
  }

  public abstract void sense(KafkaCluster cluster) throws Exception;
}
