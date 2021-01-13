package com.pinterest.orion.server.api.memq;

import com.pinterest.orion.core.ClusterManager;
import com.pinterest.orion.server.api.kafka.KafkaApiFactory;

import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Environment;

public class MemqApiFactory extends KafkaApiFactory {

  private static final long serialVersionUID = 1L;

  @Override
  public void registerAPIs(Environment globalEnv,
                           JerseyEnvironment jerseyEnv,
                           ClusterManager clusterMgr) {
    globalEnv.getObjectMapper().registerModule(this);
  }

}
