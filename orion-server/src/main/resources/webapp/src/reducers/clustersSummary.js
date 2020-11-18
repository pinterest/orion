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
import { CLUSTERS_SUMMARY_RECEIVED, CLUSTERS_SUMMARY_MAINTENANCE_STATUS_RECEIVED } from "../actions/clusterSummary";

export default function clustersSummary(state = [], action) {
  switch (action.type) {
    case CLUSTERS_SUMMARY_RECEIVED:
      return [...action.payload];
    case CLUSTERS_SUMMARY_MAINTENANCE_STATUS_RECEIVED:
      for (let clusterIdx = 0; clusterIdx < state.length; clusterIdx++) {
        if (state[clusterIdx].clusterId === action.payload.clusterId) {
          let newState = [...state];
          newState[clusterIdx].underMaintenance = action.payload.underMaintenance;
          return newState;
        };
      }
      return state;
    default:
      return state;
  }
}
