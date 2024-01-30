/*******************************************************************************
 * Copyright 2024 Pinterest, Inc.
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

public class Ami {
  private final String amiId;
  private final String applicationEnvironment;
  private final String creationDate;

  public Ami(String amiId, String applicationEnvironment, String creationDate) {
    this.amiId = amiId;
    this.applicationEnvironment = applicationEnvironment;
    this.creationDate = creationDate;
  }

  public String getAmiId() {
    return amiId;
  }

  public String getApplicationEnvironment() {
    return applicationEnvironment;
  }

  public String getCreationDate() {
    return creationDate;
  }
}
