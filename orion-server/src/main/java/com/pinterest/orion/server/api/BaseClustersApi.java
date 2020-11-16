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

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.container.ContainerRequestContext;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.ClusterManager;
import com.pinterest.orion.core.Node;
import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.ActionEngine;
import com.pinterest.orion.core.actions.ActionFactory;
import com.pinterest.orion.core.actions.Action.Type;

import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Logger;

public class BaseClustersApi {

  private static final Logger logger = Logger.getLogger(BaseClustersApi.class.getCanonicalName());
  protected ClusterManager mgr;

  public BaseClustersApi(ClusterManager mgr) {
    this.mgr = mgr;
  }

  protected Cluster checkAndGetCluster(String clusterId) {
    Cluster cluster = mgr.getCluster(clusterId);
    if (cluster == null) {
      throw new BadRequestException("Cluster(" + clusterId + ") doesn't exist");
    }
    return cluster;
  }

  protected Node checkAndGetNode(String clusterId, String nodeId) {
    Cluster cluster = checkAndGetCluster(clusterId);
    Node node = cluster.getNodeMap().get(nodeId);
    if (node == null) {
      throw new BadRequestException("Invalid nodeid(" + nodeId + ")");
    }
    return node;
  }

  protected Action validateAndGetAction(Cluster cluster, Type type, String actionKey) {
    Action instance = null;
    try {
      instance = mgr.getActionFactory().getActionInstance(cluster, actionKey);
    } catch (Exception e) {
      logger.warning("Failed to get action " + actionKey + " for cluster " + cluster.getClusterId() + ":" + e);
    }
    if(instance == null || instance.getActionType() != type){
      throw new BadRequestException("Action: " + actionKey + " is not available.");
    }
    return instance;
  }

  protected String doAction(Cluster cluster,
                            String owner,
                            Type type,
                            String actionKey,
                            Map<String, Object> attributes) {
    ActionFactory actionFactory = mgr.getActionFactory();
    ActionEngine actionEngine = cluster.getActionEngine();
    if (actionFactory == null) {
      throw new InternalServerErrorException(
          "Actions not allowed for " + this.getClass().getName() + " API");
    }
    Action action = validateAndGetAction(cluster, type, actionKey);
    try {
      action.setOwner(owner);
      if (attributes != null) {
        for (Map.Entry<String, Object> e : attributes.entrySet()) {
          action.setAttribute(e.getKey(), e.getValue());
        }
      }
      actionEngine.dispatch(action);
      return action.getUuidString();
    } catch (IllegalArgumentException | RejectedExecutionException e) {
      throw new BadRequestException(e.getMessage());
    } catch (Exception e) {
      throw new InternalServerErrorException(e);
    }
  }

  protected String getUser(ContainerRequestContext crc) {
    if (crc.getSecurityContext() != null && crc.getSecurityContext().getUserPrincipal() != null) {
      String name = crc.getSecurityContext().getUserPrincipal().getName();
      return name == null || name.isEmpty() ? "n/a" : name;
    }
    return "n/a";
  }

  public static class OrionActionRequest {

    @JsonProperty
    private Map<String, Object> attributes;

    /**
     * @return the attributes
     */
    public Map<String, Object> getAttributes() {
      return attributes;
    }

    /**
     * @param attributes the attributes to set
     */
    public void setAttributes(Map<String, Object> attributes) {
      this.attributes = attributes;
    }
  }

}