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
  Backdrop,
  Box,
  Button,
  Fade,
  Icon,
  IconButton,
  Modal,
  Popover,
  Tooltip,
} from "@material-ui/core";
import { makeStyles } from "@material-ui/core/styles";
import {
  Build,
  CheckCircle,
  Error,
  GpsFixed,
  GpsOff,
  Help,
} from "@material-ui/icons";
import MUIDataTable from "mui-datatables";
import { TextField } from "mui-rff";
import React, { lazy, Suspense, useState } from "react";
import { Form } from "react-final-form";
import { connect } from "react-redux";
import { useHistory, useRouteMatch } from "react-router-dom";
import ActionDialog from "./ActionDialog";

const useStyles = makeStyles((theme) => ({
  modal: {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
  },
  popoverContent: {
    padding: theme.spacing(2),
    minWidth: "300px",
  },
}));

let columns = [];
let dataFunction;

const mapState = (state) => {
  const { user } = state;
  return {
    isAdmin: user.isAdmin,
  };
};

function Nodes({ cluster, isAdmin }) {
  const history = useHistory();
  let match = useRouteMatch("/clusters/:clusterId/nodes/:nodeId?");

  const [action, setAction] = useState(null);

  const [clickedNode, setClickedNode] = useState();

  const [selected, setSelected] = useState([]);
  const [clusterId, setClusterId] = useState();
  if (clusterId == null || clusterId !== cluster.clusterId) {
    setClusterId(cluster.clusterId);
    setSelected([]);
  }

  const handleCloseModal = () => {
    setClickedNode(null);
    history.push("/clusters/" + cluster.clusterId + "/nodes");
  };
  const classes = useStyles();

  const onClickNodeActionHandler = (action, nodeIds) => {
    return () => {
      setAction({
        ...action,
        nodeIds,
      });
    };
  };

  const serviceStatusEnum = {
    EMPTY: "empty",
    OK: "ok",
    FAILING: "failing",
  };

  const getServiceStatus = (row) =>
    row.serviceStatus == null
      ? serviceStatusEnum.EMPTY
      : row.serviceStatus.statusType === "OK"
      ? serviceStatusEnum.OK
      : serviceStatusEnum.FAILING;

  const agentStatusEnum = {
    MAINTENANCE: "maintenance",
    PRESENT: "present",
    ABSENT: "absent",
  };

  const getAgentStatus = (row) =>
    row.underMaintenance
      ? agentStatusEnum.MAINTENANCE
      : row.agentPresent
      ? agentStatusEnum.PRESENT
      : agentStatusEnum.ABSENT;

  const getAgentStatusComponent = (agentStatus) => {
    switch (agentStatus) {
      case agentStatusEnum.MAINTENANCE:
        return (
          <Tooltip title="Under Maintenance">
            <Build />
          </Tooltip>
        );
      case agentStatusEnum.PRESENT:
        return (
          <Tooltip title="Online">
            <GpsFixed />
          </Tooltip>
        );
      default:
        return (
          <Tooltip title="Offline">
            <GpsOff />
          </Tooltip>
        );
    }
  };

  const getServiceStatusComponent = (serviceStatus) => {
    switch (serviceStatus) {
      case serviceStatusEnum.OK:
        return (
          <Tooltip title="OK">
            <CheckCircle />
          </Tooltip>
        );
      case serviceStatusEnum.EMPTY:
        return (
          <Tooltip title="Empty">
            <Help />
          </Tooltip>
        );
      default:
        return (
          <Tooltip title="Failing">
            <Error />
          </Tooltip>
        );
    }
  };

  let rows = [];

  if (cluster.nodeMap) {
    rows = Object.values(cluster.nodeMap);
  }
  for(let i=0; i<rows.length; i++) {
    if (rows[i].decommissioned) {
      rows.splice(i, 1)
    }
  }
  let ClusterNodeDetails = loadClusterNodeType(cluster);

  const data = rows.map((row) => dataFunction(row));

  if (match && match.params.nodeId !== clickedNode) {
    setClickedNode(match.params.nodeId);
  }

  const dataIdList = data.map((n) => n.nodeId);

  let nodesActionData = [];
  if (cluster.actionEngine && cluster.actionEngine.actionSchemas) {
    nodesActionData = cluster.actionEngine.actionSchemas.filter(
      (a) =>
        a.attributes && a.attributes.some((attr) => attr.name === "nodeIds")
    );
  }

  return (
    <React.Fragment>
      <ActionDialog
        clusterId={clusterId}
        action={action}
        clearAction={() => setAction(null)}
      />
      <Modal
        aria-labelledby="transition-modal-title"
        aria-describedby="transition-modal-description"
        className={classes.modal}
        open={clickedNode != null}
        onClose={handleCloseModal}
        closeAfterTransition
        BackdropComponent={Backdrop}
        BackdropProps={{
          timeout: 100,
        }}
      >
        <Fade in={clickedNode != null}>
          <div
            style={{
              backgroundColor: "white",
              padding: "20px",
              width: "1000px",
            }}
          >
            {clickedNode && (
              <Suspense fallback={<div>Loading...</div>}>
                <ClusterNodeDetails
                  node={cluster.nodeMap[clickedNode]}
                  clusterId={cluster.clusterId}
                  cluster={cluster}
                />
              </Suspense>
            )}
          </div>
        </Fade>
      </Modal>
      <MUIDataTable
        options={{
          grouping: true,
          searchOpen: true,
          selectableRowsHeader: isAdmin,
          selectableRows: isAdmin ? "multiple" : "none",
          isRowSelectable: (dataIndex, expandedRows) => true,
          onRowClick: (rowData, rowMeta) => {
            history.push(
              "/clusters/" + cluster.clusterId + "/nodes/" + rowData[0]
            );
            setClickedNode(rowData[0]);
          },
          print: false,
          rowsPerPageOptions: [10, 20, 50],
          customToolbar: () => (
            <NodesToolbarExtension
              setSelected={setSelected}
              dataIdList={dataIdList}
              isAdmin={isAdmin}
            />
          ),
          customToolbarSelect: (selectedRows, displayData, setSelectedRows) => (
            <NodesSelectToolbar
              actionData={nodesActionData}
              selectedRows={selectedRows}
              onClickHandler={onClickNodeActionHandler}
              isAdmin={isAdmin}
              dataIdList={dataIdList}
            />
          ),
          rowsSelected: selected,
          onRowSelectionChange: (c, a, r) => {
            setSelected(r);
          },
        }}
        title=""
        columns={columns}
        data={data}
      />
    </React.Fragment>
  );

  function loadClusterNodeType(cluster) {
    let ClusterNode;
    switch (cluster.type) {
      case "Kafka":
        const KafkaClusterNode = lazy(() => import("./Kafka/KafkaNode"));
        ClusterNode = KafkaClusterNode;
        columns = getKafkaColumns();
        dataFunction = getKafkaNodeData(cluster);
        break;
      case "HBase":
        const HBaseClusterNode = lazy(() => import("./HBase/HBaseNode"));
        ClusterNode = HBaseClusterNode;
        columns = getHBaseColumns();
        dataFunction = getHBaseNodeData(cluster);
        break;
      case "MemQ":
        const MemQClusterNode = lazy(() => import("./MemQ/MemqNode"));
        ClusterNode = MemQClusterNode;
        columns = getMemqColumns();
        dataFunction = getMemQNodeData(cluster);
        break;
      default:
    }
    return ClusterNode;

    function getMemQNodeData(cluster) {
      return (row) => {
        let nodeType = "";
        if (row.agentNodeInfo) {
          // if agent is active
          nodeType = row.agentNodeInfo.nodeType;
        }

        return {
          nodeId: row.currentNodeInfo.nodeId,
          hostname: row.currentNodeInfo.hostname.split(".", 1)[0],
          ip: row.currentNodeInfo.ip,
          rack: row.currentNodeInfo.rack,
          nodeType: row.currentNodeInfo.nodeType,
          topics: row.topicPartitionsForNode
            ? row.topicPartitionsForNode.length
            : 0,
          agent: getAgentStatus(row),
          service: getServiceStatus(row),
          raw: row,
        };
      };
    }

    function getMemqColumns() {
      return [
        { label: "Node Id", name: "nodeId" },
        { label: "Name", name: "hostname" },
        { label: "Rack", name: "rack" },
        { label: "Node Type", name: "nodeType" },
        { label: "Topics", name: "topics", type: "numeric" },
      ];
    }

    function getKafkaNodeData(cluster) {
      return (row) => {
        let nodeType = "";
        if (row.agentNodeInfo) {
          // if agent is active
          nodeType = row.agentNodeInfo.nodeType;
        }

        return {
          nodeId: row.currentNodeInfo.nodeId,
          hostname: row.currentNodeInfo.hostname.split(".", 1)[0],
          ip: row.currentNodeInfo.ip,
          rack: row.currentNodeInfo.rack,
          nodeType: nodeType,
          topics: row.topicPartitionsForNode.length,
          partitions: row.topicPartitionsForNode
            .map((p) => p.partitions.length)
            .reduce((l1, l2) => l1 + l2, 0),
          leaders: row.topicPartitionsForNode
            .map((topic) => {
              return topic.partitions
                .map((partition) => {
                  const topicinfo = cluster.attributes.topicinfo[topic.topic];
                  return JSON.stringify(
                    topicinfo.partitions[partition].leader
                  ) === row.currentNodeInfo.nodeId
                    ? 1
                    : 0;
                })
                .reduce((c1, c2) => c1 + c2, 0);
            })
            .reduce((c1, c2) => c1 + c2, 0),
          ibp: row.currentNodeInfo.serviceInfo["inter.broker.protocol.version"],
          agent: getAgentStatus(row),
          service: getServiceStatus(row),
          usage: row.topicPartitionsForNode
            .map((topic) => {
              const topicinfo = cluster.attributes.topicinfo[topic.topic];
              return (
                topic.partitions
                  .map((partition) => {
                    let nodeReplicaInfo =
                      topicinfo.partitions[partition].replicainfo[
                        row.currentNodeInfo.nodeId
                      ];
                    return nodeReplicaInfo ? nodeReplicaInfo.size : 0;
                  })
                  .reduce((v1, v2) => v1 + v2, 0) /
                1024 /
                1024 /
                1024 /
                1024
              );
            })
            .reduce((v1, v2) => v1 + v2, 0)
            .toFixed(2),
          raw: row,
        };
      };
    }

    function getKafkaColumns() {
      return [
        { label: "Node Id", name: "nodeId" },
        { label: "Name", name: "hostname" },
        { label: "Rack", name: "rack" },
        { label: "Node Type", name: "nodeType" },
        { label: "Topics", name: "topics", type: "numeric" },
        { label: "Partitions", name: "partitions", type: "numeric" },
        { label: "Leaders", name: "leaders", type: "numeric" },
        { label: "Usage (TB)", name: "usage", type: "numeric" },
        { label: "Proto Version", name: "ibp" },
        {
          label: "Agent",
          name: "agent",
          options: {
            customBodyRenderLite: (dataIndex, rowIndex) =>
              getAgentStatusComponent(data[dataIndex].agent),
          },
        },
        {
          label: "Service",
          name: "service",
          options: {
            customBodyRenderLite: (dataIndex, rowIndex) =>
              getServiceStatusComponent(data[dataIndex].service),
          },
        },
      ];
    }

    function getHBaseNodeData(cluster) {
      return (row) => {
        let nodeType = "";
        if (row.agentNodeInfo) {
          // if agent is active
          nodeType = row.agentNodeInfo.nodeType;
        }
        let regionserverOnlineRegions =
          cluster.attributes.regionserverOnlineRegions.value[
            row.currentNodeInfo.nodeId
          ];

        var set = new Set();
        regionserverOnlineRegions.map((r) => set.add(r.tableName));
        return {
          nodeId: row.currentNodeInfo.nodeId,
          hostname: row.currentNodeInfo.hostname.split(".", 1)[0],
          port: row.currentNodeInfo.servicePort,
          ip: row.currentNodeInfo.ip,
          rack: row.currentNodeInfo.rack,
          nodeType: nodeType,
          regions: regionserverOnlineRegions.length,
          tables: set.size,
          agent: getAgentStatus(row),
          service: getServiceStatus(row),
          raw: row,
        };
      };
    }

    function getHBaseColumns() {
      return [
        { label: "Node Id", name: "nodeId" },
        { label: "Name", name: "hostname" },
        { label: "Port", name: "port" },
        { label: "Rack", name: "rack" },
        { label: "Node Type", name: "nodeType" },
        { label: "Regions", name: "regions", type: "numeric" },
        { label: "Tables", name: "tables", type: "numeric" },
        {
          label: "Agent",
          name: "agent",
          options: {
            customBodyRenderLite: (dataIndex, rowIndex) =>
              getAgentStatusComponent(data[dataIndex].agent),
          },
        },
        {
          label: "Service",
          name: "service",
          options: {
            customBodyRenderLite: (dataIndex, rowIndex) =>
              getServiceStatusComponent(data[dataIndex].service),
          },
        },
      ];
    }
  }
}

function NodesSelectToolbar({
  actionData,
  selectedRows,
  dataIdList,
  onClickHandler,
  isAdmin,
}) {
  const nodeIds = selectedRows.data.map((r) => dataIdList[r.dataIndex]);
  const actionButtons = actionData.map((a) => {
    return (
      <Tooltip key={a.actionKey} title={a.label}>
        <IconButton onClick={onClickHandler(a, nodeIds)} disabled={!isAdmin}>
          <Icon>{a.icon}</Icon>
        </IconButton>
      </Tooltip>
    );
  });

  return <div>{actionButtons}</div>;
}

function NodesToolbarExtension({ setSelected, dataIdList, isAdmin }) {
  const classes = useStyles();
  const [anchorEl, setAnchorEl] = useState(null);

  const handleClick = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };
  const open = Boolean(anchorEl);

  const nodeIdToDataId = {};
  for (const [i, e] of dataIdList.entries()) {
    nodeIdToDataId[e] = i;
  }

  const onSubmit = (values) => {
    const nodeIdList = getNodeIdListFromString(values.nodeIds);
    setSelected(nodeIdList.map((n) => nodeIdToDataId[n]));
  };

  const getNodeIdListFromString = (nodeIdsString) => {
    return nodeIdsString.split(",").map((s) => s.trim());
  };

  const validate = (values) => {
    if (values.nodeIds) {
      const nodeIdList = getNodeIdListFromString(values.nodeIds);
      for (const nodeId of nodeIdList) {
        if (!(nodeId in nodeIdToDataId)) {
          return { nodeIds: "Invalid nodeId " + nodeId };
        }
      }
    }
    return;
  };

  if (!isAdmin) {
    return <Box />;
  }

  return (
    <React.Fragment>
      <Tooltip title="Manually select nodes">
        <IconButton onClick={handleClick}>
          <Icon>list</Icon>
        </IconButton>
      </Tooltip>
      <Popover
        anchorOrigin={{
          vertical: "bottom",
          horizontal: "right",
        }}
        transformOrigin={{
          vertical: "top",
          horizontal: "right",
        }}
        open={open}
        anchorEl={anchorEl}
        onClose={handleClose}
      >
        <Box className={classes.popoverContent}>
          <Form
            onSubmit={onSubmit}
            validate={validate}
            render={({ handleSubmit, submitting }) => (
              <React.Fragment>
                <TextField
                  id="outlined-basic"
                  label="Comma separated node IDs"
                  variant="outlined"
                  name="nodeIds"
                />
                <Box textAlign="right">
                  <Button
                    onClick={handleSubmit}
                    color="primary"
                    disabled={submitting}
                  >
                    Select Nodes
                  </Button>
                </Box>
              </React.Fragment>
            )}
          />
        </Box>
      </Popover>
    </React.Fragment>
  );
}

export default connect(mapState, null)(Nodes);
