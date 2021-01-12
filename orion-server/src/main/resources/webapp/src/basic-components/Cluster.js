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
  Box,
  Tab,
  Tabs,
  Chip,
  Grid,
  Typography,
  Icon
} from "@material-ui/core";
import React, { useEffect } from "react";
import { Link as RouterLink, Redirect, Route, Switch } from "react-router-dom";
import Actions from "./Actions";
import Automation from "./Automation";
import Nodes from "./Nodes";
import Alerts from "./Alerts";
import Dashboard from "./Dashboard";
import Service from "./Service";
import ClusterSummary from "./ClusterSummary";
import { connect } from "react-redux";
import { requestCluster } from "../actions/cluster";
import { makeStyles } from "@material-ui/core/styles";
import { BuildRounded } from "@material-ui/icons";

const routes = [
  {
    subpath: "dashboard",
    component: Dashboard,
    label: "Dashboard"
  },
  {
    subpath: "service",
    component: Service,
    label: "Service"
  },
  {
    subpath: "nodes",
    component: Nodes,
    label: "Nodes"
  },
  {
    subpath: "actions",
    component: Actions,
    label: "Actions"
  },
  {
    subpath: "automation",
    component: Automation,
    label: "Automation Status"
  },
  {
    subpath: "alerts",
    component: Alerts,
    label: "Alerts"
  }
];

const useStyles = makeStyles(theme => ({
  statusBar: {
    "& > *": {
      marginRight: theme.spacing(1)
    }
  }
}));

const mapState = (state, ownProps) => {
  const { clusterView } = state;
  return {
    ...ownProps,
    ...clusterView
  };
};

const mapDispatch = {
  requestCluster
};

function Cluster({
  clusterId,
  cluster,
  fetchError,
  isLoading,
  requestCluster
}) {
  useEffect(() => {
    requestCluster(clusterId);
  }, [clusterId, requestCluster]);

  let classes = useStyles();

  if (fetchError) {
    return <ErrorMessage clusterId={clusterId} error={fetchError} />;
  } else if (cluster.clusterId) {
    return (
      <div>
        <Grid container spacing={3}>
          <Grid item xs={12}>
            <Box mt={4} display="flex" alignItems="center">
              <Typography variant="h4">{cluster.name}</Typography>
              <Box mx={3} className={classes.statusBar}>
                <Chip variant="outlined" label={cluster.type} />
                {cluster.underMaintenance && (
                  <Chip
                    variant="outlined"
                    color="secondary"
                    icon={<Icon style={{fontSize: "18px"}}>build</Icon>}
                    label="Under maintenance"
                  />
                )}
              </Box>
              {getClusterHealthyText(cluster.healthy)}
              <br />
            </Box>
          </Grid>
          <Grid item xs="auto">
            <ClusterSummary cluster={cluster} />
          </Grid>
          <Grid item xs={12}>
            <Switch>
              <Redirect
                exact
                from="/clusters/:clusterId"
                to="/clusters/:clusterId/dashboard"
              ></Redirect>
              <Route path="/clusters/:clusterId/:tab" children={ClusterNavTabs} />
            </Switch>
          </Grid>
          <Grid item xs={12}>
            <Switch>
              {routes.map((route, idx) => {
                return (
                  <Route key={idx} path={"/clusters/:clusterId/" + route.subpath}>
                    <route.component cluster={cluster} />
                  </Route>
                );
              })}
            </Switch>
          </Grid>
        </Grid>
      </div>
    );
  } else {
    return <Box></Box>;
  }
}

function ClusterNavTabs(props) {
  return (
    <Tabs value={props.match.params.tab} variant="fullWidth">
      {routes.map((route, idx) => (
        <Tab
          key={idx}
          value={route.subpath}
          label={route.label}
          to={"/clusters/" + props.match.params.clusterId + "/" + route.subpath}
          component={RouterLink}
        />
      ))}
    </Tabs>
  );
}

function getClusterHealthyText(isHealthy) {
  if (isHealthy) {
    return (
      <Typography variant="subtitle2" style={{ color: "green" }}>
        Healthy
      </Typography>
    );
  } else {
    return (
      <Typography variant="subtitle2" style={{ color: "orange" }}>
        URPs Detected
      </Typography>
    );
  }
}

function ErrorMessage(props) {
  return (
    <Box>
      <Typography variant="h2">Error!</Typography>
      <Typography>
        Something wrong happened when fetching cluster <i>{props.clusterId}</i>
        ...
      </Typography>
      <Typography variant="caption">
        Error code: {props.error.code}
        <br />
        Error message: {props.error.message}
      </Typography>
    </Box>
  );
}

export default connect(mapState, mapDispatch)(Cluster);
