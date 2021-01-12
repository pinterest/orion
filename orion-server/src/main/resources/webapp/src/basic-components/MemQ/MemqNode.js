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
import React from "react";
import {
  Tab,
  Tabs,
  Grid,
  Typography,
  Box,
  Chip,
  Link,
} from "@material-ui/core";
import { Link as RouterLink, Redirect, Route, Switch } from "react-router-dom";
import { makeStyles } from "@material-ui/core/styles";
import PropsTable from "./PropsTable";

const routes = [
  {
    subpath: "brokerprops",
    component: PropsTable,
    label: "Broker Properties",
    getData: getServiceConfigsData,
    getColumns: getServiceConfigsColumns,
  },
  {
    subpath: "brokerenv",
    component: PropsTable,
    label: "Broker Environment",
    getData: getBrokerEnvironmentData,
    getColumns: getBrokerEnvironmentColumns,
  },
];

export default function MemqNode({ node, clusterId, cluster }) {
  return (
    <div>
      {getNodeInfoHeader(
        node,
        node.currentNodeInfo.nodeId,
        node.currentNodeInfo.serviceInfo["broker.id"],
        node.currentNodeInfo.hostname.split(".", 1)[0]
      )}
      <Grid>
        <Grid item xs={10}>
          <Switch>
            <Redirect
              exact
              from="/clusters/:clusterId/nodes/:nodeId"
              to="/clusters/:clusterId/nodes/:nodeId/brokerprops"
            ></Redirect>
            <Route
              path="/clusters/:clusterId/nodes/:nodeId/:tab"
              children={NodeNavTabs}
            />
          </Switch>
        </Grid>
        <Grid item xs={12}>
          <Switch>
            {routes.map((route, idx) => {
              return (
                <Route
                  key={idx}
                  exact
                  path={"/clusters/:clusterId/nodes/:nodeId/" + route.subpath}
                >
                  {
                    <route.component
                      data={route.getData(cluster, node)}
                      columns={route.getColumns()}
                    />
                  }
                </Route>
              );
            })}
          </Switch>
        </Grid>
      </Grid>
    </div>
  );
}

function getNodeInfoHeader(node, nodeId, brokerId, hostname) {
  let nodeType = "Node type not available";
  if (node.agentNodeInfo) {
    nodeType = node.agentNodeInfo.nodeType;
  }
  return (
    <div>
      <Box my={2}>
        <Grid container display="flex" alignItems="center" spacing={2}>
          <Grid item>
            <Typography variant="h6">{hostname}</Typography>
          </Grid>
          <Grid item>
            <Chip variant="outlined" label="Kafka Node" size="small" />
          </Grid>
          <Grid item>
            <Chip label={"Broker ID: " + brokerId} size="small" />
          </Grid>
          <Grid item>
            <Chip
              variant="outlined"
              color="primary"
              size="small"
              label={0 + " topics"}
            />
          </Grid>
          <Grid item>
            <Chip
              variant="outlined"
              color="primary"
              size="small"
              label={nodeType}
            />
          </Grid>
          <Grid item>
            <Chip
              variant="outlined"
              color="primary"
              size="small"
              label={node.currentNodeInfo.rack}
            />
          </Grid>
        </Grid>
      </Box>
    </div>
  );
}

function bytesToGB(bytes) {
  return bytes / 1073741824;
}

function nodeToLink(arry, clusterId) {
  return arry.map((node, i) => (
    <Link
      component={RouterLink}
      key={i}
      to={"/clusters/" + clusterId + "/nodes/" + node}
    >
      {node}
      {i != arry.length - 1 ? "," : ""}
    </Link>
  ));
}

function getBrokerEnvironmentData(cluster, node) {
  let environmentConfigRows = [];
  if (node && node.agentNodeInfo) {
    let attributes = node.agentNodeInfo.environment;
    let tmp = Object.entries(attributes);
    for (let [key, value] of tmp) {
      environmentConfigRows.push({ key: key, value: JSON.stringify(value) });
    }
  }
  let environmentData = environmentConfigRows.map((entry) => {
    return {
      key: entry.key,
      value: entry.value,
    };
  });
  return environmentData;
}

function getBrokerEnvironmentColumns() {
  return [
    { title: "Environment Variable", field: "key" },
    { title: "Value", field: "value" },
  ];
}

function getServiceConfigsData(cluster, node) {
  let serviceConfigRows = [];
  if (node) {
    let attributes = node.currentNodeInfo.serviceInfo;
    let tmp = Object.entries(attributes);
    for (let [key, value] of tmp) {
      serviceConfigRows.push({ key: key, value: JSON.stringify(value) });
    }
  }
  let serviceConfigsData = serviceConfigRows.map((entry) => {
    return {
      key: entry.key,
      value: entry.value,
    };
  });
  return serviceConfigsData;
}

function getServiceConfigsColumns() {
  return [
    { title: "Config Name", field: "key" },
    { title: "Value", field: "value" },
  ];
}

function NodeNavTabs(props) {
  return (
    <Tabs
      value={props.match.params.tab}
      style={{ backgroundColor: "white", width: "100%", maxWidth: "100%" }}
    >
      {routes.map((route, idx) => (
        <Tab
          key={idx}
          value={route.subpath}
          label={route.label}
          to={route.subpath}
          component={RouterLink}
        />
      ))}
    </Tabs>
  );
}
