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
import { Tab, Tabs, Grid } from "@material-ui/core";
import { Link as RouterLink, Redirect, Route, Switch } from "react-router-dom";
import { makeStyles } from "@material-ui/core/styles";
import Topics from "./Topics";
import Brokersets from "../Kafka/Brokersets";
import ConsumerGroups from "../Kafka/ConsumerGroups";

const useStyles = makeStyles({
  table: {
    minWidth: 650,
  },
});

const routes = [
  {
    subpath: "topics",
    component: Topics,
    label: "Topics",
  },
  {
    subpath: "brokersets",
    component: Brokersets,
    label: "Brokersets",
  },
  // {
  //   subpath: "consumergroups",
  //   component: ConsumerGroups,
  //   label: "Consumer Groups"
  // }
];

export default function MemqService(props) {
  let cluster = props.cluster;
  return (
    <Grid>
      <Grid item xs={12}>
        <Switch>
          <Redirect
            exact
            from="/clusters/:clusterId/service"
            to="/clusters/:clusterId/service/topics"
          ></Redirect>
          <Route path="/clusters/:clusterId/service/:tab" children={NavTabs} />
        </Switch>
      </Grid>
      <Grid item xs={12}>
        <Switch>
          {routes.map((route, idx) => {
            return (
              <Route
                key={idx}
                path={"/clusters/:clusterId/service/" + route.subpath}
              >
                <route.component cluster={cluster} />
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
          to={route.subpath}
          component={RouterLink}
        />
      ))}
    </Tabs>
  );
}
