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
import { Typography, Paper, Box } from "@material-ui/core";

export default function ClusterSummary(props) {
  let cluster = props.cluster;
  if (cluster.attributes["zookeeper.connect"]) {
    return (
      <Paper variant="outlined">
        <Box mx={2} my={1}>
          <Typography
            variant="caption"
          >
            {cluster.attributes["zookeeper.connect"]}
          </Typography>
        </Box>
      </Paper>
    );
  } else {
    return <div></div>;
  }
}
