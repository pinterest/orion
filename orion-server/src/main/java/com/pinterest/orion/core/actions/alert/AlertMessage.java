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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pinterest.orion.core.Context;
import com.pinterest.orion.core.utils.OrionUUID;

public class AlertMessage extends Context {

  private OrionUUID uuid;
  @JsonProperty
  private String title;
  @JsonProperty
  private String body;
  @JsonProperty
  private volatile boolean isRead = false;
  @JsonProperty
  private String link;
  private String owner;
  private String entity;

  public AlertMessage(String title, String body, String owner) {
    this(title, body, owner, "orion");
  }

  public AlertMessage(String title, String body, String owner, String entity) {
    this.owner = owner;
    this.uuid = new OrionUUID();
    this.title = title;
    this.body = body;
    this.entity = entity;
  }

  @JsonIgnore
  public OrionUUID getUuid() {
    return uuid;
  }

  @JsonProperty("uuid")
  public String getUuidString() {
    return uuid.toString();
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  /**
   * @return the link
   */
  public String getLink() {
    return link;
  }


  public String getOwner() {
    return owner;
  }

  /**
   * @param link the link to set
   */
  public void setLink(String link) {
    this.link = link;
  }

  @JsonProperty("timestamp")
  public long getTimestamp() {
    return uuid.getTimestamp();
  }

  @JsonProperty("isRead")
  public boolean isRead() {
    return isRead;
  }

  @JsonIgnore
  public void setRead(boolean read) {
    isRead = read;
  }

  public String getEntity() {
    return entity;
  }

  public void setEntity(String entity) {
    this.entity = entity;
  }
}
