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
export const AMI_LIST_REQUESTED = "AMI_LIST_REQUESTED";
export const AMI_LIST_RECEIVED = "AMI_LIST_RECEIVED";
export const AMI_TAG_UPDATE = "AMI_TAG_UPDATE";
export const ENV_TYPES_REQUESTED = "ENV_TYPES_REQUESTED";
export const ENV_TYPES_RECEIVED = "ENV_TYPES_RECEIVED";

export function requestAmiList(filter) {
  return {
    type: AMI_LIST_REQUESTED,
    payload: { filter },
  };
}

export function receiveAmiList(amiList) {
  return {
    type: AMI_LIST_RECEIVED,
    payload: { amiList },
  };
}

export function updateAmiTag(amiList, amiId, applicationEnvironment) {
  return {
    type: AMI_TAG_UPDATE,
    payload: { amiList, amiId, applicationEnvironment },
  };
}

export function requestEnvTypes() {
  return {
    type: ENV_TYPES_REQUESTED,
    payload: {},
  };
}

export function receiveEnvTypes(envTypeList) {
  return {
    type: ENV_TYPES_RECEIVED,
    payload: { envTypeList },
  };
}
