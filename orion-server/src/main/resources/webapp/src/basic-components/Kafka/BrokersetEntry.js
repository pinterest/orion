import React from "react";
import { Tab, Tabs, Grid } from "@material-ui/core";
import { Link as RouterLink, Redirect, Route, Switch } from "react-router-dom";
import PropsTable from "../Commons/PropsTable";

const routes = [
    {
        subpath: "status",
        component: PropsTable,
        label: "Status",
        getData: getStatusData,
        getColumns: getStatusColumns,
    },
    {
        subpath: "brokers",
        component: PropsTable,
        label: "Brokers",
        getData: getBrokerData,
        getColumns: getBrokerColumns,
    },
    {
        subpath: "assignments",
        component: PropsTable,
        label: "Assignments",
        getData: getAssignmentData,
        getColumns: getAssignmentColumns,
    },
];

function getStatusData(clusterId, rawData) {
    let brokersetStatus = [];
    brokersetStatus.push({ key: "Last Update Time", value: "2024-09-01 00:00:00" });
    brokersetStatus.push({ key: "Max CPU usage", value: "20%" });
    return brokersetStatus;
}

function getStatusColumns() {
    return [
        { title: "Key", field: "key" },
        { title: "Value", field: "value" },
    ];
}

function getBrokerData(clusterId, rawData) {
    let brokers = [];
    brokers.push({ brokerName: "Broker 0", cpuUsage: "10%", diskUsage: "20%", lastUpdated: "2024-09-01 00:00:00" });
    brokers.push({ brokerName: "Broker 1", cpuUsage: "10%", diskUsage: "20%", lastUpdated: "2024-09-01 00:00:00" });
    return brokers;
}

function getBrokerColumns() {
    return [
        { title: "Broker", field: "brokerName" },
        { title: "CPU Usage", field: "cpuUsage" },
        { title: "Disk Usage", field: "diskUsage" },
        { title: "Last Updated", field: "lastUpdated" },
    ];
}

function getAssignmentData(clusterId, rawData) {
    let assignments = [];
    assignments.push({ startIndex: "001", endIndex: "002" });
    assignments.push({ startIndex: "004", endIndex: "005" });
    return assignments;
}

function getAssignmentColumns() {
    return [
        { title: "Start Index", field: "startIndex" },
        { title: "End Index", field: "endIndex" },
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
                            from="/clusters/:clusterId/service/testbrokersets/:brokersetName"
                            to="/clusters/:clusterId/service/testbrokersets/:brokersetName/status"
                        ></Redirect>
                        <Route
                            path="/clusters/:clusterId/service/testbrokersets/:brokersetName/:tab"
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
                                    path={"/clusters/:clusterId/service/testbrokersets/:brokersetName/" + route.subpath}
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
