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
export const CLUSTERS_SUMMARY_REQUESTED = "CLUSTERS_SUMMARY_REQUESTED";
export const CLUSTERS_SUMMARY_RECEIVED = "CLUSTERS_SUMMARY_RECEIVED";
export const CLUSTERS_SUMMARY_MAINTENANCE_STATUS_RECEIVED = "CLUSTERS_SUMMARY_MAINTENANCE_STATUS_RECEIVED";

export function receiveClustersSummary(clustersSummary) {
  return { type: CLUSTERS_SUMMARY_RECEIVED, payload: clustersSummary };
}

export function requestClustersSummary() {
  return { type: CLUSTERS_SUMMARY_REQUESTED };
}

export function receiveClustersSummaryMaintenanceStatus(clusterId, underMaintenance) {
  return { type: CLUSTERS_SUMMARY_MAINTENANCE_STATUS_RECEIVED, payload: {clusterId, underMaintenance} };
}
