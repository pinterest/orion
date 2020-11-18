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

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.pinterest.orion.common.Metrics;
import com.pinterest.orion.core.ClusterManager;
import com.pinterest.orion.core.Node;

@Path("/clusters/{clusterId}/nodes/{nodeId}")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
public class NodeApi extends BaseClustersApi {

  public NodeApi(ClusterManager mgr) {
    super(mgr);
  }

  @GET
  public boolean isHealthy(@PathParam("clusterId") @NotNull String clusterId,
                           @PathParam("nodeId") @NotNull String nodeId) {
    return true;
//    Node node = checkAndGetNode(clusterId, nodeId);
//    try {
//      return node.getServiceStatus().getStatusType() == StatusType.OK;
//    } catch (TException e) {
//      throw new InternalServerErrorException("Unable to check node health");
//    }
  }
  
  @Path("/metrics")
  @GET
  public Metrics getCurrentNodeMetrics(@PathParam("clusterId") @NotNull String clusterId,
                                       @PathParam("nodeId") @NotNull String nodeId) {
    Node node = checkAndGetNode(clusterId, nodeId);
    return node.getCurrentNodeMetrics();
  }

}
