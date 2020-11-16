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
import MaterialTable from "material-table";

export default function OffsetsPanel({ consumerGroup }) {
  const rows = consumerGroup.offsets || {};

  const data = Object.entries(rows)
    .map(([key, value]) => {
      let topic = key.slice(0, key.lastIndexOf("-"));
      let p = key.slice(key.lastIndexOf("-") + 1);
      return {
        topic: topic,
        partition: p,
        beginningOffset: value.beginningOffset,
        endOffset: value.endOffset,
        offset: value.offset,
        lag: value.lag,
      };
    })
    .sort((a, b) => {
      return a.topic.localeCompare(b.topic) || a.partition - b.partition;
    });

  return (
    <MaterialTable
      options={{
        pageSize: Math.min(20, data.length),
        grouping: true,
        filtering: false,
      }}
      title=""
      columns={[
        { title: "Topic", field: "topic" },
        { title: "Partition", field: "partition", type: "numeric" },
        {
          title: "Beginning Offset",
          field: "beginningOffset",
          type: "numeric",
        },
        { title: "End Offset", field: "endOffset", type: "numeric" },
        { title: "Offset", field: "offset", type: "numeric" },
        { title: "Lag", field: "lag", type: "numeric" },
      ]}
      data={data}
    />
  );
}
