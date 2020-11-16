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
package com.pinterest.orion.core.actions.generic;


import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.alert.AlertMessage;
import com.pinterest.orion.core.actions.alert.LocalhostEmailAlertAction;
import com.pinterest.orion.utils.OrionConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

public class NotifyNimbusOwnerAction extends Action {
  public static final String ATTR_SUBJECT_KEY = "subject";
  public static final String ATTR_MESSAGE_KEY = "message";
  private static final String[]
      requiredAttributes = new String[]{OrionConstants.PROJECT, ATTR_SUBJECT_KEY, ATTR_MESSAGE_KEY};

  private static final String NIMBUS_API_ENDPOINT = "https://nimbus.pinadmin.com/api/v1";
  private static CloseableHttpClient httpClient = HttpClients.createDefault();


  @Override
  public void runAction() {
    for(String attrKey : requiredAttributes) {
      if(!containsAttribute(attrKey)) {
        markFailed("Missing attribute " + attrKey);
        return;
      }
    }

    String project = getAttribute(OrionConstants.PROJECT).getValue();
    String subject = getAttribute(ATTR_SUBJECT_KEY).getValue();
    String message = getAttribute(ATTR_MESSAGE_KEY).getValue();

    try {
      String projectEmail = getProjectEmailFromProject(project);
      this.getEngine().alert(new AlertMessage(subject, message, projectEmail), new LocalhostEmailAlertAction());
    } catch (ClientProtocolException hre) {
      markFailed("Failed to find the owner " + project + ": " + hre.getMessage());
      return;
    } catch (Exception e) {
      markFailed("Exception happened when sending email to " + project);
      return;
    }
    markSucceeded();
  }

  @Override
  public Type getActionType() {
    return null;
  }

  @Override
  public String getName() {
    return "NotifyNimbusOwnerAction";
  }

  protected static String getProjectEmailFromProject(String projectName) throws Exception {
    HttpGet httpGet = new HttpGet(NIMBUS_API_ENDPOINT + "/projects/" + projectName);
    httpGet.addHeader("accept", "application/json");

    ResponseHandler<String> handler = new ResponseHandler<String>() {
      @Override
      public String handleResponse(HttpResponse resp)
          throws IOException {
        HttpEntity entity = resp.getEntity();
        if (resp.getStatusLine().getStatusCode() != 200) {
          throw new HttpResponseException(resp.getStatusLine().getStatusCode(), resp.getStatusLine().getReasonPhrase());
        }
        if (entity == null) {
          throw new ClientProtocolException("Null response from Nimbus");
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(entity.getContent(), NimbusProjectResponse.class).projectEmail;
      }
    };
    return httpClient.execute(httpGet, handler);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class NimbusProjectResponse {
    public String projectEmail;
  }
}
