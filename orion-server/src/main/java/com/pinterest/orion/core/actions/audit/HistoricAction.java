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
package com.pinterest.orion.core.actions.audit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pinterest.orion.core.actions.Action;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoricAction extends Action {

  private String name;
  private Type type;

  public HistoricAction() {
  }

  public HistoricAction(Action action) {
    name = action.getName();
    type = action.getActionType();
    this.setStatus(action.getStatus());
    this.setResult(action.getResult());
    this.setChildren(action.getChildren());
    this.setUuid(action.getUuid());
    this.setOwner(action.getOwner());
    this.setCreateTime(action.getCreateTime());
    this.setCompleteTime(action.getCompleteTime());
    this.getAttributes().putAll(action.getAttributes());
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public void runAction() {
  }

  @Override
  public Type getActionType() {
    return type;
  }

  public void setActionType(Type type) {
    this.type = type;
  }

  @JsonProperty("children")
  public void setHistoricChildren(List<HistoricAction> children) {
    super.setChildren(new ArrayList<>(children));
  }
}