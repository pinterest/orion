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
  Box,
  ExpansionPanelSummary,
  ExpansionPanelDetails,
  ExpansionPanel,
  Grid,
  Chip,
  Typography,
  ExpansionPanelActions,
  Divider,
  Button,
  Paper
} from "@material-ui/core";
import NotificationImportantIcon from "@material-ui/icons/NotificationImportant";

export default function AlertPanel(props) {
  let a = props.alert;
  let readAlert = props.readAlert;
  let clusterId = props.clusterId;

  return (
    <ExpansionPanel key={a.uuid}>
      <ExpansionPanelSummary>
        <Grid container spacing={1}>
          <Grid item container xs alignContent="center">
            <Grid item>
              {!a.isRead &&
                <NotificationImportantIcon
                  color="primary"
                  style={{ marginRight: "5px" }}
                />}
            </Grid>
            <Grid item>
              <Typography>{a.title}</Typography>
            </Grid>
          </Grid>
          <Grid
            item
            container
            xs={4}
            alignContent="center"
            justify="space-evenly"
          >
            <Box m={0.3}>
              <Grid item>
                <Chip label={new Date(a.timestamp).toLocaleString()} />
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
        <Box container display="block" spacing={3}>
          <Box m={1}>
            <Typography variant="caption" color="textSecondary">
              {"Action ID: " + a.uuid}
            </Typography>
          </Box>
            <Box m={1}>
              <Typography>{a.body}</Typography>
            </Box>
        </Box>
      </ExpansionPanelDetails>
      <Divider />
      <ExpansionPanelActions>
        <Button variant="contained" color="primary" disabled={a.isRead} onClick={readAlert}>
          Read
        </Button>
      </ExpansionPanelActions>
    </ExpansionPanel>
  );
}
