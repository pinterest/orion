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
import { Grid, Typography, Box, Container } from "@material-ui/core";
import ActionTreeView from "./ActionTreeView";

export default function GlobalActions(props) {
  const clusters = props.clusters;
  const actions = getGlobalActionsList(clusters);

  return (
    <div>
      <Grid style={{ paddingTop: "30px" }}>
        <Grid container spacing={2}>
          <Typography variant="h5">Global Actions</Typography>
        </Grid>
      </Grid>
      {actions && actions.length > 0 && (
        <Box display="flex" alignItems="flex-start" mt={4}>
          <Container style={{ paddingLeft: 0 }}>
            <ActionTreeView actions={getGlobalActionsList(clusters)} />
          </Container>
        </Box>
      )}
    </div>
  );
}

const statusEnum = {
  NOT_STARTED: 0,
  RUNNING: 1,
  SUCCEEDED: 2,
  FAILED: 2,
  CANCELLED: 2 
}
function getGlobalActionsList(clusters) {
  return clusters
    .flatMap((cluster) => {
      return cluster.actions.map((action) => {
        return {
          ...action,
          clusterId: cluster.clusterId,
        };
      });
    })
    .sort((a1, a2) => statusEnum[a1.status] - statusEnum[a2.status] || a2.createTime - a1.createTime);
}
