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
export const CLUSTER_REQUESTED = "CLUSTER_REQUESTED";
export const CLUSTER_RECEIVED = "CLUSTER_RECEIVED";
export const CLUSTER_REQUEST_FAILED = "CLUSTER_REQUEST_FAILED";
export const CLUSTER_ACTIONS_RECEIVED = "CLUSTER_ACTIONS_RECEIVED";
export const CLUSTER_ACTIONS_REQUEST_FAILED = "CLUSTER_ACTIONS_REQUEST_FAILED";
export const CLUSTER_ACTIONS_SYNC_STARTED = "CLUSTER_ACTIONS_SYNC_STARTED";
export const CLUSTER_ACTIONS_SYNC_STOPPED = "CLUSTER_ACTIONS_SYNC_STOPPED";
export const CLUSTER_MAINTENANCE_STATUS_RECEIVED =
  "CLUSTER_MAINTENANCE_STATUS_RECEIVED";
export const CLUSTER_ENDPOINT_REQUESTED = "CLUSTER_ENDPOINT_REQUESTED";
export const CLUSTER_ENDPOINT_REQUEST_FAILED =
  "CLUSTER_ENDPOINT_REQUESTED_FAILED";
export const CLUSTER_ENDPOINT_RECEIVED = "CLUSTER_ENDPOINT_RECEIVED";
export const UTILIZATION_REQUESTED = "UTILIZATION_REQUESTED";
export const UTILIZATION_RECEIVED = "UTILIZATION_RECEIVED";
export const COST_REQUESTED = "COST_REQUESTED";
export const COST_RECEIVED = "COST_RECEIVED";

export function requestCluster(clusterId) {
  return { type: CLUSTER_REQUESTED, payload: { clusterId } };
}

export function receiveCluster(cluster) {
  return { type: CLUSTER_RECEIVED, payload: cluster };
}

export function requestClusterFail(error) {
  return { type: CLUSTER_REQUEST_FAILED, payload: error };
}

export function startClusterActionsSync(clusterId) {
  return { type: CLUSTER_ACTIONS_SYNC_STARTED, payload: clusterId };
}

export function stopClusterActionsSync() {
  return { type: CLUSTER_ACTIONS_SYNC_STOPPED };
}

export function receiveClusterActions(clusterId, clusterActions) {
  return {
    type: CLUSTER_ACTIONS_RECEIVED,
    payload: { clusterId, clusterActions },
  };
}

export function requestClusterActionsFail(error) {
  return { type: CLUSTER_ACTIONS_REQUEST_FAILED, payload: error };
}

export function requestUtilization() {
  return {
    type: UTILIZATION_REQUESTED,
    payload: {},
  };
}

export function receiveUtilization(utilization) {
  return {
    type: UTILIZATION_RECEIVED,
    payload: { utilization },
  };
}

export function requestCost() {
  return {
    type: COST_REQUESTED,
    payload: {},
  };
}

export function receiveCost(cost) {
  return {
    type: COST_RECEIVED,
    payload: { cost },
  };
}

export function receiveClusterMaintenanceStatus(clusterId, underMaintenance) {
  return {
    type: CLUSTER_MAINTENANCE_STATUS_RECEIVED,
    payload: { clusterId, underMaintenance },
  };
}

export function requestClusterEndpointFail(error, endpoint) {
  return {
    type: CLUSTER_ENDPOINT_REQUEST_FAILED,
    payload: { error, endpoint },
  };
}

export function receiveClusterEndpoint(clusterId, field, data) {
  return {
    type: CLUSTER_ENDPOINT_RECEIVED,
    payload: { clusterId, field, data },
  };
}
