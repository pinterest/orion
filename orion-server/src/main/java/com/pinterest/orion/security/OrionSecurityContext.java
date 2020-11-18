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
package com.pinterest.orion.security;

import java.security.Principal;
import java.util.Set;

import javax.ws.rs.core.SecurityContext;

public class OrionSecurityContext implements SecurityContext {
  
  private static final String ORION_AUTH = "orionauth";
  private UserPrincipal principal;
  private Set<String> roles;

  public OrionSecurityContext(UserPrincipal principal, Set<String> roles) {
    this.principal = principal;
    this.roles = roles;
  }
  
  @Override
  public Principal getUserPrincipal() {
    return principal;
  }

  @Override
  public boolean isUserInRole(String role) {
    return roles.contains(role);
  }

  @Override
  public boolean isSecure() {
    return true;
  }

  @Override
  public String getAuthenticationScheme() {
    return ORION_AUTH;
  }

  @Override
  public String toString() {
    return "OrionSecurityContext [principal=" + principal + ", roles=" + roles + "]";
  }

}