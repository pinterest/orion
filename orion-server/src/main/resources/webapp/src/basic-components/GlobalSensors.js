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
  Grid,
  Typography,
  Box,
  Container,
  CardContent,
  Tooltip,
  Card,
} from "@material-ui/core";
import CheckCircleIcon from "@material-ui/icons/CheckCircle";
import ErrorIcon from "@material-ui/icons/Error";

export default function GlobalSensors(props) {
  let globalSensors = [];
  if (props.globalSensors) {
    globalSensors = props.globalSensors;
  }
  let sensorViews = globalSensors.map((s) => {
    return (
      <Grid item xs={6} key={s.name}>
        <GlobalSensorCard info={s} />
      </Grid>
    );
  });
  return (
    <div>
      <Grid style={{ paddingTop: "30px" }}>
        <Grid container spacing={2}>
          <Typography variant="h5">Global Sensors</Typography>
        </Grid>
      </Grid>
      <Box display="flex" alignItems="flex-start" mt={4}>
        <Container style={{ paddingRight: 0 }}>{sensorViews}</Container>
      </Box>
    </div>
  );
}

function GlobalSensorCard(props) {
  return (
    <Card>
      <CardContent>
        <Box display="flex" alignItems="center" justifyContent="space-between">
          <Typography color="textPrimary">{props.info.name}</Typography>
        </Box>
        <Box>
          <Tooltip
            title={
              props.info.attributes
                ? props.info.attributes._configs
                  ? JSON.stringify(props.info.attributes._configs)
                  : "N/A"
                : "N/A"
            }
            aria-label="add"
          >
            <CheckCircleIcon style={{ color: "green" }} />
          </Tooltip>
        </Box>
        <Typography>Every {props.info.interval} seconds</Typography>
      </CardContent>
    </Card>
  );
}
