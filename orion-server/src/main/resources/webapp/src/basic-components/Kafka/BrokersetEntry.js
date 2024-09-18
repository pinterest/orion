import React from "react";
import {Tab, Tabs, Grid, Box, Link, Typography, Chip} from "@material-ui/core";
import { Link as RouterLink, Redirect, Route, Switch } from "react-router-dom";
import PropsTable from "../Commons/PropsTable";

const routes = [
    {
        subpath: "assignments",
        component: PropsTable,
        label: "Assignments",
        getData: getAssignmentData,
        getColumns: getAssignmentColumns,
    },
    {
        subpath: "brokers",
        component: PropsTable,
        label: "Brokers",
        getData: getBrokerData,
        getColumns: getBrokerColumns,
    },
    {
        subpath: "status",
        component: PropsTable,
        label: "Status",
        getData: getStatusData,
        getColumns: getStatusColumns,
    }
];

function getStatusData(clusterId, rawData) {
    let brokersetStatus = [];
    let brokersetData = rawData.brokersetData;
    brokersetStatus.push({ key: "Broker Count", value: brokersetData.size});
    return brokersetStatus;
}

function getStatusColumns() {
    return [
        { title: "Key", field: "key" },
        { title: "Value", field: "value" },
    ];
}

function brokerToLink(broker, clusterId) {
    return (
        <Link
            component={RouterLink}
            to={"/clusters/" + clusterId + "/nodes/" + broker}
        >
            {broker}
        </Link>
    );
}

function getBrokerData(clusterId, rawData) {
    let brokersetData = rawData.brokersetData;
    let brokers = [];
    for (let brokerId of brokersetData.brokerIds) {
        brokers.push({ broker: brokerToLink(brokerId, clusterId) });
    }
    return brokers;
}

function getBrokerColumns() {
    return [
        { title: "Broker", field: "broker" },
    ];
}

function getAssignmentData(clusterId, rawData) {
    let brokersetData = rawData.brokersetData;
    let assignments = [];
    for (let range of brokersetData.brokersetRanges) {
        assignments.push({
            startBrokerIdx: range[0],
            endBrokerIdx: range[1],
            size: range[1] - range[0] + 1
        });
    }
    return assignments;
}

function getAssignmentColumns() {
    return [
        { title: "Start Broker Id", field: "startBrokerIdx" },
        { title: "End Broker Id", field: "endBrokerIdx" },
        { title: "Size", field: "size" }
    ];
}

function BrokersetNavTabs(props) {
    return (
        <Tabs
            value={props.match.params.tab}
            style={{ backgroundColor: "white", width: "100%", maxWidth: "100%" }}
        >
            {routes.map((route, idx) => (
                <Tab
                    key={idx}
                    value={route.subpath}
                    label={route.label}
                    to={route.subpath}
                    component={RouterLink}
                />
            ))}
        </Tabs>
    );
}

function getBrokersetInfoHeader(rawData, clusterId) {
    let brokersetData = rawData.brokersetData;
    let brokersetAlias = brokersetData.brokersetAlias;
    let brokerCount = brokersetData.size;
    return (
        <Box my={2}>
            <Grid container display="flex" alignItems="center" spacing={2}>
                <Grid item>
                    <Typography variant="h6">
                        {brokersetAlias}
                    </Typography>
                </Grid>
                <Grid item>
                    <Chip variant="outlined" label="Kafka Brokerset" size="small" />
                </Grid>
                <Grid item>
                    <Chip
                        variant="outlined"
                        color="primary"
                        size="small"
                        label={brokerCount + " brokers"}
                    />
                </Grid>
            </Grid>
        </Box>
    );
}

export default function BrokersetEntry(props) {
    let rowData = props.rowData;
    let clusterId = props.clusterId;
    return (
        <div>
            {getBrokersetInfoHeader(rowData, clusterId)}
            <Grid>
                <Grid item xs={10}>
                    <Switch>
                        <Redirect
                            exact
                            from="/clusters/:clusterId/service/brokersets/:brokersetAlias"
                            to="/clusters/:clusterId/service/brokersets/:brokersetAlias/assignments"
                        ></Redirect>
                        <Route
                            path="/clusters/:clusterId/service/brokersets/:brokersetAlias/:tab"
                            children={BrokersetNavTabs}
                        />
                    </Switch>
                </Grid>
                <Grid item xs={12}>
                    <Switch>
                        {routes.map((route, idx) => {
                            return (
                                <Route
                                    key={idx}
                                    exact
                                    path={"/clusters/:clusterId/service/brokersets/:brokersetAlias/" + route.subpath}
                                >
                                {<route.component
                                    data={route.getData(clusterId, rowData)}
                                    columns={route.getColumns()}
                                />}
                                </Route>
                            );
                        })}
                    </Switch>
                </Grid>
            </Grid>
        </div>
    )
}
