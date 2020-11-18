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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class SlackAlert extends Alert {
  private static ObjectMapper mapper = new ObjectMapper();
  private static final Logger logger = Logger.getLogger(SlackAlert.class.getCanonicalName());

  private List<WebTarget> webTargets;

  public List<WebTarget> getWebTargets() {
    return webTargets;
  }

  public void setWebTargets(List<WebTarget> webTargets) {
    this.webTargets = webTargets;
  }

  // optional link that points to UI
  private static String CONFIG_MESSAGE_PREFIX_KEY = "message_prefix";
  private static String CONFIG_SLACK_INCOMING_WEBHOOK_KEY = "slack_webhooks";
  private static int TITLE_LENGTH_LIMIT = 100;
  private static int MESSAGE_LENGTH_LIMIT = 1000;

  private String messagePrefix = "";

  // utility class used to encode message to JSON
  protected class SimpleSlackMessage {
    @JsonProperty
    private String text;
    @JsonProperty
    private int link_names = 1;

    public SimpleSlackMessage(String title, String rawMessage, String link) {
      String message = rawMessage;

      if (title.length() >= TITLE_LENGTH_LIMIT) {
        title = title.substring(0, TITLE_LENGTH_LIMIT) + "...";
      }

      if (message.length() >= MESSAGE_LENGTH_LIMIT) {
        message = message.substring(0, MESSAGE_LENGTH_LIMIT) + "...";
      }

      // title styled using markdown bold
      text = "*" + title + "*" + "\n" + message;
      if (link != null) {
        text = text + "\n" + link;
      }
    }
  }

  private String buildSlackMessageJSONPayload(String title,
                                              String message,
                                              String link) throws JsonProcessingException {
    return mapper.writeValueAsString(new SimpleSlackMessage(title, message, link));
  }

  @Override
  public void alert(AlertMessage message) {
    sendSlackMessage(message.getTitle(), message.getBody() + "\n@" + getOwner(),
        message.getLink());
  }

  public void sendSlackMessage(String title, String message, String link) {
    List<Future<Response>> responses = new ArrayList<>();
    String payload = null;
    try {
      payload = buildSlackMessageJSONPayload(messagePrefix + title, message, link);
    } catch (JsonProcessingException jpe) {
      logger.log(Level.SEVERE, "Failed to build slack message payload: ", jpe);
      return;
    }
    for (WebTarget target : getWebTargets()) {
      Future<Response> resp = target.request(MediaType.APPLICATION_JSON_TYPE).async()
          .post(Entity.json(payload));
      responses.add(resp);
    }
    for (int i = 0; i < responses.size(); i++) {
      try {
        Response resp = responses.get(i).get();
        if (resp.getStatus() != 200) {
          logger.severe("Bad Response from Slack: " + resp.getStatus() + " from "
              + webTargets.get(i).getUri());
        }
      } catch (ExecutionException | InterruptedException e) {
        logger.log(Level.SEVERE, "Failed to get response from webTarget: ", e);
      }
    }
  }

  @Override
  public void initialize(Map<String, Object> config) throws PluginConfigurationException {
    super.initialize(config);
    if (!config.containsKey(CONFIG_SLACK_INCOMING_WEBHOOK_KEY)) {
      System.out.println(config);
      throw new PluginConfigurationException(
          "Missing config " + CONFIG_SLACK_INCOMING_WEBHOOK_KEY + " for plugin " + this.getClass());
    }
    String configSlackIncomingWebhook = config.get(CONFIG_SLACK_INCOMING_WEBHOOK_KEY).toString();

    if (config.containsKey(CONFIG_MESSAGE_PREFIX_KEY)) {
      messagePrefix = config.get(CONFIG_MESSAGE_PREFIX_KEY).toString();
    }

    List<WebTarget> webTargets = new ArrayList<>();
    for (String webhookUrl : configSlackIncomingWebhook.split(",")) {
      webTargets.add(JerseyClientBuilder.newClient().target(webhookUrl));
    }
    setWebTargets(webTargets);
  }

  @Override
  public String getName() {
    return "Slack Alert";
  }
}
