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
import { Grid, Typography, Box, Container, Link } from "@material-ui/core";
import AlertPanel from "./AlertPanel";

export default function GlobalActions(props) {
  const clusters = props.clusters;

  return (
    <div>
      <Grid style={{ paddingTop: "30px" }}>
        <Grid container spacing={2}>
          <Typography variant="h5">Global Alerts</Typography>
        </Grid>
      </Grid>
        <Box
          display="flex"
          alignItems="flex-start"
          mt={4}
        >
        <Container style={{ paddingRight: 0 }}>
            {getGlobalAlertPanels(getGlobalAlertsList(clusters))}
        </Container>
        </Box>
    </div>
  );
}

function getGlobalAlertPanels(globalAlertsList) {
  let alertsToDisplay = globalAlertsList;
  let moreAlertsLink = null;
  if (alertsToDisplay.length > 10) {
    alertsToDisplay = alertsToDisplay.slice(0,10);
    let numMoreAlerts = globalAlertsList.length - alertsToDisplay.length;
    moreAlertsLink = (
      <Box my={2}>
        <Link variant="body1">
          {"... and " + numMoreAlerts.toString() + " more alerts"}
        </Link>
      </Box>
    );
  }
  let alertPanelsToDisplay = alertsToDisplay.map(alert => {
    let readAlert = () => {
        fetch("/api/clusters/" + alert.clusterId + "/alerts/"
          + alert.alertObject.uuid + "/read",{
            method: "PUT"
        })
        .then(resp => console.log(resp))
    }
    return (
      <Box my={0.2}>
        <AlertPanel
          clusterId={alert.clusterId}
          alert={alert.alertObject}
          readAlert={readAlert}></AlertPanel>
      </Box>
    );
  });
  return (
    <Box>
      {alertPanelsToDisplay}
      {moreAlertsLink}
    </Box>
  );
}

function getGlobalAlertsList(clusters) {
  let globalAlertsList = [];
  clusters.forEach((cluster) => {
    let clusterAlerts = [];
    cluster.alerts.forEach((alert) => {
      clusterAlerts.push({
        clusterId: cluster.clusterId,
        alertObject: alert,
      });
    });
    Array.prototype.push.apply(globalAlertsList, clusterAlerts);
  });
  return globalAlertsList.sort(
    (a1, a2) => a2.alertObject.timestamp - a1.alertObject.timestamp);
}
