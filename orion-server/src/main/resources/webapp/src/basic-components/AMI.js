/*******************************************************************************
 * Copyright 2024 Pinterest, Inc.
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
import { Button, FormControl, Grid, InputLabel, MenuItem, Select, TextField,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  FormGroup, FormControlLabel, Checkbox } from '@material-ui/core';
import { makeStyles } from "@material-ui/core/styles";
import { connect } from "react-redux";
import { requestAmiList, updateAmiTag } from "../actions/cluster";

const mapState = (state, ownProps) => {
  const { amiList } = state.app;
  return {
    ...ownProps,
    amiList,
  };
};

const mapDispatch = {
  requestAmiList,
  updateAmiTag
};

const useStyles = makeStyles(theme => ({
  formControl: {
    margin: theme.spacing(1),
    minWidth: 120
  },
}));

function Ami({ amiList, requestAmiList, updateAmiTag }) {
  const classes = useStyles();
  const [os, setOS] = React.useState();
  const handleOSChange = event => {
    setOS(event.target.value);
  };
  const [cpuArch, setCPUArch] = React.useState();
  const handleCPUArchChange = event => {
    setCPUArch(event.target.value);
  };
  const [selected, setSelected] = React.useState([]);
  const handleTableRowSelect = (id, row) => {
    setSelected(id);
    setAppEnv(row.applicationEnvironment);
    const envs_str = row.applicationEnvironment;
    const envs = envs_str.split(',');
    env.dev = env.test = env.staging = env.prod = false;
    for (const env_str of envs)
      env[env_str] = true;
  };
  const [appEnv, setAppEnv] = React.useState();
  const handleAppEnvChange = event => {
    setAppEnv(event.target.value);
  };
  const [env] = React.useState({
    dev: false,
    test: false,
    staging: false,
    prod: false,
  });
  const handleCheckboxChange = (event) => {
    env[event.target.name] = event.target.checked;
    const newAppEnv = [];
    if (env.dev)
      newAppEnv.push("dev");
    if (env.test)
      newAppEnv.push("test");
    if (env.staging)
      newAppEnv.push("staging");
    if (env.prod)
      newAppEnv.push("prod");
    setAppEnv(newAppEnv.join(','));
  };
  const applyFilter = () => {
    const parms = [];
    if (os)
      parms.push("release=" + os);
    if (cpuArch)
      parms.push("cpu_architecture=" + cpuArch);
    requestAmiList(parms.join('&'));
  }

  if (!amiList)
    amiList = [];
  return (
    <div>
      <Grid container spacing={3}>
        <Grid item xs={3}>
          <center><h3>Tag filters</h3>
          <div>
            <FormControl className={classes.formControl}>
              <TextField
                id="application"
                label="application"
                defaultValue="kafka"
                InputProps={{
                  readOnly: true,
                }}
                style={{ width: "200px" }}
              />
            </FormControl>
          </div>
          <div>
            <FormControl className={classes.formControl}>
              <InputLabel id="lblOS">OS</InputLabel>
              <Select
                labelId="lblSelectOS"
                id="selectOS"
                value={os}
                onChange={handleOSChange}
                style={{ width: "200px", textAlign: "left" }}
              >
                <MenuItem value={"bionic"}>bionic</MenuItem>
                <MenuItem value={"focal"}>focal</MenuItem>
              </Select>
            </FormControl>
          </div>
          <div>
            <FormControl className={classes.formControl}>
              <InputLabel id="lblCPUArch">CPU Architecture</InputLabel>
              <Select
                labelId="lblSelectCPUArch"
                id="selectCPUArch"
                value={cpuArch}
                onChange={handleCPUArchChange}
                style={{ width: "200px", textAlign: "left" }}
              >
                <MenuItem value={"amd64"}>amd64</MenuItem>
                <MenuItem value={"arm64"}>arm64</MenuItem>
              </Select>
            </FormControl>
          </div>
          <div>
            <FormControl className={classes.formControl}>
              <TextField
                id="application_environment"
                label="application_environment"
                defaultValue="*"
                InputProps={{
                  readOnly: true,
                }}
                style={{ width: "200px" }}
              />
            </FormControl>
          </div>
          <div>
            <FormControl className={classes.formControl}>
              <Button
                variant="contained"
                onClick={applyFilter}
              >Apply</Button>
            </FormControl>
          </div></center>
        </Grid>
        <Grid item xs={6}>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>AMI Id</TableCell>
                  <TableCell>application_environment</TableCell>
                  <TableCell>Creation Date</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                { amiList.map((row) => (
                  <TableRow
                    key={row.amiId}
                    sx={{ '&:last-child td, &:last-child th': { border: 0 } }}
                    onClick={() => handleTableRowSelect(row.amiId, row)}
                    selected={selected === row.amiId}
                  >
                    <TableCell component="th" scope="row">{row.amiId}</TableCell>
                    <TableCell align="left">{row.applicationEnvironment}</TableCell>
                    <TableCell>{row.creationDate}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Grid>
        <Grid item xs={3}>
          <center><div>
            <TextField
              id="selectedAppEnv"
              label="application_environment"
              value={appEnv || ''}
              onChange={handleAppEnvChange}
              InputProps={{
                readOnly: true,
              }}
              style={{ width: "200px" }}
            />
          </div>
          <div>
            <FormGroup column>
              <FormControlLabel
                control={
                  <Checkbox
                    checked={env.dev}
                    onChange={handleCheckboxChange}
                    name="dev"
                    color="primary"
                  />}
                label="dev"
              />
              <FormControlLabel
                control={
                  <Checkbox
                    checked={env.test}
                    onChange={handleCheckboxChange}
                    name="test"
                    color="primary"
                  />}
                label="test"
              />
              <FormControlLabel
                control={
                  <Checkbox
                    checked={env.staging}
                    onChange={handleCheckboxChange}
                    name="staging"
                    color="primary"
                  />}
                label="staging"
              />
              <FormControlLabel
                control={
                  <Checkbox
                    checked={env.prod}
                    onChange={handleCheckboxChange}
                    name="prod"
                    color="primary"
                  />}
                label="prod"
              />
            </FormGroup>
          </div>
          <div>
            <FormControl className={classes.formControl}>
              <Button
                variant="contained"
                onClick={() => {
                  updateAmiTag(selected, appEnv);
                  applyFilter();
                }}
              >Update</Button>
            </FormControl>
          </div></center>
        </Grid>
      </Grid>
    </div>
  );
}

export default connect(mapState, mapDispatch)(Ami);
