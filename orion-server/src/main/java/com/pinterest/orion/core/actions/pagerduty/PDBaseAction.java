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
package com.pinterest.orion.core.actions.pagerduty;

import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.utils.pagerduty.PagerDutyClient;

public abstract class PDBaseAction extends Action {

  public static final String ATTR_PD_INCIDENT_ID_KEY = "incident_id";
  public static final String ATTR_PD_INCIDENT_IDS_KEY = "incident_ids";
  public static final String ATTR_PD_NOTES_KEY = "notes";

  public PagerDutyClient getPagerDutyClient() throws Exception {
    Cluster cluster = getEngine().getCluster();
    if (!cluster.containsAttribute(PagerDutyClient.ATTR_PD_CLIENT_KEY)) {
      throw new Exception("Missing PagerDuty client");
    }
    return getAttribute(cluster, PagerDutyClient.ATTR_PD_CLIENT_KEY).getValue();
  }

  @Override
  public Type getActionType() {
    return Type.CLUSTER;
  }
}
