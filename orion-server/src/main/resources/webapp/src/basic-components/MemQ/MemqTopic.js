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
import { Tab, Tabs, Grid, Typography, Box, Chip, Link } from "@material-ui/core";
import { Link as RouterLink, Redirect, Route, Switch } from "react-router-dom";
import { makeStyles } from "@material-ui/core/styles";
import PropsTable from "../Commons/PropsTable";

const routes = [
  {
    subpath: "topicpartitions",
    component: PropsTable,
    label: "Topic Partitions",
    getData: getTopicPartitionsData,
    getColumns: getTopicPartitionsColumns,
  },
  {
    subpath: "topicconfigs",
    component: PropsTable,
    label: "Topic Configurations",
    getData: getTopicConfigData,
    getColumns: getTopicConfigColumns
  },
];

export default function MemqTopic(props) {
  let rowData = props.rowData;
  let clusterId = props.clusterId;

  return (
    <div>
      {getTopicInfoHeader(rowData, clusterId)}
      <Grid>
        <Grid item xs={10}>
          <Switch>
            <Redirect
              exact
              from="/clusters/:clusterId/service/topics/:topicName"
              to="/clusters/:clusterId/service/topics/:topicName/topicpartitions"
            ></Redirect>
            <Route
              path="/clusters/:clusterId/service/topics/:topicName/:tab"
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
                  path={"/clusters/:clusterId/service/topics/:topicName/" + route.subpath}
                >
                {<route.component
                  data={route.getData(rowData)}
                  columns={route.getColumns()}
                />}
                </Route>
              );
            })}
          </Switch>
        </Grid>
      </Grid>
    </div>
  );
}

function getBrokerset(rowData) {
  if (rowData.brokerset === "n/a") {
    return "No brokerset assigned"
  }
  return rowData.brokerset;
}

function getTopicInfoHeader(rowData, clusterId) {
  let topic = rowData.topic;
  return (
    <Box my={2}>
      <Grid container display="flex" alignItems="center" spacing={2}>
        <Grid item>
          <Typography variant="h6">
            {topic}
          </Typography>
        </Grid>
        <Grid item>
          <Chip variant="outlined" label="MemQ Topic" size="small" />
        </Grid>
        <Grid item>
          <Chip
            variant="outlined"
            color="primary"
            size="small"
            label={rowData.retentionHrs + " Hrs"}
          />
        </Grid>
        <Grid item>
          <Chip
            variant="outlined"
            color="primary"
            size="small"
            label={rowData.size + " TB"}
          />
        </Grid>
        <Grid item>
          <Chip
            variant="outlined"
            color="primary"
            size="small"
            label={rowData.partition + " Partitions"}
          />
        </Grid>
        <Grid item>
          <Chip
            variant="outlined"
            color="primary"
            size="small"
            label={getBrokerset(rowData)}
          />
        </Grid>
      </Grid>
    </Box>
  );
}

function getTopicPartitionsData(rowData) {
  let rows = [];
  let topic = rowData.raw;
  if (topic) {
    rows = topic.partitions;
  }
  let data = [];
  rows.map(partition => {
    data.push({
      partition: partition.partition,
      isrs: <Box>{nodeToLink(partition.isrs, rowData.clusterId)}</Box>,
      replicas: <Box>{nodeToLink(partition.replicas, rowData.clusterId)}</Box>,
      leader: partition.replicas[0],
      underReplicatedCount: partition.replicas.length - partition.isrs.length > 0 ? "Yes" : "No",
      isPreferredLeader:
        partition.leader === partition.replicas[0] ? "True" : "False",
      size: bytesToGB(
        Object.values(partition.replicainfo)
          .map(e => e.size)
          .reduce((l1, l2) => l1 + l2, 0)
      ).toFixed(2)
    });
  });
  return data;
}

function getTopicPartitionsColumns() {
  return ([
    { title: "Partition", field: "partition" },
    { title: "ISRs", field: "isrs" },
    { title: "Replicas", field: "replicas" },
    { title: "Leader", field: "leader"},
    { title: "Under Replicated", field: "underReplicatedCount"},
    { title: "Preferred Leader?", field: "isPreferredLeader" },
    { title: "Size (GB)", field: "size", type: "numeric" }
  ]);
}

function getTopicConfigData(rowData) {
  let topicConfigData = [];
  if (rowData) {
    let attributes = rowData.configs;
    let overrideConfigs = rowData.raw.overrideConfigs;
    let tmp = Object.entries(attributes);
    for (let [key, value] of tmp) {
      topicConfigData.push({
        key: key,
        value: JSON.stringify(value),
        overridden: overrideConfigs && overrideConfigs.includes(key) ? "true" : "false"
      });
    }
  }
  return topicConfigData;
}

function getTopicConfigColumns() {
  return ([
    { title: "Config Name", field: "key" },
    { title: "Value", field: "value" },
    { title: "Overridden", field: "overridden" }
  ]);
}

function nodeToLink(arry, clusterId) {
  return arry.map((node, i) => (
    <Link component={RouterLink} key={i} to={"/clusters/" + clusterId + "/nodes/" + node}>
      {node}
      {i != arry.length - 1 ? "," : ""}
    </Link>
  ));
}

function bytesToGB(bytes) {
  return bytes / 1073741824;
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
