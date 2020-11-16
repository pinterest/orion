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
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Snackbar
} from "@material-ui/core";
import React, { useState } from "react";
import { connect } from "react-redux";
import { receiveClusterMaintenanceStatus } from "../actions/cluster";
import Alert from "@material-ui/lab/Alert";
import { receiveClustersSummaryMaintenanceStatus } from "../actions/clusterSummary";

const mapState = state => {
  const { clusterView, user } = state;
  return {
    maintenance: clusterView.cluster.underMaintenance,
    cluster: clusterView.cluster,
    isAdmin: user.isAdmin
  };
};

const mapDispatch = {
  setClusterMaintenanceStatus: receiveClusterMaintenanceStatus,
  setClusterSummaryMaintenanceStatus: receiveClustersSummaryMaintenanceStatus
};

function MaintenanceButton({
  cluster,
  maintenance,
  isAdmin,
  setClusterMaintenanceStatus,
  setClusterSummaryMaintenanceStatus
}) {
  const [open, setOpen] = useState(false);
  const [openToast, setOpenToast] = useState(false);
  const [toastData, setToastData] = useState({});

  const handleClickOpen = () => {
    setOpen(true);
  };

  const handleCancel = () => {
    setOpen(false);
  };

  const handleCloseToast = (event, reason) => {
    if (reason === "clickaway") {
      return;
    }

    setOpenToast(false);
  };

  const handleConfirm = () => {
    const method = maintenance ? "DELETE" : "PUT";
    fetch("/api/clusters/" + cluster.clusterId + "/maintenance", {
      method: method,
      headers: {
        "Content-Type": "application/json"
      }
    }).then(response => {
      if (!response.ok) {
        response.json().then(error => {
          console.error(error);
          setOpen(false);
          setToastData({
            severity: "error",
            content:
              "Failed to update cluster  " +
              cluster.clusterId +
              " maintenance mode: " +
              error.code +
              ": " +
              error.message
          });
          setOpenToast(true);
        });
        return null;
      } else {
        setClusterMaintenanceStatus(cluster.clusterId, !maintenance);
        setClusterSummaryMaintenanceStatus(cluster.clusterId, !maintenance);
        setOpen(false);
        setToastData({
          severity: "success",
          content:
            "Cluster " +
            cluster.clusterId +
            (!maintenance ? " is in " : " is out of ") +
            " maintenance mode"
        });
        setOpenToast(true);
      }
    });
  };

  return (
    <Box>
      <Button variant="outlined" color="primary" onClick={handleClickOpen} disabled={!isAdmin}>
        {maintenance ? "Disable" : "Enable"} maintenance
      </Button>
      <Dialog
        open={open}
        onClose={handleCancel}
        aria-labelledby="alert-dialog-title"
        aria-describedby="alert-dialog-description"
      >
        <DialogTitle id="alert-dialog-title">
          {maintenance ? "Disable" : "Enable"} maintenance mode
        </DialogTitle>
        <DialogContent>
          <DialogContentText id="alert-dialog-description">
            Do you want to {maintenance ? "disable" : "enable"} maintenance mode
            on this cluster?
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleConfirm} color="primary">
            Yes
          </Button>
          <Button onClick={handleCancel} color="primary" autoFocus>
            Cancel
          </Button>
        </DialogActions>
      </Dialog>
      <Snackbar
        anchorOrigin={{
          vertical: "bottom",
          horizontal: "left"
        }}
        open={openToast}
        autoHideDuration={2000}
        onClose={handleCloseToast}
      >
        <Alert severity={toastData.severity}>{toastData.content}</Alert>
      </Snackbar>
    </Box>
  );
}

export default connect(mapState, mapDispatch)(MaintenanceButton);
