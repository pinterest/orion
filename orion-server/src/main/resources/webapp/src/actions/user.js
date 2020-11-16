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
export const USER_REQUESTED = "USER_REQUESTED";
export const USER_RECEIVED = "USER_RECEIVED";
export const USER_REQUEST_FAILED = "USER_REQUEST_FAILED";

export function requestUser() {
  return { type: USER_REQUESTED };
}

export function receiveUser(user) {
  return { type: USER_RECEIVED, payload: user };
}

export function requestUserFail() {
  return { type: USER_REQUEST_FAILED };
}
