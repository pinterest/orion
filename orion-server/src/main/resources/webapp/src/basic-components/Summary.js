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
import { Grid, Tab, Tabs } from "@material-ui/core";
import Chart from "react-apexcharts";
import { Link as RouterLink, Redirect, Route, Switch } from "react-router-dom";
import { makeStyles } from "@material-ui/core/styles";
import AlertPanel from "./AlertPanel";
import GlobalActions from "./GlobalActions";
import GlobalAlerts from "./GlobalAlerts";

const routes = [
  {
    subpath: "global-actions",
    component: GlobalActions,
    label: "Global Actions"
  },
  {
    subpath: "global-alerts",
    component: GlobalAlerts,
    label: "Global Alerts"
  }
];

export default function Summary(props) {
  let clusters = [];

  if (props.clusters) {
    clusters = props.clusters;
  }

  return (
    <Grid style={{ paddingTop: "10px" }}>
      <Grid item xs={12}>
        <Switch>
          <Redirect
            exact
            from="/homepage/summary"
            to="/homepage/summary/global-actions"
          ></Redirect>
          <Route path="/homepage/summary/:tab" children={NavTabs} />
        </Switch>
      </Grid>
      <Grid item xs={12}>
        <Switch>
          {routes.map((route, idx) => {
            return (
              <Route
                key={idx}
                path={"/homepage/summary/" + route.subpath}
              >
                <route.component clusters={clusters} />
              </Route>
            );
          })}
        </Switch>
      </Grid>
    </Grid>
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
          to={"/homepage/summary/" + route.subpath}
          component={RouterLink}
        />
      ))}
    </Tabs>
  );
}
