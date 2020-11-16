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
import React, { Suspense, lazy, useState } from "react";
import { useRouteMatch } from "react-router-dom";
import {
  Box,
  Typography,
  Paper,
  Grid,
  Divider,
  Card,
  Button,
  Modal,
  Fade,
  Backdrop,
  IconButton,
  Icon
} from "@material-ui/core";
import { makeStyles } from "@material-ui/core/styles";
import { useHistory } from "react-router-dom";

const modalStyles = makeStyles(theme => ({
  modal: {
    display: "flex",
    alignItems: "center",
    justifyContent: "center"
  },
  paper: {
    backgroundColor: theme.palette.background.paper,
    border: "2px solid #000",
    maxWidth: "700px",
    maxHeight: "500px",
    boxShadow: theme.shadows[5],
    padding: theme.spacing(2, 4, 3)
  }
}));

const actButtonStyle = makeStyles({});

export default function ActionStore(props) {
  const classes = modalStyles();
  const history = useHistory();
  let cluster = props.cluster;
  let entries = getActionStoreEntries(cluster);
  const isAdmin = props.isAdmin;

  let match = useRouteMatch("/:clusterId/actions/:key?");
  const [selectedAction, setSelectedAction] = useState();
  const [openModal, setOpenModal] = useState(false);

  if (match && match.params.key != selectedAction) {
    console.log("matched");
    setSelectedAction(match.params.key);
  }
  if (selectedAction != null && !openModal) {
    setOpenModal(true);
  }

  const handleCloseModal = () => {
    setOpenModal(false);
    setSelectedAction(null);
    history.push("/" + cluster.clusterId + "/actions");
  };

  let panel = Object.values(entries).map(e => {
    // load the component
    return (
      <Grid item xs={2} key={e.key}>
        <Button
          onClick={event => {
            setSelectedAction(e.key);
            history.push("/" + cluster.clusterId + "/actions/" + e.key);
          }}
          style={{ width: "80%", backgroundColor: "#f0eaeb" }}
          disabled={!isAdmin}
        >
          <Box>
            <Box>{<Icon style={{ fontSize: "30pt" }}>{e.icon}}</Icon>}</Box>
            <Box>{e.displayName}</Box>
          </Box>
        </Button>
      </Grid>
    );
  });
  return (
    <Box>
      <Modal
        aria-labelledby="transition-modal-title"
        aria-describedby="transition-modal-description"
        className={classes.modal}
        open={openModal}
        onClose={handleCloseModal}
        closeAfterTransition
        BackdropComponent={Backdrop}
        BackdropProps={{
          timeout: 100
        }}
      >
        <Fade in={openModal}>
          <Card
            style={{
              backgroundColor: "white",
              padding: "20px",
              maxWidth: "1000px"
            }}
          >
            {selectedAction && (
              <Suspense fallback={<div>Loading...</div>}>
                {renderComponent(
                  entries[selectedAction].component,
                  cluster,
                  entries[selectedAction]
                )}
              </Suspense>
            )}
          </Card>
        </Fade>
      </Modal>
      <Grid container spacing={1}>
        {panel}
      </Grid>
    </Box>
  );
}

function getActionStoreEntries(cluster) {
  let entries = baseActionSet();
  switch (cluster.type) {
    case "Kafka":
      entries["rebalance"] = {
        key: "rebalance",
        displayName: "Rebalance",
        icon: "accessibility",
        component: lazy(() => import("./Kafka/Actions/Rebalance.js"))
      };
      entries["cutretention"] = {
        key: "cutretention",
        displayName: "Cut Retention",
        icon: "delete_sweep",
        component: lazy(() => import("./Kafka/Actions/CutRetention.js"))
      };
      break;
  }
  return entries;
}

function baseActionSet() {
  return {
    // restart: {
    //   key: "restart",
    //   displayName: "Restart",
    //   icon: "replay",
    //   component: lazy(() => import("./Commons/NodeAction.js"))
    // },
    // start: {
    //   key: "start",
    //   displayName: "Start",
    //   icon: "play_arrow",
    //   component: lazy(() => import("./Commons/NodeAction.js"))
    // },
    // stop: {
    //   key: "stop",
    //   displayName: "Stop",
    //   icon: "stop",
    //   component: lazy(() => import("./Commons/NodeAction.js"))
    // },
    replace: {
      key: "replace",
      displayName: "Replace",
      icon: "compare_arrows",
      component: lazy(() => import("./Commons/Replace.js"))
    }
  };
}

function renderComponent(Component, cluster, action) {
  return <Component cluster={cluster} action={action} />;
}
