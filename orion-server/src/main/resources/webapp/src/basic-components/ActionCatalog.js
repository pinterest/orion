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
import React, { useState } from "react";
import { Box, Grid, Button, Icon } from "@material-ui/core";
import ActionDialog from "./ActionDialog";

export default function ActionCatalog({ cluster, isAdmin }) {
  const [selectedAction, setSelectedAction] = useState();
  const actions = getActionCatalogEntries(cluster);
  
  let panel = actions.map((action) => {
    // load the component
    return (
      <Grid item xs={2} key={action.actionKey}>
        <Button
          onClick={(event) => {
            setSelectedAction(action);
          }}
          style={{ width: "80%", backgroundColor: "#f0eaeb" }}
          disabled={!isAdmin}
        >
          <Box>
            <Box>{<Icon style={{ fontSize: "30pt" }}>{action.icon}</Icon>}</Box>
            <Box>{action.displayName}</Box>
          </Box>
        </Button>
      </Grid>
    );
  });
  return (
    <Box>
      <ActionDialog
        clusterId={cluster.clusterId}
        action={selectedAction}
        clearAction={() => setSelectedAction(null)}
      />
      <Grid container spacing={1}>
        {panel}
      </Grid>
    </Box>
  );
}

function getActionCatalogEntries(cluster) {
  let actionData = [];
  if (cluster.actionEngine && cluster.actionEngine.actionSchemas) {
    actionData = cluster.actionEngine.actionSchemas.filter(
      (a) =>
        a.attributes && a.attributes.every((attr) => attr.name !== "nodeIds")
    );
  }
  return actionData;
}
