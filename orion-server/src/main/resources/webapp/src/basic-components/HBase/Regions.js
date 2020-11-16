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
import React, { Component, forwardRef } from "react";
import { Suspense, lazy, useState } from "react";
import { useRouteMatch } from "react-router-dom";
import { useHistory } from "react-router-dom";
import MaterialTable from "material-table";
import { makeStyles } from "@material-ui/core/styles";
import { Button, Box, Modal, Fade, Backdrop } from "@material-ui/core";
import CreateTopicPanel from "./CreateTopicPanel";
import KafkaTopic from "./KafkaTopic";

const useStyles = makeStyles({
  MuiTableCell: {
    backgroundColor: "black",
  },
});

const modalStyles = makeStyles((theme) => ({
  modal: {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
  },
  paper: {
    backgroundColor: theme.palette.background.paper,
    border: "2px solid #000",
    maxHeight: "500px",
    boxShadow: theme.shadows[5],
    padding: theme.spacing(2, 4, 3),
  },
}));

export default function Regions(props) {
  let clusterId = props.cluster.clusterId;

  let regionserverOnlineRegions = {};
  if (props.cluster.attributes.regionserverOnlineRegions) {
    topicInfoRows = Object.values(
      props.cluster.attributes.regionserverOnlineRegions
    );
  }

  const classes = modalStyles();

  const history = useHistory();
  let match = useRouteMatch("/clusters/:clusterId/service/regions/:region?");

  const [selectedRow, setSelectedRow] = useState();
  const [openModal, setOpenModal] = useState(false);
  const [openDetailsModal, setOpenDetailsModal] = useState(false);
  const handleOpen = () => {
    setOpenModal(true);
  };
  const handleClose = () => {
    setOpenModal(false);
  };
  const handleDetailsClose = () => {
    setOpenDetailsModal(false);
    setSelectedRow();
    history.push("/clusters/" + clusterId + "/service/regions");
  };

  if (match && match.params.topicName) {
    if (!selectedRow) {
      setSelectedRow(topicToRowValuesMap[match.params.topicName]);
    }
  }

  if (selectedRow != null && !openDetailsModal) {
    setOpenDetailsModal(true);
  }

  let data = Object.values(topicToRowValuesMap);
  let columns = [
    { title: "Table", field: "table" },
    { title: "Region", field: "region" },
  ];

  const exportAsJson = async () => {
    const fileName = "export";
    const json = JSON.stringify(data);
    const blob = new Blob([json], { type: "application/json" });
    const href = await URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = href;
    link.download = fileName + ".json";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <Box>
      <Modal
        aria-labelledby="transition-modal-title"
        aria-describedby="transition-modal-description"
        className={classes.modal}
        open={openDetailsModal}
        onClose={handleDetailsClose}
        closeAfterTransition
        BackdropComponent={Backdrop}
        BackdropProps={{
          timeout: 500,
        }}
      >
        <Fade in={openDetailsModal}>
          <div
            style={{
              backgroundColor: "white",
              padding: "20px",
              width: "1000px",
            }}
          >
            {selectedRow && (
              <Suspense fallback={<div>Loading...</div>}>
                <KafkaTopic rowData={selectedRow} clusterId={clusterId} />
              </Suspense>
            )}
          </div>
        </Fade>
      </Modal>
      <MaterialTable
        options={{ pageSize: 10, grouping: true, filtering: false }}
        title={""}
        onRowClick={(event, rowData, togglePanel) => {
          history.push(
            "/clusters/" + clusterId + "/service/regions/" + rowData.topic
          );
          setSelectedRow(rowData);
          setOpenDetailsModal(true);
        }}
        columns={columns}
        data={data}
      />
      <Box m={2}>
        <Button variant="contained" color="primary" onClick={exportAsJson}>
          Export Topic Data as JSON
        </Button>
      </Box>
    </Box>
  );
}

function calculateTopicSizeInTB(row) {
  return (
    row.partitions
      .map((p) =>
        Object.values(p.replicainfo)
          .map((v) => v.size)
          .reduce((l1, l2) => l1 + l2, 0)
      )
      .reduce((l1, l2) => l1 + l2, 0) / 1099511627776
  ).toFixed(2);
}
