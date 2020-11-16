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

import java.util.List;

import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.ClusterManager;
import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.utils.OrionUUID;

@Path("/clusters/{clusterId}/actions")
@Produces({MediaType.APPLICATION_JSON })
public class ActionEngineApi extends BaseClustersApi {

  public ActionEngineApi(ClusterManager mgr) {
    super(mgr);
  }

  @GET
  public List<Action> getActions(@PathParam("clusterId") @NotNull String clusterId) {
    Cluster cluster = checkAndGetCluster(clusterId);
    return cluster.getActionEngine().getTrackedActionsList();
  }

//  @Path("/{actionKey}")
//  @GET
  public Action getAction(@PathParam("clusterId") @NotNull String clusterId,
                          @PathParam("actionKey") @NotNull String actionKey) {
    Cluster cluster = checkAndGetCluster(clusterId);
    Action action = cluster.getActionEngine().getTrackedActionsMap().get(OrionUUID.create(actionKey));
    if (action == null) {
      throw new NotFoundException("Action id:" + actionKey + " not found in cluster:" + clusterId);
    }
    return action;
  }

}
