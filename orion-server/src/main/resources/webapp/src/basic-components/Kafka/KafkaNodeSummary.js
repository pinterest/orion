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
import { Typography, Grid } from "@material-ui/core";
import { makeStyles } from "@material-ui/core/styles";

export default function KafkaNodeSummary(props) {
  let node = props.node;
  let topicCount = node.topicPartitionsForNode
    ? node.topicPartitionsForNode.length
    : 0;

  return (
    <div>
      <Grid item container xs alignContent="center">
        <Grid item container xs={1} alignContent="center">
          <Grid>
            <Typography variant="subtitle1" color="textSecondary">
              Topics:{topicCount}
            </Typography>
          </Grid>
        </Grid>
      </Grid>
      <Grid item container xs={1} alignContent="center">
        <Typography variant="subtitle1" color="textSecondary">
          Partitions:
          {node.topicPartitionsForNode
            .map(m => (m.partitions ? m.partitions.length : 0))
            .reduce((l1, l2) => l1 + l2, 0)}
        </Typography>
      </Grid>
    </div>
  );
}
