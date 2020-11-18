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
import { USER_RECEIVED, USER_REQUEST_FAILED } from "../actions/user";


export default function user(state = { name: null, auth: false }, action) {
    switch (action.type) {
      case USER_RECEIVED:
        return { ...state, name: action.payload.name, auth: true, isAdmin: action.payload.admin };
      case USER_REQUEST_FAILED:
        return { ...state, name: "N/A", auth: false};
      default:
        return state;
    }
  }
