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
import React, { useState } from "react";
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  Tooltip,
  Popover,
  makeStyles,
} from "@material-ui/core";
import CheckCircleIcon from "@material-ui/icons/CheckCircle";
import ErrorIcon from "@material-ui/icons/Error";
import WarningIcon from "@material-ui/icons/Warning";
import MaintenanceButton from "./MaintenanceButton";
import { SensorCard } from "./SensorCard";

function Automation(props) {
  let cluster = props.cluster;
  let sensors = cluster.automationEngine.sensors;
  let operators = cluster.automationEngine.operators;
  let maintenance = cluster.underMaintenance;

  let sensorViews = sensors.map((mCntr) => {
    let m = mCntr.sensor;
    return (
      <Grid item xs={6} key={cluster.clusterId + "." + m.name}>
        <SensorCard m={mCntr} />
      </Grid>
    );
  });

  let operatorViews = operators.map((oCntr) => {
    return (
      <Grid item key={cluster.clusterId + "." + oCntr.operator.name} xs={6}>
        <OperatorCard o={oCntr} maintenance={maintenance} />
      </Grid>
    );
  });

  return (
    <Box>
      <Grid container spacing={3}>
        <Grid item container xs={12} justify="flex-end">
          <MaintenanceButton />
        </Grid>
        <Grid item xs={6}>
          <Typography variant="h5">Sensors</Typography>
          <Grid container spacing={1}>
            {sensorViews}
          </Grid>
        </Grid>
        <Grid item xs={6}>
          <Typography variant="h5">
            Operators{" "}
            {maintenance && (
              <Tooltip title="Operators are paused since cluster is under maintenance">
                <WarningIcon style={{ color: "orange" }} />
              </Tooltip>
            )}
          </Typography>
          <Grid container spacing={1}>
            {operatorViews}
          </Grid>
        </Grid>
      </Grid>
    </Box>
  );
}

const useStyles = makeStyles((theme) => ({
  popover: {
    pointerEvents: "none",
  },
  paper: {
    padding: theme.spacing(1),
  },
  popoverTypography: {
    whiteSpace: "pre-wrap",
  },
}));

function OperatorCard(props) {
  const classes = useStyles();
  const [anchorEl, setAnchorEl] = useState(null);

  const handlePopoverOpen = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handlePopoverClose = () => {
    setAnchorEl(null);
  };

  return (
    <Card>
      <CardContent
        onMouseEnter={handlePopoverOpen}
        onMouseLeave={handlePopoverClose}
      >
        <Typography>{props.o.operator.name}</Typography>
        <Box>
          <Tooltip
            title={
              props.maintenance
                ? "Under maintenance"
                : props.o.previousSuccess
                ? ""
                : "Failing"
            }
          >
            {props.maintenance ? (
              <WarningIcon style={{ color: "orange" }} />
            ) : props.o.previousSuccess ? (
              <CheckCircleIcon style={{ color: "green" }} />
            ) : (
              <ErrorIcon style={{ color: "red" }} />
            )}
          </Tooltip>
        </Box>
      </CardContent>
      <Popover
        className={classes.popover}
        classes={{
          paper: classes.paper,
        }}
        open={Boolean(anchorEl) && props.o.previousOutput}
        anchorEl={anchorEl}
        anchorOrigin={{
          vertical: "bottom",
          horizontal: "left",
        }}
        transformOrigin={{
          vertical: "top",
          horizontal: "left",
        }}
        onClose={handlePopoverClose}
        disableRestoreFocus
      >
        <Typography className={classes.popoverTypography}>
          {props.o.previousOutput}
        </Typography>
      </Popover>
    </Card>
  );
}

export default Automation;
