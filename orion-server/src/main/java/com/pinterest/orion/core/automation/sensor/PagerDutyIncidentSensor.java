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
package com.pinterest.orion.core.automation.sensor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.utils.pagerduty.PagerDutyClient;

public class PagerDutyIncidentSensor extends Sensor {
  private static final String CONF_BOT_USER_ID_KEY = "pd_user_id";
  private static final String CONF_PD_SERVICES_KEY = "pd_service_ids";

  public static final String ATTR_PD_INCIDENTS_KEY = "pd_incidents";

  private String userId;
  private List<String> serviceIds;

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    super.initialize(config);

    if(!config.containsKey(CONF_BOT_USER_ID_KEY)) {
      throw new PluginConfigurationException("Missing configuration " + CONF_BOT_USER_ID_KEY);
    }
    userId = config.get(CONF_BOT_USER_ID_KEY).toString();

    if(!config.containsKey(CONF_PD_SERVICES_KEY)) {
      throw new PluginConfigurationException("Missing configuration " + CONF_PD_SERVICES_KEY);
    }
    serviceIds = (List<String>) config.get(CONF_PD_SERVICES_KEY);
  }

  @Override
  public void observe(Cluster cluster) throws Exception {
    PagerDutyClient pd;
    if(!cluster.containsAttribute(PagerDutyClient.ATTR_PD_CLIENT_KEY)) {
      pd = new PagerDutyClient();
      cluster.setAttribute(PagerDutyClient.ATTR_PD_CLIENT_KEY, pd);
    } else {
      pd = cluster.getAttribute(PagerDutyClient.ATTR_PD_CLIENT_KEY).getValue();
    }

    List<PagerDutyClient.Incident> incidents = pd.getTriggeredPagerDutyIncidents(userId, serviceIds);
    cluster.setAttribute(ATTR_PD_INCIDENTS_KEY, incidents);
  }

  @Override
  public String getName() {
    return "PagerDutyIncidentSensor";
  }

  public static void main(String[] args) throws Exception {
    if(args.length < 2) {
      System.out.println("Usage: PagerDutyIncidentSensor <user_id> <list of service_ids>");
      System.exit(-1);
    }
    PagerDutyClient pd = new PagerDutyClient();
    System.out.println(pd.getTriggeredPagerDutyIncidents(args[0], Arrays.asList(args).subList(1, args.length - 1)));
  }
}
