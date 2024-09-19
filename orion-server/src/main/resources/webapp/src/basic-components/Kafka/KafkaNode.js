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
  {
    subpath: "brokersets",
    component: PropsTable,
    label: "Brokersets",
    getData: getBrokersetData,
    getColumns: getBrokersetColumns,
  },
  {
    subpath: "brokerstats",
    component: PropsTable,
    label: "Broker Stats",
    getData: getBrokerStatsData,
    getColumns: getBrokerStatsColumns,
  }
];

export default function KafkaNode({node, clusterId, cluster}) {
  return (
    <div>
      {getNodeInfoHeader(
        node,
        node.currentNodeInfo.nodeId,
        node.currentNodeInfo.serviceInfo["broker.id"],
        node.currentNodeInfo.hostname.split(".",1)[0]
      )}
      <Grid>
        <Grid item xs={10}>
          <Switch>
            <Redirect
              exact
              from="/clusters/:clusterId/nodes/:nodeId"
              to="/clusters/:clusterId/nodes/:nodeId/topicpartitions"
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
                {<route.component
                  data={route.getData(cluster, node)}
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
            <Typography variant="h6">
              {hostname}
            </Typography>
          </Grid>
          <Grid item>
            <Chip variant="outlined" label="Kafka Node" size="small"/>
          </Grid>
          <Grid item>
            <Chip label={"Broker ID: " + brokerId} size="small" />
          </Grid>
          <Grid item>
            <Chip
              variant="outlined"
              color="primary"
              size="small"
              label={node.topicPartitionsForNode.length + " topics"}
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
          <Grid item>
            <Chip
                variant="outlined"
                color="primary"
                size="small"
                label={node.currentNodeInfo.brokersets.length + " brokersets"}
            />
          </Grid>
        </Grid>
      </Box>
    </div>
  );
}

function getTopicPartitionsData(cluster, node) {
  let rows = [];
  let clusterId = node.currentNodeInfo.clusterId;
  const topicinfo = cluster.attributes.topicinfo;
  if (node) {
    rows = node.topicPartitionsForNode;
  }
  let data = [];
  rows.map(row =>
    row.partitions.map(partition => {
      const partitionInfo = topicinfo[row.topic].partitions[partition];
      data.push({
        topic: (
          <Link component={RouterLink} to={"/clusters/" + clusterId
            + "/service/topics/" + row.topic + "/topicpartitions"}>
            {row.topic}
          </Link>
        ),
        partition: partition,
        isrs: <Box>{nodeToLink(partitionInfo.isrs, clusterId)}</Box>,
        replicas: <Box>{nodeToLink(partitionInfo.replicas, clusterId)}</Box>,
        isLeader:
          JSON.stringify(partitionInfo.leader) === node.currentNodeInfo.nodeId
            ? "True"
            : "False",
        isPreferredLeader:
        partitionInfo.leader === partitionInfo.replicas[0] ? "True" : "False",
        size: partitionInfo.replicainfo[node.currentNodeInfo.nodeId]
          ? bytesToGB(
            partitionInfo.replicainfo[node.currentNodeInfo.nodeId].size
            ).toFixed(2)
          : -1
      });
    })
  );
  return data;
}

function bytesToGB(bytes) {
  return bytes / 1073741824;
}

function nodeToLink(arry, clusterId) {
  return arry.map((node, i) => (
    <Link component={RouterLink} key={i} to={"/clusters/" + clusterId + "/nodes/" + node}>
      {node}
      {i != arry.length - 1 ? "," : ""}
    </Link>
  ));
}

function getTopicPartitionsColumns() {
  return ([
    { title: "Topic", field: "topic" },
    { title: "Partition", field: "partition", type: "numeric" },
    { title: "ISRs", field: "isrs" },
    { title: "Replicas", field: "replicas" },
    { title: "Is Leader?", field: "isLeader" },
    { title: "Preferred Leader?", field: "isPreferredLeader" },
    { title: "Size (GB)", field: "size", type: "numeric" }
  ]);
}

function brokersetToLink(brokerset, clusterId) {
  return (
      <Link
          component={RouterLink}
          to={"/clusters/" + clusterId + "/service/brokersets/" + brokerset + "/status"}
      >
        {brokerset}
      </Link>
  );
}

function getBrokersetData(cluster, node) {
  let clusterId = node.currentNodeInfo.clusterId;
  let brokersets = node.currentNodeInfo.brokersets;
  let brokersetRows = [];
  for (let brokersetAlias of brokersets) {
    const brokersetAliasSplit = brokersetAlias.split("_");
    let brokersetType = brokersetAliasSplit[0];
    let brokerCount = -1;
    let partitionCount = -1;
    if (brokersetType === "Capacity" || brokersetType === "Static") {
      brokerCount = parseInt(brokersetAliasSplit[1].replace("B", ""));
      partitionCount = parseInt(brokersetAliasSplit[2].replace("P", ""));
    }
    brokersetRows.push({
      brokersetAlias: <Box>{brokersetToLink(brokersetAlias, clusterId)}</Box>,
      type: brokersetType,
      brokerCount: brokerCount,
      partitionCount: partitionCount
    });
  }
  return brokersetRows;
}

function getBrokersetColumns() {
    return ([
      { title: "Brokerset Name", field: "brokersetAlias" },
      { title: "Type", field: "type" },
      { title: "Broker Count", field: "brokerCount" },
      { title: "Partition Count", field: "partitionCount" }
    ]);
}

function getBrokerStatsData(cluster, node) {
  let brokerStatsData = [];
  return brokerStatsData;
}

function getBrokerStatsColumns() {
  return ([
      { title: "Key", field: "key" },
      { title: "Value", field: "value" }
  ]);
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
  let environmentData = environmentConfigRows.map(entry => {
    return {
      key: entry.key,
      value: entry.value
    };
  });
  return environmentData;
}

function getBrokerEnvironmentColumns() {
  return ([
    { title: "Environment Variable", field: "key" },
    { title: "Value", field: "value" }
  ]);
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
  let serviceConfigsData = serviceConfigRows.map(entry => {
    return {
      key: entry.key,
      value: entry.value
    };
  });
  return serviceConfigsData;
}

function getServiceConfigsColumns() {
  return ([
    { title: "Config Name", field: "key" },
    { title: "Value", field: "value" }
  ]);
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
