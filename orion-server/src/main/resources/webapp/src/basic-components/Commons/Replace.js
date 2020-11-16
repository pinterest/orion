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
import { Button, FormControl, Grid, InputLabel, MenuItem, Select } from "@material-ui/core";
import { makeStyles } from "@material-ui/core/styles";
import React from "react";

const useStyles = makeStyles(theme => ({
  formControl: {
    margin: theme.spacing(1),
    minWidth: 120
  },
  selectEmpty: {
    marginTop: theme.spacing(2)
  }
}));

export default function Replace(props) {
  const classes = useStyles();
  const [instanceType, setInstanceType] = React.useState();
  const handleInstanceTypeChange = event => {
    setInstanceType(event.target.value);
  };
  return (
    <div>
      <Grid>
        <h3>Replace Cluster Nodes</h3>
        <Grid
          item
          xs={12}
          style={{ paddingLeft: "20px", paddingRight: "20px" }}
        >
          <FormControl className={classes.formControl}>
            <InputLabel id="lblInstanceType">Instance Type</InputLabel>
            <Select
              labelId="lblReplicationFactor"
              id="selectReplicationFactor"
              value={instanceType}
              onChange={handleInstanceTypeChange}
              style={{ width: "200px" }}
            >
              <MenuItem value={1}>1</MenuItem>
            </Select>
          </FormControl>
        </Grid>
        <Grid item xs={1} style={{ paddingLeft: "20px", paddingRight: "20px" }}>
          <FormControl className={classes.formControl}>
            <Button>Replace</Button>
          </FormControl>
        </Grid>
      </Grid>
    </div>
  );
}
