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
package com.pinterest.orion.core.actions.alert;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinterest.orion.core.PluginConfigurationException;

import org.glassfish.jersey.client.JerseyClientBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

// Uses Event API v2
public class PagerDutyAlert extends Alert {
  private static ObjectMapper mapper = new ObjectMapper();
  private static final Logger logger = Logger.getLogger(PagerDutyAlert.class.getCanonicalName());

  private WebTarget webTarget;

  private static final String CONF_PAGERDUTY_SERVICE_TOKEN_KEY = "pagerduty_service_token";
  private static final String PAGERDUTY_EVENT_API_V2_ENDPOINT =
      "https://events.pagerduty.com/v2/enqueue";

  private String pagerdutyServiceToken;

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    super.initialize(config);
    if(!config.containsKey(CONF_PAGERDUTY_SERVICE_TOKEN_KEY)) {
      throw new PluginConfigurationException("Missing config " + CONF_PAGERDUTY_SERVICE_TOKEN_KEY);
    }
    pagerdutyServiceToken = (String) config.get(CONF_PAGERDUTY_SERVICE_TOKEN_KEY);
    webTarget = JerseyClientBuilder.newClient().target(PAGERDUTY_EVENT_API_V2_ENDPOINT);
  }

  @Override
  public void alert(AlertMessage message) {
    sendPager(message);
  }

  private void sendPager(AlertMessage message) {
    Map<String, Object> customDetails = new HashMap<>();
    customDetails.put("message_body", message.getBody());
    String summary = "[Orion] " + message.getBody();
    PagerDutyEvent event = new PagerDutyEvent(pagerdutyServiceToken, summary, message.getEntity(), customDetails);
    String payload = null;
    try {
      payload = mapper.writeValueAsString(event);
    } catch (JsonProcessingException jpe) {
      logger.log(Level.SEVERE, "Failed to build PagerDuty event payload: ", jpe);
      return;
    }
    try {
      Response resp = webTarget.request(MediaType.APPLICATION_JSON_TYPE).async()
        .post(Entity.json(payload)).get();
      if (resp.getStatus() >= 400) {
        logger.severe("Bad Response from PagerDuty: " + resp.getStatus() + " from "
            + webTarget.getUri() + ": " + resp.readEntity(String.class));
      }
    } catch (ExecutionException | InterruptedException e) {
      logger.log(Level.SEVERE, "Failed to get response from webTarget: ", e);
    }
  }

  @Override
  public String getName() {
    return "PagerDuty pager";
  }

  private static class PagerDutyEvent {
    @JsonProperty("routing_key")
    private String routingKey;
    @JsonProperty("event_action")
    private String eventAction = "trigger";
    @JsonProperty
    private PagerDutyEventPayload payload;

    private static class PagerDutyEventPayload {
      @JsonProperty
      private String summary;
      @JsonProperty
      private String source;
      @JsonProperty
      private String severity = "critical";
      @JsonProperty("custom_details")
      private Map<String, Object> customDetails;

      public PagerDutyEventPayload(String summary, String source, Map<String, Object> customDetails) {
        this.summary = summary;
        this.source = source;
        this.customDetails = customDetails;
      }
    }

    public PagerDutyEvent(String routingKey, String summary, String source, Map<String, Object> customDetails) {
      this.routingKey = routingKey;
      this.payload = new PagerDutyEventPayload(summary, source, customDetails);
    }
  }
}
