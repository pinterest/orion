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
  call,
  delay,
  put,
  takeEvery,
  take,
  cancel,
  cancelled,
  fork,
} from "redux-saga/effects";
import {
  CLUSTER_REQUESTED,
  CLUSTER_ACTIONS_SYNC_STARTED,
  CLUSTER_ACTIONS_SYNC_STOPPED,
  receiveCluster,
  requestClusterFail,
  requestClusterActionsFail,
  receiveClusterActions,
  CLUSTER_ENDPOINT_REQUESTED,
  receiveClusterEndpoint,
  requestClusterEndpointFail,
  UTILIZATION_REQUESTED,
  receiveUtilization,
  COST_REQUESTED,
  receiveCost,
} from "../actions/cluster";
import {
  CLUSTERS_SUMMARY_REQUESTED,
  receiveClustersSummary,
  requestClustersSummary,
  GLOBAL_SENSOR_REQUESTED,
  requestGlobalSensor,
  receiveGlobalSensor,
} from "../actions/clusterSummary";
import { USER_REQUESTED, receiveUser, requestUserFail } from "../actions/user";
import {
  showAppError,
  hideAppError,
  showLoading,
  hideLoading,
  showAutoRefreshTimer,
  hideAutoRefreshTimer,
} from "../actions/app";

export default function* rootSaga() {
  yield fork(clusterSummaryWatcher);
  yield fork(userWatcher);
  yield fork(clusterWatcher);
  yield fork(syncClusterActions);
  yield fork(fetchCustomEndpoint);
  yield fork(utilizationWatcher);
  yield fork(costWatcher);
  yield fork(globalSensorWatcher);
}

function* clusterSummaryWatcher() {
  yield takeEvery(CLUSTERS_SUMMARY_REQUESTED, fetchClusterSummaryAndRetry);
}

function* utilizationWatcher() {
  yield takeEvery(UTILIZATION_REQUESTED, fetchUtilization);
}

function* costWatcher() {
  yield takeEvery(COST_REQUESTED, fetchCost);
}

function* userWatcher() {
  yield takeEvery(USER_REQUESTED, fetchUser);
}

function* clusterWatcher() {
  yield takeEvery(CLUSTER_REQUESTED, fetchCluster);
}

function* globalSensorWatcher() {
  yield takeEvery(GLOBAL_SENSOR_REQUESTED, fetchGlobalSensors);
}

function* fetchCost() {
  try {
    const resp = yield fetch("/api/costByCluster");
    const data = yield resp.json();
    yield put(hideAppError());
    yield put(receiveCost(data));
  } catch (e) {}
}

function* fetchUtilization() {
  try {
    const resp = yield fetch("/api/utilizationDetailsByCluster");
    const data = yield resp.json();
    yield put(hideAppError());
    yield put(receiveUtilization(data));
  } catch (e) {
    yield put(showAppError(e));
  }
}

function* fetchClusterSummaryAndRetry(action) {
  try {
    const resp = yield fetch("/api/clusterssummary");
    const data = yield resp.json();
    yield put(hideAppError());
    yield put(receiveClustersSummary(data));
  } catch (e) {
    yield put(showAppError(e));
    yield delay(5000);
    yield put(requestClustersSummary());
  }
}

function* fetchGlobalSensors(action) {
  try {
    const resp = yield fetch("/api/globalsensors");
    const data = yield resp.json();
    yield put(hideAppError());
    yield put(receiveGlobalSensor(data));
  } catch (e) {
    yield put(showAppError(e));
    yield delay(5000);
    yield put(requestGlobalSensor());
  }
}

function* fetchUser(action) {
  try {
    const resp = yield call(fetch, "/api/user");
    const data = yield resp.json();
    yield put(receiveUser(data));
  } catch (e) {
    yield put(requestUserFail());
  }
}

function* fetchCluster(action) {
  try {
    yield put(showLoading());
    const resp = yield call(fetch, "/api/clusters/" + action.payload.clusterId);
    const data = yield resp.json();
    if (!resp.ok) {
      yield put(requestClusterFail(data));
    } else {
      yield put(receiveCluster(data));
    }
  } catch (e) {
    yield put(requestClusterFail(e));
  } finally {
    yield put(hideLoading());
  }
}

function* syncClusterActions() {
  while (true) {
    const action = yield take(CLUSTER_ACTIONS_SYNC_STARTED);
    const syncTask = yield fork(fetchClusterActions, action);
    yield take(CLUSTER_ACTIONS_SYNC_STOPPED);
    yield cancel(syncTask);
  }
}

function* fetchClusterActions(action) {
  const clusterId = action.payload;
  try {
    while (true) {
      try {
        const resp = yield call(
          fetch,
          "/api/clusters/" + clusterId + "/actions"
        );
        const data = yield resp.json();
        if (resp.ok) {
          yield put(receiveClusterActions(clusterId, data));
        }
      } catch (e) {
        yield put(requestClusterActionsFail());
      }
      yield put(showAutoRefreshTimer(Date.now(), 10000));
      yield delay(10000);
    }
  } finally {
    if (yield cancelled()) {
      yield put(requestClusterActionsFail());
      yield put(hideAutoRefreshTimer());
    }
  }
}

function* fetchCustomEndpoint() {
  yield takeEvery(CLUSTER_ENDPOINT_REQUESTED, fetchClusterCustomEndpoint);
}

function* fetchClusterCustomEndpoint(action) {
  const clusterId = action.payload.clusterId;
  const endpoint = action.payload.endpoint;
  const field = action.payload.field;
  try {
    yield put(showLoading());
    const resp = yield call(
      fetch,
      "/api/clusters/" + clusterId + "/" + endpoint
    );
    const data = yield resp.json();
    if (!resp.ok) {
      yield put(requestClusterEndpointFail(endpoint, data));
    } else {
      yield put(receiveClusterEndpoint(clusterId, field, data));
    }
  } catch (e) {
    yield put(requestClusterFail(e));
  } finally {
    yield put(hideLoading());
  }
}
