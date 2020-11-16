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
import React, { Suspense, lazy, useState } from "react";
import {
  Box,
  Grid,
  TextField,
  Button,
  Card,
  Select,
  MenuItem,
  InputLabel,
  FormControl,
  FormControlLabel,
  Checkbox
} from "@material-ui/core";
import { makeStyles } from "@material-ui/core/styles";

const useStyles = makeStyles(theme => ({
  formControl: {
    margin: theme.spacing(1),
    minWidth: 120
  },
  selectEmpty: {
    marginTop: theme.spacing(2)
  }
}));

export default function CreateTopicPanel(props) {
  const classes = useStyles();
  const [replicationFactor, setReplicationFactor] = React.useState(3);
  const handleReplicationFactorChange = event => {
    setReplicationFactor(event.target.value);
  };
  const [brokerSet, setBrokerSet] = React.useState();
  const handleBrokerSetChange = event => {
    setBrokerSet(event.target.value);
  };

  return (
    <Card
      style={{
        width: "400px",
        height: "400px",
        padding: "20px"
      }}
    >
      <Grid>
        <h3>Create Topic View</h3>
        <Grid
          item
          xs={12}
          style={{ paddingLeft: "20px", paddingRight: "20px" }}
        >
          <FormControl className={classes.formControl}>
            <TextField id="standard-basic" label="Topic Name" />
          </FormControl>
          <FormControl className={classes.formControl}>
            <InputLabel id="lblReplicationFactor">
              Replication Factor
            </InputLabel>
            <Select
              labelId="lblReplicationFactor"
              id="selectReplicationFactor"
              value={replicationFactor}
              onChange={handleReplicationFactorChange}
            >
              <MenuItem value={1}>1</MenuItem>
              <MenuItem value={2}>2</MenuItem>
              <MenuItem value={3}>3</MenuItem>
            </Select>
          </FormControl>
          <FormControl className={classes.formControl}>
            <TextField id="standard-basic" label="Throughput" />
          </FormControl>
          <FormControl className={classes.formControl}>
            <TextField id="standard-basic" label="Retention (Hours)" />
          </FormControl>
          <FormControl className={classes.formControl}>
            <TextField id="standard-basic" label="Partitions" />
          </FormControl>
          <FormControl className={classes.formControl}>
            <InputLabel id="lblBrokerSet">Brokerset</InputLabel>
            <Select
              labelId="lblBrokerSet"
              id="selectBrokerSet"
              value={brokerSet}
              onChange={handleBrokerSetChange}
            ></Select>
          </FormControl>
        </Grid>
        <Grid item xs={1} style={{ paddingLeft: "20px", paddingRight: "20px" }}>
          <FormControl className={classes.formControl}>
            <Button>Create</Button>
          </FormControl>
        </Grid>
      </Grid>
    </Card>
  );
}
