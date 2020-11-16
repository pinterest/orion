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
import MembersPanel from "./MembersPanel";
import OffsetsPanel from "./OffsetsPanel"
import MaterialTable from "material-table";
import { Box } from "@material-ui/core";
import { connect } from "react-redux";
import { requestConsumerGroup } from "../../actions/kafka"

const mapDispatch = {
  requestConsumerGroup
}
function ConsumerGroups({cluster, requestConsumerGroup}) {
  let rows = [];
  
  if (!cluster.attributes.consumerGroups) {
    requestConsumerGroup(cluster.clusterId);
  } else {
    rows = Object.values(cluster.attributes.consumerGroups);
  }

  const managedConsumers = cluster.attributes.consumers || {};

  let data = [];
  let columns = [
    { title: "Consumer Group", field: "groupId" },
    { title: "Coordinator", field: "coordinator", type: "numeric" },
    { title: "Partition Assignor", field: "partitionAssignor" },
    { title: "State", field: "state" },
    { title: "Simple Group", field: "simpleConsumerGroup", type: "boolean" },
    { title: "Members", field: "members", type: "numeric" },
    { title: "Managed", field: "managed", type: "boolean" }
  ];
  if (rows.length > 0) {
  }
  rows.map(row => {
    data.push({
      groupId: row.groupId,
      coordinator: row.coordinator,
      partitionAssignor: row.partitionAssignor,
      state: row.state,
      simpleConsumerGroup: row.simpleConsumerGroup,
      members: row.members.length,
      managed: row.groupId in managedConsumers,
      raw: row
    });
  });
  return (
    <Box>
      <MaterialTable
        title=""
        options={{ pageSize: 10, grouping: true, filtering: false }}
        detailPanel={[
          {
            tooltip: 'Show Offsets',
            render: rowData => {
              return (
                <div style={{ backgroundColor: "black" }}>
                  <div style={{ padding: "20px" }}>
                    <OffsetsPanel cluster={cluster} consumerGroup={rowData.raw} />
                  </div>
                </div>
              );
            },
          },
          {
            icon: 'group', //'account_circle',
            tooltip: 'Show Members',
            render: rowData => {
              return (
                  <div style={{ backgroundColor: "black" }}>
                    <div style={{ padding: "20px" }}>
                      <MembersPanel cluster={cluster} consumerGroup={rowData.raw} />
                    </div>
                  </div>
              );
            },
          },
        ]}
        columns={columns}
        data={data}
      />
    </Box>
  );
}

export default connect(null, mapDispatch)(ConsumerGroups);