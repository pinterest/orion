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
  Container,
  Typography,
  Button,
  Chip,
  Paper,
  Grid,
  Divider,
  Box
} from "@material-ui/core";
import Chart from "react-apexcharts";
import { Link as RouterLink, Redirect, Route, Switch } from "react-router-dom";
import { makeStyles } from "@material-ui/core/styles";

const numb = 30;

const useStyles = makeStyles({
  minWidth: 50
});

export default function Dashboard(props) {
  let cluster = props.cluster;
  let series = getData(cluster);

  return (
    <Grid container spacing={1}>
      <Grid item container xs={12}></Grid>
      Nodes:
      {series.map(entry => (
        <Grid item container key={entry.name} xs={12}>
          {entry.data.map(node => (
            <div
              key={node.x}
              style={{
                minWidth: 100,
                backgroundColor: "#81c784",
                alignContent: "center",
                textAlign: "center",
                border: "1px solid black"
              }}
            >
              <Typography> {node.x}</Typography>
            </div>
          ))}
        </Grid>
      ))}
    </Grid>
  );
}

function getData(cluster) {
  let nodes = Object.values(cluster.nodeMap);
  let series = {};
  let racks = new Set(nodes.map(n => n.currentNodeInfo.rack));
  racks.forEach(r => {
    series[r] = {
      name: r,
      data: []
    };
  });
  nodes.map(n => {
    let info = n.currentNodeInfo;
    series[info.rack].data.push({
      x: info.nodeId,
      y: 10
    });
  });

  return Object.values(series);
}
