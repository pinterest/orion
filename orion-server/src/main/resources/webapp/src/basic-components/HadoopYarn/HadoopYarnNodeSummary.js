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
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  ExpansionPanelActions,
  Divider
} from "@material-ui/core";
import { makeStyles } from "@material-ui/core/styles";

export default function HadoopYarnNodeSummary(props) {
  let node = props.node;
  return (
    <Grid item container xs={1} alignContent="center">
      <Grid>
        <Typography>{node.currentNodeInfo.nodeId}</Typography>
      </Grid>
    </Grid>
  );
}
