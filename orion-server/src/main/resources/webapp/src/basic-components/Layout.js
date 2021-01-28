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
  AppBar,
  Box,
  Toolbar,
  Typography,
  Link,
  Grid,
  Menu,
  MenuItem,
  IconButton,
  Container,
  Backdrop,
  CircularProgress,
  LinearProgress,
} from "@material-ui/core";
import { Icon } from "gestalt";
import React, { useState, useEffect } from "react";
import { Route } from "react-router-dom";
import Cluster from "./Cluster";
import ClusterListSideBar from "./ClusterListSideBar";
import AccountCircle from "@material-ui/icons/AccountCircle";
import { connect } from "react-redux";
import Summary from "./Summary";
import Homepage from "./Homepage";
import {
  requestClustersSummary,
  requestGlobalSensor,
} from "../actions/clusterSummary";
import { requestUser } from "../actions/user";
import { hideLoading, initializeApp } from "../actions/app";
import { makeStyles } from "@material-ui/core/styles";

const mapState = (state) => {
  const { clustersSummary, user, app, globalSensors } = state;
  return {
    clustersSummary,
    globalSensors,
    user,
    app,
  };
};

const mapDispatch = {
  requestClustersSummary,
  requestUser,
  requestGlobalSensor,
  initializeApp,
};

const useStyles = makeStyles((theme) => ({
  backdrop: {
    zIndex: theme.zIndex.drawer + 1,
    color: "#fff",
  },
  autoRefreshTimer: {
    width: "100%",
    zIndex: theme.zIndex.drawer + 2,
  },
  appBar: {
    zIndex: theme.zIndex.drawer + 1,
  },
}));

function Layout({
  clustersSummary,
  user,
  globalSensors,
  app,
  requestClustersSummary,
  requestUser,
  requestGlobalSensor,
  initializeApp,
}) {
  const classes = useStyles();

  useEffect(() => {
    requestClustersSummary();
    requestUser();
    requestGlobalSensor();
    initializeApp();
  }, [initializeApp, requestClustersSummary, requestUser, requestGlobalSensor]);
  return (
    <Box display="flex">
      <Backdrop
        className={classes.backdrop}
        open={app.showLoading}
        onClick={hideLoading}
      >
        <CircularProgress color="inherit" />
      </Backdrop>
      <AppBar position="fixed" className={classes.appBar}>
        <Toolbar>
          <Link href="/">
            <Box
              display="flex"
              alignItems="center"
              style={{ color: "white", textDecoration: "none" }}
            >
              <Icon
                icon="pinterest"
                accessibilityLabel="Pinterest"
                size={30}
                color="red"
              />
              <Typography variant="h6" style={{ paddingLeft: "10px" }}>
                Orion
              </Typography>
            </Box>
          </Link>
          <UserIconMenu user={user} />
        </Toolbar>
        {app.refreshInfo && <AutoRefreshTimer {...app.refreshInfo} />}
      </AppBar>
      {app.showError && (
        <Box marginTop={10}>
          No Clusters currently available to view. If Orion was recently started
          please wait a few minutes before checking. UI will attempt
          auto-refresh in 5s.
        </Box>
      )}
      {!app.showError && (
        <Box display="flex" flexGrow={1}>
          <Toolbar />
          <ClusterListSideBar clusters={clustersSummary} />
          <Container className="content" fixed>
            <Toolbar />
            <Route
              path="/:rootPath?/:subPath?"
              render={(routeProps) => {
                if (
                  routeProps.match.params.rootPath === "clusters" &&
                  routeProps.match.params.subPath
                ) {
                  return (
                    <Cluster clusterId={routeProps.match.params.subPath} />
                  );
                } else {
                  return (
                    <Homepage
                      clusters={clustersSummary}
                      globalSensors={globalSensors}
                    />
                  );
                }
              }}
            />
          </Container>
        </Box>
      )}
    </Box>
  );
}

function UserIconMenu({ user }) {
  let [anchorEl, setAnchorEl] = useState(null);

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleMenu = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const logout = () => {
    fetch("/__logout")
      .then((response) => {
        if (!response.ok) {
          this.setState({ showError: true });
          return "";
        }
        return response.text();
      })
      .then((data) => {
        console.log("logged out");
        handleClose();
      });
  };

  return (
    <Grid container alignItems="flex-start" justify="flex-end" direction="row">
      <IconButton onClick={handleMenu}>
        <AccountCircle style={{ fill: "white" }} />
        <Typography style={{ color: "white" }}>{user.name}</Typography>
      </IconButton>
      {user.auth && (
        <Menu
          id="menu-appbar"
          anchorEl={anchorEl}
          anchorOrigin={{
            vertical: "top",
            horizontal: "right",
          }}
          keepMounted
          transformOrigin={{
            vertical: "top",
            horizontal: "right",
          }}
          open={Boolean(anchorEl)}
          onClose={handleClose}
        >
          {/* <MenuItem onClick={handleClose}>Logout</MenuItem> */}
          {/* <MenuItem>Profile</MenuItem> */}
          <MenuItem onClick={logout}>Logout</MenuItem>
        </Menu>
      )}
    </Grid>
  );
}

function AutoRefreshTimer({ lastUpdateTime, interval }) {
  const classes = useStyles();
  const [progress, setProgress] = useState(0);

  let timer = () => {
    setProgress(
      Math.max(
        0,
        Math.min(100, (100 * (Date.now() - lastUpdateTime)) / interval)
      )
    );
  };

  useEffect(() => {
    const timerInterval = setInterval(timer, 100);
    return () => clearInterval(timerInterval);
  });
  return (
    <Box className={classes.autoRefreshTimer}>
      <LinearProgress variant="determinate" value={progress} />
    </Box>
  );
}

export default connect(mapState, mapDispatch)(Layout);
