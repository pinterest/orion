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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.ClusterManager;
import com.pinterest.orion.core.Node;
import com.pinterest.orion.core.actions.alert.AlertLevel;
import com.pinterest.orion.core.actions.alert.AlertMessage;
import com.pinterest.orion.core.automation.operator.OperatorContainer;
import com.pinterest.orion.core.automation.sensor.SensorContainer;
import com.pinterest.orion.server.OrionServer;
import com.pinterest.orion.server.MetricsConstants;
import com.pinterest.orion.server.config.OrionConf;

@Path("/clusters/{clusterId}")
@Produces({ MediaType.APPLICATION_JSON })
public class ClusterApi extends BaseClustersApi {

  private static final Logger logger = Logger.getLogger(ClusterApi.class.getCanonicalName());

  public ClusterApi(ClusterManager mgr) {
    super(mgr);
  }

  @GET
  public Cluster getCluster(@Context ContainerRequestContext crc,
                            @PathParam("clusterId") @NotNull String clusterId) {
    return checkAndGetCluster(clusterId);
  }

  /**
   * Non-blocking action call, returns the UUID of the Action that can then be
   * polled for Status
   *
   * @param clusterId
   * @return
   */
  @RolesAllowed({ OrionConf.ADMIN_ROLE })
  @Path("/actions/{actionKey}")
  @Consumes({ MediaType.APPLICATION_JSON })
  @POST
  public String runClusterAction(@Context ContainerRequestContext crc,
                                 @PathParam("clusterId") @NotNull String clusterId,
                                 @PathParam("actionKey") @NotNull String actionKey,
                                 OrionActionRequest request) {
    Cluster cluster = checkAndGetCluster(clusterId);
    Map<String, Object> attributes = new HashMap<>();
    if (request != null && !request.getAttributes().isEmpty()) {
      attributes.putAll(request.getAttributes());
    }
    String user = getUser(crc);
    logger.info("Triggered action(" + actionKey + ") request by(" + user + ")");
    OrionServer.METRICS.counter(MetricsConstants.ACTION_PREFIX + actionKey).inc();
    return doAction(cluster, user, actionKey, attributes);
  }

  @Path("/nodes")
  @GET
  public List<Node> getNodes(@Context ContainerRequestContext crc,
                             @PathParam("clusterId") @NotNull String clusterId) {
    Cluster cluster = checkAndGetCluster(clusterId);
    return new ArrayList<>(cluster.getNodeMap().values());
  }

  @Path("/monitors")
  @GET
  public List<SensorContainer> getMonitors(@Context ContainerRequestContext crc,
                                           @PathParam("clusterId") @NotNull String clusterId) {
    Cluster cluster = checkAndGetCluster(clusterId);
    return cluster.getAutomationEngine().getSensors();
  }

  @Path("/operators")
  @GET
  public List<OperatorContainer> getOperators(@Context ContainerRequestContext crc,
                                              @PathParam("clusterId") @NotNull String clusterId) {
    Cluster cluster = checkAndGetCluster(clusterId);
    return cluster.getAutomationEngine().getOperators();
  }

  @RolesAllowed({ OrionConf.ADMIN_ROLE })
  @Path("/alerts/{alertId}/read")
  @PUT
  public void setAlertToRead(@Context ContainerRequestContext crc,
                             @PathParam("clusterId") @NotNull String clusterId,
                             @PathParam("alertId") @NotNull String alertId) {
    Cluster cluster = checkAndGetCluster(clusterId);
    try {
      cluster.getActionEngine().getAlert(alertId).setRead(true);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Bad alert id", e);
    }
  }

  @RolesAllowed({ OrionConf.ADMIN_ROLE })
  @Path("/maintenance")
  @PUT
  public void enableMaintenanceMode(@Context ContainerRequestContext crc,
                                    @PathParam("clusterId") @NotNull String clusterId) {
    String user = getUser(crc);
    Cluster cluster = checkAndGetCluster(clusterId);
    cluster.setMaintenance(true);
    AlertMessage alert = new AlertMessage(
        cluster.getClusterId() + " is in maintenance mode",
        cluster.getClusterId() + " placed in maintenance mode on " + new Date(),
        user);
    try {
      cluster.getActionEngine().alert(AlertLevel.MEDIUM, alert);
    } catch (Exception e) {
      // best effort alerting
      logger.severe("Couldn't alert maintenance mode status: " + e);
    }
  }

  @RolesAllowed({ OrionConf.ADMIN_ROLE })
  @Path("/maintenance")
  @DELETE
  public void disableMaintenanceMode(@Context ContainerRequestContext crc,
                                    @PathParam("clusterId") @NotNull String clusterId) {
    String user = getUser(crc);
    Cluster cluster = checkAndGetCluster(clusterId);
    cluster.setMaintenance(false);
    AlertMessage alert = new AlertMessage(
        cluster.getClusterId() + " is out of maintenance mode",
        cluster.getClusterId() + " out of maintenance mode on " + new Date(),
        user);
    try {
      cluster.getActionEngine().alert(AlertLevel.MEDIUM, alert);
    } catch (Exception e) {
      // best effort alerting
      logger.severe("Couldn't alert maintenance mode status: " + e);
    }
  }
}
