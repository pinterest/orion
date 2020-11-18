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

import javax.ws.rs.container.ContainerRequestFilter;

import com.pinterest.orion.server.config.OrionConf;
/**
 * This extends JAX-RS containter request filter for authorization. 
 * 
 * Please refer to https://docs.oracle.com/javaee/7/api/javax/ws/rs/container/ContainerRequestFilter.html
 * for more details on how {@link ContainerRequestFilter} works
 */
public interface OrionAuthorizationFilter extends ContainerRequestFilter {
  
  public void configure(OrionConf config) throws Exception;

}