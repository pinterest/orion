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
  CircularProgress,
  Typography,
  ExpansionPanelDetails,
  Box,
  ExpansionPanelSummary,
  ExpansionPanel,
  Button,
  Chip,
  Grid,
  TableCell,
  TableRow,
  TableHead,
  Table,
  TableContainer,
  TableBody,
  Card,
  CardContent,
  ExpansionPanelActions,
  Divider
} from "@material-ui/core";
import ExpandMoreIcon from "@material-ui/icons/ExpandMore";
import CheckCircleIcon from '@material-ui/icons/CheckCircle';
import CancelIcon from '@material-ui/icons/Cancel';
import ReactJson from "react-json-view";

export default function ActionPanel(props) {
  const getColor = action =>
    action.status !== "FAILED" ? "primary" : "secondary";
  const getVariant = action => "determinate";
  const getValue = action => {
    action.status = "RUNNING";
    if (action.status === "RUNNING") {
      if (action.children.length > 0) {
        let val =
          100 *
          action.children
            .map(c => (c.done ? 1 : 0))
            .reduce((l1, l2) => l1 + l2, 0);
        val = val / action.children.length;
        return val;
      } else {
        return action.done ? 100 : -1;
      }
    } else if (action.done) {
      return 100;
    } else {
      return -1;
    }
  };
  const getFormattedOutput = value => value || "None";

  const renderAttributes = attributes => {
    return (
      <ReactJson
        src={attributes}
        name={"attributes"}
        shouldCollapse={f => f.name !== "attributes"}
        enableClipboard={false}
        displayDataTypes={false}
      />
    );
  };

  const getActionStatusIcon = (action) => {
    let value = getValue(action);
    if (action.result.err) {
      return (
        <CancelIcon style={{ fill: "red" }} />
      );
    } else if (value === 100) {
      return (
        <CheckCircleIcon style={{ fill: "green" }}/>
      );
    } else {
      return (
        <CircularProgress
          size={30}
          variant={getVariant(action)}
          color={getColor(action)}
          value={value}
        />
      );
    }
  }

  const renderChildren = (action, level) => {
    if (level === 0) {
      // prevent too many children getting displayed
      return null;
    }
    let childrenArray = action.children;
    if (Array.isArray(childrenArray) && childrenArray.length > 0) {
      let sortedChildrenArray = childrenArray.sort((a1, a2) => a2.createTime - a1.createTime);
      let children = sortedChildrenArray.map(action => (
        <ActionPanel
          key={action.uuid}
          action={action}
          level={level - 1}
          clusterId={props.clusterId}
        ></ActionPanel>
      ));
      return (
        <Grid item xs={12}>
          Child Actions:
          {children}
        </Grid>
      );
    }
    return null;
  };
  let action = props.action;
  let clusterId = props.clusterId;
  return (
    <ExpansionPanel TransitionProps={{ unmountOnExit: true }}>
      <ExpansionPanelSummary>
        <Grid container spacing={1}>
          <Grid item container xs={1} alignContent="center">
            <Grid>
              {getActionStatusIcon(action)}
            </Grid>
          </Grid>
          <Grid item container xs alignContent="center">
            <Grid item>
              <Typography>{action.name}</Typography>
            </Grid>
          </Grid>
          <Grid
            item
            container
            xs={3}
            alignContent="center"
            justify="space-evenly"
          >
            <Box m={0.3}>
              <Grid item>
                <Chip label={new Date(action.createTime).toLocaleString()} />
              </Grid>
            </Box>
            <Box m={0.3}>
              <Grid item>
                <Chip label={clusterId} />
              </Grid>
            </Box>
          </Grid>
        </Grid>
      </ExpansionPanelSummary>
      <ExpansionPanelDetails>
        <Grid container spacing={2}>
          <Grid item container xs={8}>
            <Typography variant="caption" color="textSecondary">
              {"Action ID: " + action.uuid}
            </Typography>
            <Grid item xs={12}>
              <Box m={1}>
                <Card variant="outlined" style={{ "overflowX": "scroll" }}>
                  <CardContent>
                    <Typography variant="h5">Output</Typography>
                    <Typography>
                      {getFormattedOutput(action.result.out)}
                    </Typography>
                  </CardContent>
                </Card>
              </Box>
            </Grid>
            <Grid item xs={12}>
              <Box m={1}>
                <Card variant="outlined" style={{ "overflowX": "scroll" }}>
                  <CardContent>
                    <Typography variant="h5">Error</Typography>
                    <Typography>
                      {getFormattedOutput(action.result.err)}
                    </Typography>
                  </CardContent>
                </Card>
              </Box>
            </Grid>
          </Grid>
          <Grid item xs={4}>
            <Box m={1}>
              <Card variant="outlined" style={{ "overflowX": "scroll" }}>
                <CardContent>
                  <Typography variant="h5">Attributes</Typography>
                  {renderAttributes(action.attributes)}
                </CardContent>
              </Card>
            </Box>
          </Grid>
          <Grid item xs={12}>
            {renderChildren(action, props.level)}
          </Grid>
        </Grid>
      </ExpansionPanelDetails>
      <Divider />
      <ExpansionPanelActions>
        <Button variant="contained" color="primary" disabled={action.done}>
          Cancel
        </Button>
      </ExpansionPanelActions>
    </ExpansionPanel>
  );
}

ActionPanel.defaultProps = {
  level: 3
};
