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
import Button from "@material-ui/core/Button";
import Menu from "@material-ui/core/Menu";
import MenuItem from "@material-ui/core/MenuItem";
import React, { useState } from "react";
import Snackbar from "@material-ui/core/Snackbar";
import IconButton from "@material-ui/core/IconButton";
import CloseIcon from "@material-ui/icons/Close";

export default function NodeActionMenu(props) {
  let nodeId = props ? props.node.nodeId : null;
  let clusterId = props ? props.node.clusterId : null;

  const [anchorEl, setAnchorEl] = useState(null);
  const [actionId, setActionId] = useState(null);
  const [open, setOpen] = useState(false);
  const [actionText, setActionText] = useState("dummy");

  const handleClick = event => {
    setAnchorEl(event.currentTarget);
  };

  const executeNodeAction = type => {
    let url =
      "/api/clusters/" + clusterId + "/nodes/" + nodeId + "/actions/" + type;
    setActionText("Triggered " + type + " on node:" + nodeId);
    fetch(url, {
      method: "post"
    })
      .then(response => {
        if (!response.ok) {
          return null;
        } else {
          return response.text();
        }
      })
      .then(data => {
        if (data) {
          console.log(data);
          setActionId(data);
        } else {
          setActionText("Action failed");
        }
        setOpen(true);
      });
    console.log("action executed:" + type + " actionid:" + actionId);
  };

  const handleClose = (event, type) => {
    setAnchorEl(null);
    executeNodeAction(type);
  };

  const handleCloseToast = (event, reason) => {
    if (reason === "clickaway") {
      return;
    }
    setOpen(false);
  };

  return (
    <div>
      <Button
        aria-controls="simple-menu"
        aria-haspopup="true"
        onClick={handleClick}
      >
        Open Menu
      </Button>
      <Snackbar
        anchorOrigin={{
          vertical: "bottom",
          horizontal: "left"
        }}
        open={open}
        autoHideDuration={2000}
        ContentProps={{
          "aria-describedby": "message-id"
        }}
        onClose={handleCloseToast}
        message={<span id="message-id">{actionText}</span>}
        action={[
          <IconButton
            key="close"
            aria-label="close"
            color="inherit"
            onClick={handleCloseToast}
          >
            <CloseIcon />
          </IconButton>
        ]}
      />
      <Menu
        id="simple-menu"
        anchorEl={anchorEl}
        keepMounted
        open={Boolean(anchorEl)}
        onClose={handleClose}
      >
        <MenuItem onClick={event => handleClose(event, "restart")}>
          Restart Service
        </MenuItem>
        <MenuItem onClick={event => handleClose(event, "stop")}>
          Stop Service
        </MenuItem>
        <MenuItem onClick={event => handleClose(event, "start")}>
          Start Service
        </MenuItem>
        <MenuItem onClick={event => handleClose(event, "update")}>
          Update Configs
        </MenuItem>
        <MenuItem onClick={event => handleClose(event, "dummy")}>
          Dummy
        </MenuItem>
      </Menu>
    </div>
  );
}
