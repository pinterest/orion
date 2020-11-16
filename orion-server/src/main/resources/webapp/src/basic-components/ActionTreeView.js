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
import React, { useState } from "react";
import { TreeView, TreeItem } from "@material-ui/lab";
import {
  Grid,
  Box,
  Typography,
  Card,
  CardContent,
  TextField,
  Chip,
  SvgIcon,
  makeStyles,
} from "@material-ui/core";
import ExpandMoreIcon from "@material-ui/icons/ExpandMore";
import ChevronRightIcon from "@material-ui/icons/ChevronRight";
import CheckCircleIcon from "@material-ui/icons/CheckCircle";
import ErrorIcon from "@material-ui/icons/Error";
import HourglassEmptyIcon from "@material-ui/icons/HourglassEmpty";
import ScheduleIcon from "@material-ui/icons/Schedule";
import CancelIcon from "@material-ui/icons/Cancel";
import ReactJson from "react-json-view";

const useActionTreeItemStyles = makeStyles({
  iconContainer: {
    marginRight: "2px",
  },
  content: {
    borderBottom: "1px dashed #aaa",
  },
  group: {
    marginLeft: "10px",
    paddingLeft: "12px",
    borderLeft: "1px dashed #aaa",
    marginBottom: "2px"
  },
  label: {
    overflow: "hidden",
  },
});

const treeItemIcons = {
  FAILED: <ErrorIcon style={{ fill: "red" }} />,
  SUCCEEDED: <CheckCircleIcon style={{ fill: "green" }} />,
  RUNNING: <HourglassEmptyIcon style={{ fill: "blue" }} />,
  CANCELLED: <CancelIcon style={{ fill: "orange" }} />,
  NOT_STARTED: <ScheduleIcon style={{ fill: "purple" }} />,
};

export default function ActionTreeView({ actions, clusterId }) {
  const classes = useActionTreeItemStyles();
  const uuidActionMap = {};

  const [displayAction, setDisplayAction] = useState(actions && actions[0]);
  const [selected, setSelected] = useState(actions && actions[0].uuid);
  const [expanded, setExpanded] = useState([
    actions && Array.isArray(actions[0].children) && actions[0].uuid,
  ]);
  const getActionStatusIcon = (action) => {
    return treeItemIcons[action.status];
  };
  const renderTree = (actions, clusterId, shouldDisplayIndex) => {
    const totalActions = actions.length;
    return actions.map((action, idx) => {
      const actualClusterId = action.clusterId || clusterId;
      const icon = getActionStatusIcon(action);
      return (
        <TreeItem
          key={action.uuid}
          nodeId={action.uuid}
          classes={classes}
          icon={
            Array.isArray(action.children) && action.children.length ? null : (
              <Box>
                {icon}
                <SvgIcon />
              </Box>
            )
          }
          expandIcon={
            <Box>
              {icon}
              <ChevronRightIcon />
            </Box>
          }
          collapseIcon={
            <Box>
              {icon}
              <ExpandMoreIcon />
            </Box>
          }
          label={
            <Grid container>
              <Grid item xs={12}>
                <Typography noWrap>{(shouldDisplayIndex ? "(" + (idx + 1) + "/" + totalActions + ") " : "") + action.name}</Typography>
              </Grid>
              <Grid item xs={12} lg={7}>
                <Typography variant="caption" noWrap>
                  {new Date(action.createTime).toLocaleString()}
                </Typography>
              </Grid>
              {actualClusterId && (
                <Grid item xs={12} lg={5}>
                  <Typography variant="caption" noWrap>
                    {actualClusterId}
                  </Typography>
                </Grid>
              )}
            </Grid>
          }
        >
          {Array.isArray(action.children)
            ? renderTree(action.children, actualClusterId, true)
            : null}
        </TreeItem>
      );
    });
  };

  const buildUUIDActionMap = (actions, clusterId) => {
    actions.forEach((action) => {
      if (clusterId) {
        uuidActionMap[action.uuid] = { ...action, clusterId };
      } else {
        uuidActionMap[action.uuid] = action;
      }
      if (action.children)
        buildUUIDActionMap(action.children, action.clusterId || clusterId);
    });
  };
  buildUUIDActionMap(actions, clusterId);

  const nodeSelectHandler = (event, value) => {
    setSelected(value);
    const currentAction = uuidActionMap[value];
    if (currentAction) {
      setDisplayAction(uuidActionMap[value]);
    }
  };

  const nodeToggleHandler = (event, value) => {
    if (expanded.length > value.length) {
      // this is to only collapse an item if it is selected
      // since users might want to check the parent action after clicking on a child action
      if (expanded.includes(selected) && !value.includes(selected)) {
        setExpanded(value);
      }
    } else {
      setExpanded(value);
    }
  };

  if (!actions) {
    return <Box></Box>;
  }

  return (
    <Grid container>
      <Grid item xs={3} style={{overflowY: "scroll"}}>
        <TreeView
          selected={selected}
          expanded={expanded}
          onNodeSelect={nodeSelectHandler}
          onNodeToggle={nodeToggleHandler}
          defaultCollapseIcon={<ExpandMoreIcon />}
          defaultExpandIcon={<ChevronRightIcon />}
        >
          {renderTree(actions)}
        </TreeView>
      </Grid>
      <Grid item xs={9}>
        <Box m={1}>
          {displayAction && (
            <ActionTreeViewDetail
              action={displayAction}
              clusterId={displayAction.clusterId || clusterId}
            />
          )}
        </Box>
      </Grid>
    </Grid>
  );
}

function ActionTreeViewDetail({ action, clusterId }) {
  const getFormattedOutput = (value) => value || "None";

  const renderAttributes = (attributes) => {
    return (
      <ReactJson
        src={attributes}
        name={"attributes"}
        shouldCollapse={(f) => f.name !== "attributes"}
        enableClipboard={false}
        displayDataTypes={false}
      />
    );
  };

  return (
    <Grid container spacing={2}>
      <Grid item xs={8}>
        <Box>
          <Typography>{action.name}</Typography>
        </Box>
        <Box>
          <Typography variant="caption" color="textSecondary">
            {"Action ID: " + action.uuid}
          </Typography>
        </Box>
      </Grid>
      <Grid item xs={4} container alignContent="center" justify="space-evenly">
        <Box m={0.3}>
          <Grid item>
            <Chip label={new Date(action.createTime).toLocaleString()} />
          </Grid>
        </Box>
        <Box m={0.3}>
          <Grid item>
            <Chip label={clusterId} />
          </Grid>
        </Box>
      </Grid>
      <Grid item container xs={12}>
        <Grid item xs={12}>
          <Box m={1}>
            <TextField
              id="filled-basic"
              label="Output"
              variant="outlined"
              readOnly
              multiline
              fullWidth
              rowsMax={12}
              value={getFormattedOutput(action.result.out)}
            />
          </Box>
        </Grid>
        <Grid item xs={12}>
          <Box m={1}>
            <TextField
              id="filled-basic"
              label="Error"
              variant="outlined"
              readOnly
              multiline
              fullWidth
              rowsMax={12}
              value={getFormattedOutput(action.result.err)}
            />
          </Box>
        </Grid>
        <Grid item xs={12}>
          <Box m={1}>
            <Card
              variant="outlined"
              style={{ overflowX: "scroll", backgroundColor: "rgba(0,0,0,0)" }}
            >
              <CardContent>{renderAttributes(action.attributes)}</CardContent>
            </Card>
          </Box>
        </Grid>
      </Grid>
    </Grid>
  );
}

ActionTreeView.defaultProps = {
  displayCluster: false,
};
