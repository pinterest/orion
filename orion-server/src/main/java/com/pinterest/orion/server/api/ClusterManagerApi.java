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

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.lang3.mutable.MutableInt;

import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.ClusterCost;
import com.pinterest.orion.core.ClusterManager;
import com.pinterest.orion.core.Node;
import com.pinterest.orion.core.Utilization;
import com.pinterest.orion.core.global.sensor.GlobalPluginManager;
import com.pinterest.orion.core.global.sensor.GlobalSensor;
import com.pinterest.orion.core.actions.aws.AmiTagManager;
import com.pinterest.orion.server.config.OrionConf;

@Path("/")
@Produces({ MediaType.APPLICATION_JSON })
public class ClusterManagerApi extends BaseClustersApi {
  private AmiTagManager amiTagManager;

  public ClusterManagerApi(ClusterManager mgr) {
    super(mgr);
  }

  @Path("/clusters")
  @GET
  public List<String> getClusters() {
    return new ArrayList<>(mgr.getClusters().keySet());
  }

  @Path("/clusterssummary")
  @GET
  public List<ClusterSummary> getClusterSummary() {
    List<ClusterSummary> clusters = new ArrayList<>();
    for (Entry<String, Cluster> entry : mgr.getClusters().entrySet()) {
      Cluster value = entry.getValue();
      ClusterSummary clusterSummary = new ClusterSummary(value);
      clusters.add(clusterSummary);
    }
    return clusters;
  }
  
  @Path("/globalsensors")
  @GET
  public Collection<GlobalSensor> getGlobalSensors() {
    return GlobalPluginManager.listSensors();
  }

  @Path("/utilizationsummary")
  @GET
  public UtilizationSummary getUtilizationSummary() {
    UtilizationSummary utilizationSummary = new UtilizationSummary();
    Map<String, MutableInt> instancesByType = new HashMap<>();
    for (Entry<String, Cluster> entry : mgr.getClusters().entrySet()) {
      Map<String, Node> nodeMap = entry.getValue().getNodeMap();
      for (Entry<String, Node> entry2 : nodeMap.entrySet()) {
        Node node = entry2.getValue();
        String type = "Not_available";
        if (node.getAgentNodeInfo() != null && node.getAgentNodeInfo().getNodeType() != null) {
          type = node.getAgentNodeInfo().getNodeType();
        }
        type = type.replace(".", "_");
        MutableInt val = instancesByType.get(type);
        if (val == null) {
          val = new MutableInt();
          instancesByType.put(type, val);
        }
        val.increment();
      }
    }
    utilizationSummary.setInstancesByType(instancesByType);
    return utilizationSummary;
  }

  @Path("/utilizationDetailsByCluster")
  @GET
  public Map<String, Map<String, Utilization>> getUtilizationDetailsByCluster() {
    Map<String, Map<String, Utilization>> utilizationMap = new HashMap<>();
    for (Entry<String, Cluster> entry : mgr.getClusters().entrySet()) {
      utilizationMap.put(entry.getKey(), entry.getValue().getUtilizationMap());
    }
    return utilizationMap;
  }

  @Path("/describeImages")
  @GET
  public List<Ami> describeImages(
      @QueryParam(AmiTagManager.KEY_RELEASE) String os,
      @QueryParam(AmiTagManager.KEY_CPU_ARCHITECTURE) String arch
  ) {
    Map<String, String> filter = new HashMap<>();
    if (os != null)
      filter.put(AmiTagManager.KEY_RELEASE, os);
    if (arch != null)
      filter.put(AmiTagManager.KEY_CPU_ARCHITECTURE, arch);
    if (amiTagManager == null)
      amiTagManager = new AmiTagManager();
    return amiTagManager.getAmiList(filter);
  }

  @Path("/updateImageTag")
  @PUT
  public void updateImageTag(
      @QueryParam(AmiTagManager.KEY_AMI_ID) String amiId,
      @QueryParam(AmiTagManager.KEY_APPLICATION_ENVIRONMENT) String applicationEnvironment
  ) {
    if (amiTagManager == null)
      amiTagManager = new AmiTagManager();
    amiTagManager.updateAmiTag(amiId, applicationEnvironment);
  }


  @Path("/getEnvTypes")
  @GET
  public List<String> getEnvTypes() {
    List<String> envTypes = null;
    Map<String, Object> additionalConfigs = mgr.getOrionConf().getAdditionalConfigs();
    if(additionalConfigs != null && additionalConfigs.containsKey(AmiTagManager.ENV_TYPES_KEY)) {
      envTypes = (List<String>) additionalConfigs.get(AmiTagManager.ENV_TYPES_KEY);
    }
    return envTypes;
  }

  @RolesAllowed({ OrionConf.ADMIN_ROLE, OrionConf.MGMT_ROLE })
  @Path("/costByCluster")
  @GET
  public Map<String, ClusterCost> getCostByCluster() {
    Map<String, ClusterCost> costMap = new HashMap<>();
    for (Entry<String, Cluster> entry : mgr.getClusters().entrySet()) {
      costMap.put(entry.getKey(), entry.getValue().getCostMap());
    }
    return costMap;
  }

  @RolesAllowed({ OrionConf.ADMIN_ROLE, OrionConf.MGMT_ROLE })
  @Path("/clusterInstanceTypesCSV")
  @GET
  public String getClusterInstanceTypesReport() {
    Map<String, Map<String, MutableInt>> instanceTypeMap = new HashMap<>();
    for (Entry<String, Cluster> entry : mgr.getClusters().entrySet()) {
      Map<String, MutableInt> value = new HashMap<>();
      instanceTypeMap.put(entry.getKey(), value);
      Map<String, Node> nodeMap = entry.getValue().getNodeMap();
      for (Entry<String, Node> entry2 : nodeMap.entrySet()) {
        Node node = entry2.getValue();
        String type = "Not_available";
        if (node.getAgentNodeInfo() != null && node.getAgentNodeInfo().getNodeType() != null) {
          type = node.getAgentNodeInfo().getNodeType();
        }
        type = type.replace(".", "_");
        MutableInt val = value.get(type);
        if (val == null) {
          val = new MutableInt();
          value.put(type, val);
        }
        val.increment();
      }
    }
    StringBuilder builder = new StringBuilder();
    builder.append("Cluster,Type");
    for (Entry<String, Map<String, MutableInt>> entry : instanceTypeMap.entrySet()) {
      String clusterName = entry.getKey();
      for (Entry<String, MutableInt> entry2 : entry.getValue().entrySet()) {
        builder.append(
            "\n" + clusterName + "," + entry2.getKey() + "," + entry2.getValue().getValue());
      }

    }
    return builder.toString();
  }

  @Path("/clustersdump")
  @GET
  public Map<String, Cluster> getDump() {
    return mgr.getClusters();
  }

  @Path("/user")
  @GET
  public Principal getUser(@Context SecurityContext ctx) {
    if (ctx.getUserPrincipal() != null) {
      return ctx.getUserPrincipal();
    } else {
      throw new NotFoundException();
    }
  }

}
