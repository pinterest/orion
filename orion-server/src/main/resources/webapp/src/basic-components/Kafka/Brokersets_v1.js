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
import BrokersetPanel from "./BrokersetPanel";
import MaterialTable from "material-table";
import { Box } from "@material-ui/core";
import CreateTopicPanel from "./CreateTopicPanel";


export default function Brokersets(props) {
  let rows = [];
  if (props.cluster.attributes.brokerset) {
    rows = Object.values(props.cluster.attributes.brokerset);
  }

  let columns = [
    { title: "Alias", field: "brokersetAlias" },
    { title: "Brokers", field: "brokers" }
  ];
  let data = rows.map(row => ({
      brokersetAlias: row.brokersetAlias,
      brokers: row.size,
      raw: row
  }));
  return (
    <Box>
      <MaterialTable
        options={{ pageSize: 10, grouping: true, filtering: false }}
        title=""
        // style={useStyles}
        detailPanel={rowData => {
          return (
            <div style={{ backgroundColor: "black" }}>
              <div style={{ padding: "20px" }}>
                <BrokersetPanel brokerset={rowData.raw} />
              </div>
            </div>
          );
        }}
        columns={columns}
        data={data}
      />
    </Box>
  );
}
