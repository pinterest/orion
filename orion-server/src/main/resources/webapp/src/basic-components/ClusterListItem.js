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
import { ListItem, Grid, Typography, Chip, Box, Icon } from "@material-ui/core";
import NotificationImportantIcon from "@material-ui/icons/NotificationImportant";
import AppsIcon from "@material-ui/icons/Apps";
import DoubleArrowIcon from "@material-ui/icons/DoubleArrow";
import { Link as RouterLink, useRouteMatch } from "react-router-dom";

export default function ClusterListItem(props) {
  let cluster = props.cluster;
  let match = useRouteMatch("/clusters/:clusterId/:tab");
  let tabSuffix = match && match.params.tab ? "/" + match.params.tab : "";

  let clusterTextColor = "green";
  if (!cluster.healthy) {
    clusterTextColor = "orange";
  }

  return (
    <ListItem
      button
      divider
      selected={match && match.params.clusterId === cluster.clusterId}
      component={RouterLink}
      to={"/clusters/" + cluster.clusterId + tabSuffix}
    >
      <Grid container justify="space-evenly">
        <Grid item xs={12}>
          <Box className="ListItemClusterName" textAlign="center">
            <Typography style={{ color: clusterTextColor }}>
              {cluster.name}{" "}
              {cluster.underMaintenance && (
                <Icon color="disabled" style={{ fontSize: "16px" }}>
                  build
                </Icon>
              )}
            </Typography>
          </Box>
        </Grid>
        <Grid item>
          <Chip size="small" label={cluster.nodes} icon={<AppsIcon />} />
        </Grid>
        <Grid item>
          <Chip
            size="small"
            label={cluster.alerts.length}
            icon={<NotificationImportantIcon />}
          />
        </Grid>
        <Grid>
          <Chip
            size="small"
            label={cluster.actions.length}
            icon={<DoubleArrowIcon />}
          />
        </Grid>
      </Grid>
    </ListItem>
  );
}
