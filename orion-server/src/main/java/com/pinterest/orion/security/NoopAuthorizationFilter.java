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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.container.ContainerRequestContext;

import com.pinterest.orion.server.config.OrionConf;

public class NoopAuthorizationFilter implements OrionAuthorizationFilter {

  private static final Set<String> ADMIN_ROLE_SET = new HashSet<>(
      Arrays.asList(OrionConf.ADMIN_ROLE));

  @Override
  public void configure(OrionConf config) throws Exception {

  }

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    OrionSecurityContext
        ctx = new OrionSecurityContext(new UserPrincipal("anonymous", true), ADMIN_ROLE_SET);
    requestContext.setSecurityContext(ctx);
  }

}
