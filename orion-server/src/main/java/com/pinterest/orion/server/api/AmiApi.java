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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import com.pinterest.orion.core.ClusterManager;
import com.pinterest.orion.core.actions.aws.AmiTagManager;

@Path("/")
@Produces({ MediaType.APPLICATION_JSON })
public class AmiApi extends BaseClustersApi {
  private AmiTagManager amiTagManager = new AmiTagManager();

  public AmiApi(ClusterManager mgr) {
    super(mgr);
  }

  @Path("/describeImages")
  @GET
  public Object describeImages(
      @QueryParam(AmiTagManager.KEY_RELEASE) String os,
      @QueryParam(AmiTagManager.KEY_ARCHITECTURE) String arch,
      @QueryParam(AmiTagManager.KEY_ENVIRONMENT) String environment
  ) {
    Map<String, String> filter = new HashMap<>();
    if (os != null)
      filter.put(AmiTagManager.KEY_RELEASE, os);
    if (arch != null)
      filter.put(AmiTagManager.KEY_ARCHITECTURE, arch);
    if (environment != null)
      filter.put(AmiTagManager.KEY_ENVIRONMENT, environment);
    return amiTagManager.getStatusOrAmiList(filter);
  }

  @Path("/updateImageTag")
  @PUT
  public void updateImageTag(
      @QueryParam(AmiTagManager.KEY_AMI_ID) String amiId,
      @QueryParam(AmiTagManager.KEY_APPLICATION_ENVIRONMENT) String applicationEnvironment
  ) {
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

}
