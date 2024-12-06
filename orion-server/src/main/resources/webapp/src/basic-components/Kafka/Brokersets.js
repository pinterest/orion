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
import React, {Suspense, useState} from "react";
import MaterialTable from "material-table";
import {Backdrop, Box, Fade, Modal} from "@material-ui/core";
import {useHistory, useRouteMatch} from "react-router-dom";
import BrokersetEntry from "./BrokersetEntry";
import {makeStyles} from "@material-ui/core/styles";

const modalStyles = makeStyles(theme => ({
    modal: {
        display: "flex",
        alignItems: "center",
        justifyContent: "center"
    },
    paper: {
        backgroundColor: theme.palette.background.paper,
        border: "2px solid #000",
        maxHeight: "500px",
        boxShadow: theme.shadows[5],
        padding: theme.spacing(2, 4, 3)
    }
}));

export default function Brokersets(props) {
    let brokersets = [];
    if (props.cluster.attributes.brokersetState) {
        brokersets = Object.values(props.cluster.attributes.brokersetState);
    }
    let columns = [
        { title: "Name", field: "brokersetAlias" },
        { title: "Broker Count", field: "brokerCount" },
        { title: "Max CPU Usage", field: "maxCpuUsage" },
        { title: "Max Disk Usage", field: "maxDiskUsage" },
        { title: "Updated Time", field: "timestamp" }
    ]
    let clusterId = props.cluster.clusterId;
    let brokersetToRowValuesMap = {};
    for (let brokerset of brokersets) {
        let brokersetAlias = brokerset.brokersetAlias;
        let maxCpuUsage = "N/A";
        let maxDiskUsage = "N/A";
        let timestamp = "N/A"
        if (brokerset.brokersetStatus) {
            if (brokerset.brokersetStatus["CPU_Usage_All_Brokers_Max"] !== undefined) {
                maxCpuUsage = brokerset.brokersetStatus["CPU_Usage_All_Brokers_Max"];
            }
            if (brokerset.brokersetStatus["Disk_Usage_All_Brokers_Max"] !== undefined) {
                maxDiskUsage = brokerset.brokersetStatus["Disk_Usage_All_Brokers_Max"];
            }
            if (brokerset.brokersetStatus["Short_Timestamp"] !== undefined) {
                timestamp = brokerset.brokersetStatus["Short_Timestamp"];
            }
        }
        brokersetToRowValuesMap[brokersetAlias] = {
            "brokersetAlias": brokersetAlias,
            "clusterId": clusterId,
            "brokerCount": brokerset.size,
            "brokersetData": brokerset,
            "maxCpuUsage": maxCpuUsage,
            "maxDiskUsage": maxDiskUsage,
            "timestamp": timestamp
        }
    }

    let data = Object.values(brokersetToRowValuesMap);

    const classes = modalStyles();
    const history = useHistory();
    let match = useRouteMatch("/clusters/:clusterId/service/brokersets/:brokersetAlias?");

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
        history.push("/clusters/" + clusterId + "/service/brokersets")
    }

    if (selectedRow != null && !openDetailsModal) {
        setOpenDetailsModal(true);
    }

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
                    timeout: 500
                }}
            >
                <Fade in={openDetailsModal}>
                    <div
                        style={{
                            backgroundColor: "white",
                            padding: "20px",
                            width: "1000px"
                        }}
                    >
                        {selectedRow && (
                            <Suspense fallback={<div>Loading...</div>}>
                                <BrokersetEntry
                                    rowData={selectedRow}
                                    clusterId={clusterId}
                                />
                            </Suspense>
                        )}
                    </div>
                </Fade>
            </Modal>
            <MaterialTable
                options={{ pageSize: 10, grouping: true, filtering: false }}
                title={""}
                onRowClick={(event, rowData, togglePanel) => {
                    history.push("/clusters/" + clusterId + "/service/brokersets/" + rowData.brokersetAlias);
                    setSelectedRow(rowData);
                    setOpenDetailsModal(true);
                }}
                columns={columns}
                data={data}
            />
        </Box>
    )
}
