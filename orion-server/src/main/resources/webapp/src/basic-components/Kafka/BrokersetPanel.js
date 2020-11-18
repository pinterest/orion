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
import React, { forwardRef } from "react";
import {
  Paper,
  Grid,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  ExpansionPanelDetails,
  ExpansionPanelSummary,
  ExpansionPanel,
  Divider,
  Link,
  Box
} from "@material-ui/core";
import ExpandMoreIcon from "@material-ui/icons/ExpandMore";
import { Link as RouterLink, Redirect, Route, Switch } from "react-router-dom";
import { withStyles, makeStyles } from "@material-ui/core/styles";

import MaterialTable from "material-table";

export default function Brokerset(props) {
  let brokerset = props.brokerset;
  let rows = [];
  if (brokerset) {
    rows = brokerset.entries;
  }

  let data = [];
  rows.map(entry => {
    console.log(entry);
    data.push({
      startBrokerIdx: entry.startBrokerIdx,
      endBrokerIdx: entry.endBrokerIdx,
      size: entry.size
    });
  });

  return (
    <MaterialTable
      options={{ pageSizeOptions: [5] }}
      title=""
      columns={[
        { title: "Start Broker Id", field: "startBrokerIdx" },
        { title: "End Broker Id", field: "endBrokerIdx" },
        { title: "Size", field: "size" }
      ]}
      data={data}
    />
  );
}
