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
package com.pinterest.orion.core.utils.pagerduty;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PagerDutyClient {
  public static final String ATTR_PD_CLIENT_KEY = "pd_client";

  private static Logger logger = Logger.getLogger(PagerDutyClient.class.getName());
  private static final String ENV_API_TOKEN_KEY = "PD_API_KEY";

  private static final String PD_API_ENDPOINT = "api.pagerduty.com";
  private static final String PD_REST_API_VERSION = "2";
  private static final ObjectMapper mapper = new ObjectMapper();

  private CloseableHttpClient httpClient;

  public PagerDutyClient() throws Exception {
    String apiToken = System.getenv(ENV_API_TOKEN_KEY);
    if (apiToken == null || apiToken.isEmpty()) {
      throw new Exception("Failed to find API token in " + ENV_API_TOKEN_KEY);
    }
    Collection<Header> headers = Arrays.asList(
        new BasicHeader(HttpHeaders.AUTHORIZATION, "Token token=" + apiToken),
        new BasicHeader(HttpHeaders.ACCEPT, "application/vnd.pagerduty+json;version=" + PD_REST_API_VERSION)
    );
    httpClient = HttpClientBuilder.create().setDefaultHeaders(headers).setDefaultRequestConfig(
        RequestConfig.custom()
            .setConnectTimeout(10_000)
            .setConnectionRequestTimeout(10_000)
            .setSocketTimeout(10_000)
            .build()
    ).build();
  }

  public User getCurrentUser() throws Exception {
    return getUser("me");
  }

  public User getUser(String userId) throws Exception {
    URIBuilder uriBuilder = new URIBuilder()
        .setScheme("https")
        .setHost(PD_API_ENDPOINT)
        .setPathSegments("users", userId)
        ;
    HttpGet getRequest = new HttpGet(uriBuilder.build().toString());
    GetUserResponse resp = httpClient.execute(getRequest, new Handler<>(GetUserResponse.class));
    if(resp != null) {
      return resp.user;
    }
    return null;
  }

  public List<Incident> getTriggeredPagerDutyIncidents(String userId, List<String> serviceIds) throws Exception {
    URIBuilder uriBuilder = new URIBuilder()
        .setScheme("https")
        .setHost(PD_API_ENDPOINT)
        .setPath("incidents")
        .addParameter("user_ids[]", userId)
        .addParameter("statuses[]", "triggered")
        .addParameter("include[]", "services")
        ;
    for (String s : serviceIds) {
      uriBuilder.addParameter("service_ids[]", s);
    }

    List<Incident> ret = new ArrayList<>();

    while (true) {
      HttpGet getRequest = new HttpGet(uriBuilder.build().toString());
      GetIncidentsResponse resp = httpClient.execute(getRequest, new Handler<>(GetIncidentsResponse.class));
      if (resp == null) {
        return ret;
      }
      if (resp.getIncidents() != null) {
        ret.addAll(resp.getIncidents());
      }
      if (resp.isMore()) {
        uriBuilder.setParameter("offset", Integer.toString(ret.size()));
      } else {
        break;
      }
    }
    return ret;

  }

  public void addNoteToIncident(String incidentId, String noteContent) throws Exception {
    URI uri = new URIBuilder()
        .setScheme("https")
        .setHost(PD_API_ENDPOINT)
        .setPathSegments("incidents", incidentId, "notes")
        .build()
        ;
    Note note = new Note();
    note.content = noteContent;

    HttpPost postRequest = new HttpPost(uri.toString());
    postRequest.setEntity(new StringEntity(mapper.writeValueAsString(note), ContentType.APPLICATION_JSON));

    httpClient.execute(postRequest, new Handler<>(Note.class));
  }

  private static class UpdateIncidentRequestEntry {
    private String id;
    private String status;
    private String type = "incident_reference";

    @JsonProperty("escalation_level")
    private Integer escalationLevel;
    private String resolution;

    public String getId() {
      return id;
    }

    public String getStatus() {
      return status;
    }

    public String getType() {
      return type;
    }

    public Integer getEscalationLevel() {
      return escalationLevel;
    }

    public String getResolution() {
      return resolution;
    }
  }

  public void acknowledgeIncidents(List<String> incidentIds) throws Exception {
    manageIncidents(incidentIds, null, null);
  }

  public void escalateIncidents(List<String> incidentIds) throws Exception {
    manageIncidents(null, incidentIds, null);
  }

  public void resolveIncidentsWithResolution(List<String> incidentIds, List<String> resolutions) throws Exception {
    URI uri = new URIBuilder()
        .setScheme("https")
        .setHost(PD_API_ENDPOINT)
        .setPathSegments("incidents")
        .build()
        ;

    List<UpdateIncidentRequestEntry> incidents = new ArrayList<>();

    if (incidentIds != null && !incidentIds.isEmpty()) {
      for(int i = 0; i < incidentIds.size(); i++) {
        UpdateIncidentRequestEntry entry = new UpdateIncidentRequestEntry();
        entry.id = incidentIds.get(i);
        entry.status = "resolved";
        entry.resolution = resolutions.get(i);
      }
    }

    PutIncidentsRequest payload = new PutIncidentsRequest();
    payload.incidents = incidents;

    HttpPut putRequest = new HttpPut(uri.toString());
    putRequest.setEntity(new StringEntity(mapper.writeValueAsString(payload), ContentType.APPLICATION_JSON));
    httpClient.execute(putRequest, new Handler<>(Object.class));
  }

  public void resolveIncident(String incidentId, String resolution) throws Exception {
    URI uri = new URIBuilder()
        .setScheme("https")
        .setHost(PD_API_ENDPOINT)
        .setPathSegments("incidents", incidentId)
        .build()
        ;

    UpdateIncidentRequestEntry entry = new UpdateIncidentRequestEntry();
    entry.status = "resolved";
    if(resolution != null) {
      entry.resolution = resolution;
    }
    PutIncidentRequest payload = new PutIncidentRequest();
    payload.incident = entry;

    HttpPut putRequest = new HttpPut(uri.toString());
    putRequest.setEntity(new StringEntity(mapper.writeValueAsString(payload), ContentType.APPLICATION_JSON));
    httpClient.execute(putRequest, new Handler<>(Object.class));
  }

  public void manageIncidents(List<String> ackIds, List<String> escalateIds, List<String> resolveIds) throws Exception {
    URI uri = new URIBuilder()
        .setScheme("https")
        .setHost(PD_API_ENDPOINT)
        .setPathSegments("incidents")
        .build()
        ;

    List<UpdateIncidentRequestEntry> incidents = new ArrayList<>();
    if (ackIds != null && !ackIds.isEmpty()) {
      incidents.addAll(ackIds.stream().map(id -> {
        UpdateIncidentRequestEntry entry = new UpdateIncidentRequestEntry();
        entry.id = id;
        entry.status = "acknowledged";
        return entry;
      }).collect(Collectors.toSet()));
    }

    if (escalateIds != null && !escalateIds.isEmpty()) {
      incidents.addAll(escalateIds.stream().map(id -> {
        UpdateIncidentRequestEntry entry = new UpdateIncidentRequestEntry();
        entry.id = id;
        entry.escalationLevel = 2;
        return entry;
      }).collect(Collectors.toSet()));
    }

    if (resolveIds != null && !resolveIds.isEmpty()) {
      incidents.addAll(resolveIds.stream().map(id -> {
        UpdateIncidentRequestEntry entry = new UpdateIncidentRequestEntry();
        entry.id = id;
        entry.status = "resolved";
        return entry;
      }).collect(Collectors.toSet()));
    }

    PutIncidentsRequest payload = new PutIncidentsRequest();
    payload.incidents = incidents;

    HttpPut putRequest = new HttpPut(uri.toString());
    putRequest.setEntity(new StringEntity(mapper.writeValueAsString(payload), ContentType.APPLICATION_JSON));
    httpClient.execute(putRequest, new Handler<>(Object.class));
  }

  private class Handler<T> implements ResponseHandler<T> {

    Class<T> clazz;

    public Handler(Class<T> clazz) {
      this.clazz = clazz;
    }

    @Override
    public T handleResponse(HttpResponse httpResponse) throws ClientProtocolException, IOException {
      //Get the status of the response
      int status = httpResponse.getStatusLine().getStatusCode();
      if (status >= 200 && status < 300) {
        HttpEntity entity = httpResponse.getEntity();
        if(entity == null) {
          return null;
        } else {
          return mapper.readValue(httpResponse.getEntity().getContent(), clazz);
        }
      } else {
        throw new IOException("Error happened when requesting from PagerDuty: " + EntityUtils.toString(httpResponse.getEntity()));
      }
    }
  }

  private static class GetIncidentsResponse {
    private int offset;
    private int limit;
    private boolean more;
    private int total;
    private List<Incident> incidents;

    public int getOffset() {
      return offset;
    }

    public int getLimit() {
      return limit;
    }

    public boolean isMore() {
      return more;
    }

    public int getTotal() {
      return total;
    }

    public List<Incident> getIncidents() {
      return incidents;
    }
  }

  private static class PutIncidentsRequest {
    private List<UpdateIncidentRequestEntry> incidents;

    public List<UpdateIncidentRequestEntry> getIncidents() {
      return incidents;
    }
  }

  private static class PutIncidentRequest {
    private UpdateIncidentRequestEntry incident;

    public UpdateIncidentRequestEntry getIncident() {
      return incident;
    }
  }

  private static class GetUserResponse {
    private User user;

    public User getUser() {
      return user;
    }
  }

  public static class Reference {
    String id;
    String type;
    String summary;
    String self;
    @JsonProperty("html_url")
    String htmlUrl;

    public String getId() {
      return id;
    }

    public String getType() {
      return type;
    }

    public String getSummary() {
      return summary;
    }

    public String getSelf() {
      return self;
    }

    public String getHtmlUrl() {
      return htmlUrl;
    }
  }

  public static class Incident extends Reference {
    private Map<String, Object> data = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getData() {
      return data;
    }

    @JsonAnySetter
    public void setData(String name, Object value) {
      data.put(name, value);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class User extends Reference {
    private String email;

    public String getEmail() {
      return email;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Note {
    private String content;

    public String getContent() {
      return content;
    }
  }

  public static void main(String[] args) throws Exception {
    PagerDutyClient pd = new PagerDutyClient();
    pd.getTriggeredPagerDutyIncidents("",null).forEach(i -> System.out.println(i.data));
  }
}
