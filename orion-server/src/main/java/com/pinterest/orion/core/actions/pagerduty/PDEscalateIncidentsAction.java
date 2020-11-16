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

import java.util.List;

import com.pinterest.orion.core.utils.pagerduty.PagerDutyClient;

public class PDEscalateIncidentsAction extends PDBaseAction {

  @Override
  public void runAction() throws Exception {

    PagerDutyClient pd = getPagerDutyClient();

    if (!containsAttribute(PDBaseAction.ATTR_PD_INCIDENT_IDS_KEY)) {
      markFailed("No incidents in attributes");
      return;
    }
    List<String> incidentIds = getAttribute(PDBaseAction.ATTR_PD_INCIDENT_IDS_KEY).getValue();
    String notes = null;

    if (containsAttribute(PDBaseAction.ATTR_PD_NOTES_KEY)) {
      notes = getAttribute(PDBaseAction.ATTR_PD_NOTES_KEY).getValue();
      for(String id : incidentIds) {
        pd.addNoteToIncident(id, notes);
      }
    }
    pd.escalateIncidents(incidentIds);

    markSucceeded();
  }

  @Override
  public String getName() {
    return null;
  }
}
