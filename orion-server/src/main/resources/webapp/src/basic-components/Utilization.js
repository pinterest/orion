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
import React from "react";
import { Typography, Box, Grid } from "@material-ui/core";
import { connect } from "react-redux";
import { requestUtilization, requestCost } from "../actions/cluster";
import Chart from "react-apexcharts";
import PriorityQueue from "javascript-priority-queue";

const mapState = (state, ownProps) => {
  const { utilization, cost } = state.app;
  return {
    ...ownProps,
    utilization,
    cost,
  };
};

const mapDispatch = {
  requestUtilization,
  requestCost,
};

function Utilization({ utilization, requestUtilization, cost, requestCost }) {
  React.useEffect(() => {
    requestUtilization();
    requestCost();
  }, [requestUtilization, requestCost]);
  if (!utilization) {
    return (
      <Box>
        <Typography>Utilization not available yet.</Typography>
      </Box>
    );
  } else {
    var costCharts = buildCostCharts(cost);
    return (
      <Box>
        <Grid container spacing={2}>
          <Grid item>
            <Box>
              <Typography>Resources</Typography>
              {buildUtilizationCharts(utilization)}
            </Box>
          </Grid>
          <Grid item>
            <Box>
              <Typography>Cost</Typography>
              {costCharts}
            </Box>
          </Grid>
        </Grid>
      </Box>
    );
  }
}

export default connect(mapState, mapDispatch)(Utilization);

function buildCostCharts(cost) {
  if (!cost || Object.keys(cost).length == 0) {
    return (
      <Box>
        <Typography>Cost not available.</Typography>
      </Box>
    );
  } else {
    const topicCost = {
      options: {
        labels: [],
        plotOptions: {
          pie: {
            donut: {
              labels: {
                show: true,
                total: {
                  show: true,
                  showAlways: true,
                  label: "Topics($)",
                  color: "#373d3f",
                  formatter: function (w) {
                    return w.globals.seriesTotals
                      .reduce((a, b) => {
                        return a + b;
                      }, 0)
                      .toFixed(2);
                  },
                },
              },
            },
          },
        },
      },
      series: [],
    };
    const clusterCost = {
      options: {
        labels: [],
        plotOptions: {
          pie: {
            donut: {
              labels: {
                show: true,
                total: {
                  show: true,
                  showAlways: true,
                  label: "Total($)",
                  color: "#373d3f",
                  formatter: function (w) {
                    return w.globals.seriesTotals
                      .reduce((a, b) => {
                        return a + b;
                      }, 0)
                      .toFixed(2);
                  },
                },
              },
            },
          },
        },
      },
      series: [],
    };
    const maxQueue = new PriorityQueue("max");
    try {
      for (let [clustername, cluster] of Object.entries(cost)) {
        var networkCost = 0;
        var topicNames = Object.keys(cluster.entityCostMap);
        for (var topicName in topicNames) {
          var topic = cluster.entityCostMap[topicNames[topicName]];
          networkCost += topic.networkCost;
          var topicCostTmp = topic.networkCost + topic.nodeCost;
          if (topicCostTmp > 0) {
            maxQueue.enqueue(
              {
                topicName: topicNames[topicName],
                topic: topic,
              },
              topicCostTmp
            );
          }
        }
        var totalCost = networkCost + cluster.nodeCost;
        if (totalCost > 0) {
          clusterCost.series.push(totalCost);
          clusterCost.options.labels.push(clustername);
        }
      }

      var max = 20;
      while (max > 0 && maxQueue.size() > 0) {
        var topicEntry = maxQueue.dequeue();
        var topic = topicEntry.topic;
        var cost = topic.networkCost + topic.nodeCost;
        topicCost.series.push(cost);
        topicCost.options.labels.push(topicEntry.topicName.substring(0, 15));
        max--;
      }
    } catch (err) {
      console.log(err);
    }
    return (
      <Grid container spacing={2}>
        <Grid item>
          <Chart
            options={clusterCost.options}
            series={clusterCost.series}
            type="donut"
            width="500"
          />
        </Grid>
        <Grid item>
          <Chart
            options={topicCost.options}
            series={topicCost.series}
            type="donut"
            width="500"
          />
        </Grid>
      </Grid>
    );
  }
}

function buildUtilizationCharts(utilization) {
  const networkChart = {
    options: {
      labels: [],
      plotOptions: {
        pie: {
          donut: {
            labels: {
              show: true,
              total: {
                show: true,
                showAlways: true,
                label: "Network In(MB/s)",
                color: "#373d3f",
                formatter: function (w) {
                  return w.globals.seriesTotals
                    .reduce((a, b) => {
                      return a + b;
                    }, 0)
                    .toFixed(2);
                },
              },
            },
          },
        },
      },
    },
    series: [],
  };
  const diskChart = {
    options: {
      labels: [],
      plotOptions: {
        pie: {
          donut: {
            labels: {
              show: true,
              total: {
                show: true,
                showAlways: true,
                label: "Disk(TB)",
                color: "#373d3f",
                formatter: function (w) {
                  return w.globals.seriesTotals
                    .reduce((a, b) => {
                      return a + b;
                    }, 0)
                    .toFixed(2);
                },
              },
            },
          },
        },
      },
    },
    series: [],
  };
  for (let [clustername, cluster] of Object.entries(utilization)) {
    var totalNetworkMBPerSecond = 0;
    var totalDiskInTB = 0;
    var topicNames = Object.keys(cluster);
    for (var topicName in topicNames) {
      var topic = cluster[topicNames[topicName]];
      totalNetworkMBPerSecond += topic.networkUtilizationInMBPerSecond;
      totalDiskInTB += topic.diskUtilizationInMB / 1024 / 1024;
    }
    networkChart.series.push(totalNetworkMBPerSecond);
    networkChart.options.labels.push(clustername);

    diskChart.series.push(totalDiskInTB);
    diskChart.options.labels.push(clustername);
  }
  return (
    <Grid container spacing={2}>
      <Grid item>
        <Chart
          options={networkChart.options}
          series={networkChart.series}
          type="donut"
          width="500"
        />
      </Grid>
      <Grid item>
        <Chart
          options={diskChart.options}
          series={diskChart.series}
          type="donut"
          width="500"
        />
      </Grid>
    </Grid>
  );
}
