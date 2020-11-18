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
import PropsTable from "../Commons/PropsTable";

const routes = [
  {
    subpath: "regions",
    component: PropsTable,
    label: "Regions",
    getData: getRegions,
    getColumns: getRegionColumns,
  },
  {
    subpath: "serverprops",
    component: PropsTable,
    label: "Server Properties",
    getData: getServiceConfigsData,
    getColumns: getServiceConfigsColumns,
  },
  {
    subpath: "serverenv",
    component: PropsTable,
    label: "Server Environment",
    getData: getRegionServerEnvironmentData,
    getColumns: getRegionServerEnvironmentColumns,
  },
];

export default function HBaseNode({ node, clusterId, cluster }) {
  return (
    <div>
      {getNodeInfoHeader(
        cluster,
        node,
        node.currentNodeInfo.hostname.split(".", 1)[0]
      )}
      <Grid>
        <Grid item xs={10}>
          <Switch>
            <Redirect
              exact
              from="/clusters/:clusterId/nodes/:nodeId"
              to="/clusters/:clusterId/nodes/:nodeId/regions"
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

function getNodeInfoHeader(cluster, node, hostname) {
  let nodeId = node.currentNodeInfo.nodeId;
  let regionserverOnlineRegions =
    cluster.attributes.regionserverOnlineRegions.value[nodeId];
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
            <Chip variant="outlined" label="HBase Region Server" size="small" />
          </Grid>
          <Grid item>
            <Chip
              label={"Regions: " + regionserverOnlineRegions.length}
              size="small"
            />
          </Grid>
          {/* <Grid item>
            <Chip
              variant="outlined"
              color="primary"
              size="small"
              label={node.topicPartitionsForNode.length + " topics"}
            />
          </Grid> */}
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

function getRegions(cluster, node) {
  let rows = [];
  let nodeId = node.currentNodeInfo.nodeId;
  let clusterId = node.currentNodeInfo.clusterId;
  if (node) {
    let regionserverOnlineRegions =
      cluster.attributes.regionserverOnlineRegions.value[nodeId];
    rows = regionserverOnlineRegions;
  }
  let data = [];
  rows.map((row) => {
    data.push({
      table: (
        <Link
          component={RouterLink}
          to={"/clusters/" + clusterId + "/service/table/" + row.tableName}
        >
          {row.table.nameAsString}
        </Link>
      ),
      region: row.regionNameAsString,
      startKey: row.startKey,
      endKey: row.endKey,
    });
  });
  return data;
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

function getRegionColumns() {
  return [
    { title: "Table", field: "table" },
    { title: "Region", field: "region" },
    { title: "Start Key", field: "startKey" },
    { title: "End Key", field: "endKey" },
  ];
}

function getRegionServerEnvironmentData(cluster, node) {
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

function getRegionServerEnvironmentColumns() {
  return [
    { title: "Environment Variable", field: "key" },
    { title: "Value", field: "value" },
  ];
}

function getServiceConfigsData(cluster, node) {
  let serviceConfigRows = [];
  if (node && node.currentNodeInfo.serviceInfo) {
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
