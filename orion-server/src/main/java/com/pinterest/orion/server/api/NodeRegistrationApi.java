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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Preconditions;
import com.pinterest.orion.common.AgentHeartbeat;
import com.pinterest.orion.common.NodeCmd;
import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.ClusterManager;
import com.pinterest.orion.server.OrionServer;
import com.pinterest.orion.server.MetricsConstants;

@Path("/register")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
public class NodeRegistrationApi {

  private static final Logger logger = Logger.getLogger(NodeRegistrationApi.class.getName());
  private ClusterManager mgr;

  public NodeRegistrationApi(ClusterManager mgr) {
    this.mgr = mgr;
  }

  @PUT
  public NodeCmd registerNode(@NotNull AgentHeartbeat heartbeat) {
    try {
      validate(heartbeat);
    } catch (Exception e) {
      OrionServer.METRICS.counter(MetricsConstants.REGISTRATION_BAD_REQUEST).inc();
      throw new BadRequestException(e);
    }
    try {
      if (mgr.getMetricsStore() != null) {
        mgr.getMetricsStore().publishMetrics(heartbeat);
      }
    } catch (Exception e) {
      logger.log(Level.FINE, "Metrics ingestion failed", e);
      OrionServer.METRICS.counter(MetricsConstants.METRICS_INGEST_EXCEPTION).inc();
    }
    NodeInfo info = heartbeat.getNodeInfo();
    Cluster cluster = mgr.getCluster(info.getClusterId());
    if (cluster == null) {
      OrionServer.METRICS.counter(MetricsConstants.REGISTRATION_INVALID_CLUSTER).inc();
      throw new BadRequestException("Cluster(" + info.getClusterId() + ") doesn't exist");
    }
    try {
      OrionServer.METRICS.counter(MetricsConstants.REGISTRATION_VALID_REQUEST).inc();
      NodeCmd cmd = cluster.updateNodeFromAgentHeartbeat(heartbeat);
      return cmd;
    } catch (Exception e) {
      throw new InternalServerErrorException("Server failed to open RPC connect to agent", e);
    }
  }

  public static void validate(AgentHeartbeat heartbeat) {
    validate(heartbeat.getNodeInfo());
  }

  public static void validate(NodeInfo info) {
    Preconditions.checkNotNull(info.getClusterId());
    Preconditions.checkNotNull(info.getHostname());
    Preconditions.checkNotNull(info.getNodeId());
  }

}
