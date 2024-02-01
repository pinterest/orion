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
  ERROR_VIEW_ENABLED,
  ERROR_VIEW_DISABLED,
  LOADING_BACKDROP_ENABLED,
  LOADING_BACKDROP_DISABLED,
  APP_INITIALIZED,
  AUTO_REFRESH_ENABLED,
  AUTO_REFRESH_DISABLED,
} from "../actions/app";
import { UTILIZATION_RECEIVED, COST_RECEIVED, AMI_LIST_RECEIVED } from "../actions/cluster";

export default function showError(
  state = {
    showError: false,
    showLoading: false,
    isInit: false,
  },
  action
) {
  switch (action.type) {
    case ERROR_VIEW_ENABLED:
      return { ...state, showError: true };
    case ERROR_VIEW_DISABLED:
      return { ...state, showError: false };
    case LOADING_BACKDROP_ENABLED:
      return { ...state, showLoading: true };
    case LOADING_BACKDROP_DISABLED:
      return { ...state, showLoading: false };
    case APP_INITIALIZED:
      return { ...state, isInit: true };
    case AUTO_REFRESH_ENABLED:
      const { lastUpdateTime, interval } = action.payload;
      return { ...state, refreshInfo: { lastUpdateTime, interval } };
    case AUTO_REFRESH_DISABLED:
      return { ...state, refreshInfo: null };
    case UTILIZATION_RECEIVED:
      return { ...state, utilization: action.payload.utilization };
    case COST_RECEIVED:
      return { ...state, cost: action.payload.cost };
    case AMI_LIST_RECEIVED:
      return { ...state, amiList: action.payload.amiList };
    default:
      return state;
  }
}
