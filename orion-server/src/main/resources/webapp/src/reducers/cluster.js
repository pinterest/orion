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
import {
  CLUSTER_RECEIVED,
  CLUSTER_REQUEST_FAILED,
  CLUSTER_ACTIONS_RECEIVED,
  CLUSTER_MAINTENANCE_STATUS_RECEIVED,
  CLUSTER_ENDPOINT_RECEIVED
} from "../actions/cluster";

export default function clusterView(
  state = {
    cluster: {},
  },
  action
) {
  switch (action.type) {
    case CLUSTER_RECEIVED:
      return { ...state, cluster: action.payload };
    case CLUSTER_REQUEST_FAILED:
      return { ...state, cluster: {}, fetchError: action.payload };
    case CLUSTER_ACTIONS_RECEIVED:
      if(state.cluster && state.cluster.clusterId === action.payload.clusterId && state.cluster.actionEngine){
          const actionEngine = { ...state.cluster.actionEngine, trackedActionsList: action.payload.clusterActions };
          return {...state, cluster: { ...state.cluster, actionEngine}};
      }
        return state;
    case CLUSTER_MAINTENANCE_STATUS_RECEIVED:
      if(state.cluster && state.cluster.clusterId === action.payload.clusterId) {
        return {...state, cluster: { ...state.cluster, underMaintenance: action.payload.underMaintenance}}
      }
      return state;
    case CLUSTER_ENDPOINT_RECEIVED:
      if(state.cluster && state.cluster.clusterId === action.payload.clusterId && state.cluster.attributes) {
        let ret = {...state, cluster: {...state.cluster}};
        ret.cluster.attributes[action.payload.field] = action.payload.data;
        return ret;
      }
      return state;
    default:
      return state;
  }
}