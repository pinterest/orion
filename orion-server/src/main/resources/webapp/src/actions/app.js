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
export const ERROR_VIEW_ENABLED = "ERROR_VIEW_ENABLED";
export const ERROR_VIEW_DISABLED = "ERROR_VIEW_DISABLED";
export const LOADING_BACKDROP_ENABLED = "LOADING_BACKDROP_ENABLED";
export const LOADING_BACKDROP_DISABLED = "LOADING_BACKDROP_DISABLED";
export const APP_INITIALIZED = "APP_INITIALIZED";
export const AUTO_REFRESH_ENABLED = "AUTO_REFRESH_ENABLED";
export const AUTO_REFRESH_DISABLED = "AUTO_REFRESH_DISABLED";

export function showAppError(e) {
  return { type: ERROR_VIEW_ENABLED, payload: e };
}

export function hideAppError() {
  return { type: ERROR_VIEW_DISABLED };
}

export function showLoading() {
  return { type: LOADING_BACKDROP_ENABLED };
}

export function hideLoading() {
  return { type: LOADING_BACKDROP_DISABLED };
}

export function initializeApp() {
  return { type: APP_INITIALIZED };
}

export function showAutoRefreshTimer(lastUpdateTime, interval) {
  return { type: AUTO_REFRESH_ENABLED, payload: { lastUpdateTime, interval } };
}

export function hideAutoRefreshTimer() {
  return { type: AUTO_REFRESH_DISABLED };
}
