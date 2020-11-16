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
import React, { forwardRef } from "react";

import MaterialTable from "material-table";

export default function OffsetsPanel(props) {
  let clusterId = props.cluster.clusterId;
  let consumerGroup = props.consumerGroup;
  let rows = [];
  if (consumerGroup) {
    rows = consumerGroup.members;
  }

  let data = [];
  rows.map(member => {
    data.push({
      clientId: member.clientId,
      consumerId: member.consumerId,
      host: member.host,
      assignment: JSON.stringify(member.assignment)
    });
  });

  return (
    <MaterialTable
      options={{ pageSize: Math.min(20, data.length), grouping: true, filtering: false }}
      title=""
      columns={[
        { title: "Client Id", field: "clientId" },
        { title: "Consumer Id", field: "consumerId" },
        { title: "Host", field: "host" },
        { title: "Assignment", field: "assignment" }
      ]}
      data={data}
    />
  );
}
