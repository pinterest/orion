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
package com.pinterest.orion.core.state;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Map;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.ClusterStateSink;
import com.pinterest.orion.core.PluginConfigurationException;

public class FileBackedClusterStateSink implements ClusterStateSink {

  private File checkPointDir;

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    String checkPointDirPath = config.getOrDefault("checkpointDirectory", "/tmp/orion").toString();
    checkPointDir = new File(checkPointDirPath);
  }

  @Override
  public String getName() {
    return "FileBackedClusterStateSink";
  }

  @Override
  public void serialize(Cluster cluster) {
    Kryo kryo = new Kryo();
    try {
      kryo.writeClassAndObject(
          new Output(new FileOutputStream(checkPointFileForCluster(cluster.getClusterId()))),
          cluster);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  protected File checkPointFileForCluster(String clusterId) {
    return new File(checkPointDir, clusterId + ".dat");
  }

  @Override
  public Cluster deserialize(String clusterId) {
    File checkPointFileForCluster = checkPointFileForCluster(clusterId);
    if (checkPointFileForCluster.exists()) {
      Kryo kryo = new Kryo();
      try {
        return (Cluster) kryo.readClassAndObject(new Input(new FileInputStream(checkPointFileForCluster)));
      } catch (FileNotFoundException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return null;
  }

}
