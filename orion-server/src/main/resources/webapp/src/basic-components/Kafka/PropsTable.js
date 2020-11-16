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
import MaterialTable from "material-table";

const useStyles = makeStyles({
  table: {
    minWidth: 650
  }
});

export default function PropsTable(props) {
  const classes = useStyles();
  let data = props.data;
  let columns = props.columns;
  removeQuotesFromData(data);

  function removeQuotesFromData(data) {
    data.forEach(obj => {
      let val = obj.value;
      if (typeof val === 'string') {
        obj.value = val.replace('"', '').replace('"', '');
      }
    });
  }

  return (
    <MaterialTable
      style={{ width: "100%" }}
      options={{ pageSize: 7 }}
      columns={columns}
      data={data}
      title=""
    />
  );
}
