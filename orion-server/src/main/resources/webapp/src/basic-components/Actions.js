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
import React, { useEffect } from "react";
import ActionCatalog from "./ActionCatalog";
import { Box, Typography, Divider } from "@material-ui/core";
import { connect } from "react-redux";
import {
  startClusterActionsSync,
  stopClusterActionsSync,
} from "../actions/cluster";
import ActionTreeView from "./ActionTreeView";

const mapDispatch = {
  startClusterActionsSync,
  stopClusterActionsSync,
};

const mapState = (state) => {
  const { user } = state;
  return {
    user,
  };
};

function Actions({
  cluster,
  user,
  startClusterActionsSync,
  stopClusterActionsSync,
}) {
  let actionsPanel = null;
  const clusterId = cluster.clusterId;
  useEffect(() => {
    startClusterActionsSync(clusterId);
    return stopClusterActionsSync;
  }, [clusterId, startClusterActionsSync, stopClusterActionsSync]);
  if (cluster.actionEngine) {
    if (
      !cluster.actionEngine.trackedActionsList ||
      cluster.actionEngine.trackedActionsList.length === 0
    ) {
      actionsPanel = (
        <Box>
          <Typography>
            No actions have been run or planned for this cluster.
          </Typography>
        </Box>
      );
    } else {
      const trackedActionsList = cluster.actionEngine.trackedActionsList;
      let sortedActionsToDisplay = trackedActionsList.sort(
        (a1, a2) => a2.createTime - a1.createTime
      );
      actionsPanel = (
        <ActionTreeView
          actions={sortedActionsToDisplay}
          clusterId={clusterId}
        />
      );
    }
  }
  return (
    <Box>
      <Box style={{ paddingBottom: "20px" }}>
        <Typography variant="h5">Action Catalog</Typography>
        <ActionCatalog cluster={cluster} isAdmin={user.isAdmin} />
      </Box>
      <Divider />
      <Box style={{ paddingTop: "20px" }}>
        <Typography variant="h5">Action History</Typography>
        {actionsPanel}
      </Box>
    </Box>
  );
}

export default connect(mapState, mapDispatch)(Actions);
