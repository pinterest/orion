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
package com.pinterest.orion.server;

public class MetricsConstants {

  public static final String HEARTBEAT = "heartbeat";

  public static final String REQUEST_PREFIX = "request.";

  // action metrics
  public static final String ACTION_PREFIX = REQUEST_PREFIX + "action.";

  // node registration metrics
  public static final String NODEREGISTRATION_PREFIX = REQUEST_PREFIX + "noderegistration.";
  public static final String REGISTRATION_BAD_REQUEST = NODEREGISTRATION_PREFIX + "badrequest";
  public static final String REGISTRATION_INVALID_CLUSTER = NODEREGISTRATION_PREFIX
      + "invalidcluster";
  public static final String REGISTRATION_VALID_REQUEST = NODEREGISTRATION_PREFIX + "validrequest";
  public static final String METRICS_INGEST_EXCEPTION = "metrics.ingest.exception";

}
