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
package com.pinterest.orion.server.api;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.pinterest.orion.core.Attribute;
import com.pinterest.orion.core.ClusterManager;

import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Environment;

public abstract class CustomApiFactory extends SimpleModule {
  
  private static final long serialVersionUID = 1L;

  public CustomApiFactory() {
    addSerializer(Attribute.class, new AttributeSerializer());
  }

  public abstract void registerAPIs(Environment globalEnv,
                           JerseyEnvironment jerseyEnv,
                           ClusterManager clusterMgr);

}
