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
  Box,
  ExpansionPanelSummary,
  ExpansionPanelDetails,
  ExpansionPanel,
  Grid,
  Chip,
  Typography,
  ExpansionPanelActions,
  Divider,
  Button,
  Link
} from "@material-ui/core";
import AlertPanel from "./AlertPanel";

export default function Alerts(props) {
  let alertsList = props.cluster.actionEngine.alertsList;
  let sortedAlertsList = alertsList.sort((a1, a2) => a2.timestamp - a1.timestamp);
  let clusterId = props.cluster.clusterId;
  let alertsToDisplay = sortedAlertsList;
  let moreAlertsLink = null;

  if (alertsToDisplay.length > 10) {
    alertsToDisplay = alertsToDisplay.slice(0,10);
    let numMoreAlerts = sortedAlertsList.length - alertsToDisplay.length;
    moreAlertsLink = (
      <Box my={2}>
        <Link variant="body1">
          {"... and " + numMoreAlerts.toString() + " more alerts"}
        </Link>
      </Box>
    );
  }

  let alertPanels = alertsToDisplay.map(a => {
    let readAlert = () => {
        fetch("/api/clusters/" + props.cluster.clusterId + "/alerts/" + a.uuid + "/read",{
            method: "PUT"
        })
        .then(resp => console.log(resp))
    }
    return <AlertPanel alert={a} readAlert={readAlert} clusterId={clusterId}></AlertPanel>
  });
  return (
    <Box>
      <Box>
        {alertPanels}
      </Box>
      {moreAlertsLink}
    </Box>
  );
}
