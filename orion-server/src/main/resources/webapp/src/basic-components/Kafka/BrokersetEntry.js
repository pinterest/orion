import React from "react";
import {Tab, Tabs, Grid, Box, Link} from "@material-ui/core";
import { Link as RouterLink, Redirect, Route, Switch } from "react-router-dom";
import PropsTable from "../Commons/PropsTable";

const routes = [
    // {
    //     subpath: "status",
    //     component: PropsTable,
    //     label: "Status",
    //     getData: getStatusData,
    //     getColumns: getStatusColumns,
    // },
    {
        subpath: "brokers",
        component: PropsTable,
        label: "Brokers",
        getData: getBrokerData,
        getColumns: getBrokerColumns,
    },
    // {
    //     subpath: "assignments",
    //     component: PropsTable,
    //     label: "Assignments",
    //     getData: getAssignmentData,
    //     getColumns: getAssignmentColumns,
    // },
];

function getStatusData(clusterId, rawData) {
    let brokersetStatus = [];
    brokersetStatus.push({ key: "Last Update Time", value: "2024-09-01 00:00:00" });
    brokersetStatus.push({ key: "Broker Count", value: "4" });
    brokersetStatus.push({ key: "Max CPU usage", value: "20%" });
    brokersetStatus.push({ key: "Min CPU usage", value: "10%" });
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
    let rawDataStr = JSON.stringify(rawData);
    console.log("[DEBUG-brokersetEntry]" + rawDataStr)
    let brokers = [];
    // for (let broker of rawData.brokerIds) {
    //     brokers.push({ broker: brokerToLink(broker, clusterId) });
    // }
    return brokers;
}

function getBrokerColumns() {
    return [
        { title: "Broker", field: "broker" },
    ];
}

function getAssignmentData(clusterId, rawData) {
    let assignments = [];
    assignments.push({ startId: "1", endId: "2" });
    assignments.push({ startId: "4", endId: "5" });
    return assignments;
}

function getAssignmentColumns() {
    return [
        { title: "Start Broker ID", field: "startId" },
        { title: "End Broker ID", field: "endId" },
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


export default function BrokersetEntry(props) {
    return (
        <div>
            {/*Add brokerset header*/}
            <Grid>
                <Grid item xs={10}>
                    <Switch>
                        <Redirect
                            exact
                            from="/clusters/:clusterId/service/brokersets/:brokersetName"
                            to="/clusters/:clusterId/service/brokersets/:brokersetName/status"
                        ></Redirect>
                        <Route
                            path="/clusters/:clusterId/service/brokersets/:brokersetName/:tab"
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
                                    path={"/clusters/:clusterId/service/brokersets/:brokersetName/" + route.subpath}
                                >
                                {<route.component
                                    data={route.getData(props.clusterId, props.rowData)}
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
