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
package com.pinterest.orion.server.api.kafka;

import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.pinterest.orion.core.ClusterManager;
import com.pinterest.orion.core.automation.sensor.kafka.KafkaConsumerGroupOffsetSensor;
import com.pinterest.orion.core.kafka.Brokerset;
import com.pinterest.orion.core.kafka.KafkaCluster;
import com.pinterest.orion.core.kafka.KafkaConsumerGroupDescription;
import com.pinterest.orion.server.api.BaseClustersApi;

import java.util.Collections;
import java.util.List;

@Path("/clusters/{clusterId}/kafka")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
public class KafkaClusterApi extends BaseClustersApi {

  public KafkaClusterApi(ClusterManager mgr) {
    super(mgr);
  }

  @Path("/consumer_groups")
  @GET
  public List<KafkaConsumerGroupDescription> getConsumerGroups(@PathParam("clusterId") @NotNull String clusterId) {
    KafkaCluster cluster = checkAndGetKafkaCluster(clusterId);

    if(cluster.containsAttribute(KafkaConsumerGroupOffsetSensor.ATTR_CONSUMER_GROUPS_KEY)) {
      return cluster.getAttribute(KafkaConsumerGroupOffsetSensor.ATTR_CONSUMER_GROUPS_KEY).getValue();
    }
    return Collections.EMPTY_LIST;
  }

  @Path("/brokerset")
  @PUT
  public void createBrokerset(@PathParam("clusterId") @NotNull String clusterId,
                              Brokerset brokerset) {
    KafkaCluster cluster = checkAndGetKafkaCluster(clusterId);
    try {
      cluster.addBrokerset(brokerset);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

  public KafkaCluster checkAndGetKafkaCluster(String clusterId) {
    return (KafkaCluster) checkAndGetCluster(clusterId);
  }

}