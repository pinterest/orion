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
import React, { useEffect } from "react";
import { Link as RouterLink, Redirect, Route, Switch } from "react-router-dom";
import {
  Tab,
  Tabs,
  Grid,
  Typography,
  Card,
  CardContent,
  Box,
} from "@material-ui/core";
import Summary from "./Summary";
import Utilization from "./Utilization";
import Ami from "./Ami";

const routes = [
  {
    subpath: "summary",
    component: Summary,
    label: "Summary",
  },
  {
    subpath: "utilization",
    component: Utilization,
    label: "Utilization",
  },
  {
    subpath: "ami",
    component: Ami,
    label: "AMI",
  },
];

export default function Homepage(props) {
  let clusters = [];
  let globalSensors = [];
  if (props.clusters) {
    clusters = props.clusters;
  }
  if (props.globalSensors) {
    globalSensors = props.globalSensors;
  }
  let clusterCount = clusters.length;
  let nodeCount = clusters.map((c) => c.nodes).reduce((l1, l2) => l1 + l2, 0);
  let alertCount = clusters
    .map((c) => c.alerts.length)
    .reduce((l1, l2) => l1 + l2, 0);
  let actionCount = clusters
    .map((c) => c.actions.length)
    .reduce((l1, l2) => l1 + l2, 0);
  let agentCount = clusters.map((c) => c.agents).reduce((l1, l2) => l1 + l2, 0);

  return (
    <div>
      <Grid container spacing={2} style={{ paddingTop: "20px" }}>
        <Grid item xs={12} sm={3}>
          <SummaryCard value={clusterCount} name="clusters" />
        </Grid>
        <Grid item xs={12} sm={3}>
          <SummaryCard
            value={agentCount + "/" + nodeCount}
            name="agents/nodes"
          />
        </Grid>
        <Grid item xs={12} sm={3}>
          <SummaryCard value={alertCount} name="alerts" />
        </Grid>
        <Grid item xs={12} sm={3}>
          <SummaryCard value={actionCount} name="actions" />
        </Grid>
      </Grid>
      <Grid style={{ paddingTop: "20px" }}>
        <Grid item xs={12}>
          <Switch>
            <Redirect exact from="/" to="/homepage/summary"></Redirect>
            <Route path="/homepage/:tab" children={NavTabs} />
          </Switch>
        </Grid>
        <Grid item xs={12}>
          <Switch>
            {routes.map((route, idx) => {
              return (
                <Route key={idx} path={"/homepage/" + route.subpath}>
                  <route.component
                    clusters={clusters}
                    globalSensors={globalSensors}
                  />
                </Route>
              );
            })}
          </Switch>
        </Grid>
      </Grid>
    </div>
  );
}

function SummaryCard(props) {
  return (
    <Card>
      <CardContent>
        <Box display="flex" alignItems="center">
          <Box mr={1}>
            <Typography variant="h6">{props.value}</Typography>
          </Box>
          <Typography variant="subtitle1">{props.name}</Typography>
        </Box>
      </CardContent>
    </Card>
  );
}

function NavTabs(props) {
  return (
    <Tabs value={props.match.params.tab}>
      {routes.map((route, idx) => (
        <Tab
          key={idx}
          value={route.subpath}
          label={route.label}
          to={"/homepage/" + route.subpath}
          component={RouterLink}
        />
      ))}
    </Tabs>
  );
}
