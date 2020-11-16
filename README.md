# Orion
Orion is a generalized plugable management and automation platform for stateful sistributed systems. Orion provides a unified interface of one or more clusters to both human and machine operators. Orion is capable of efficiently handling thousands of nodes spread across of 10s of clusters. 

Our intent is to use automation to handle commonly encountered operations issues and build a library of learnings from experiences of various large scale environments like ours.

### Problem
Orion aims to address the following problems:
- **Lack of single console to manage large clusters of Stateful Distributed Systems**: Present open source tooling lacks ability to manage 10s of clusters and 1000s of nodes simultaneously, requiring engineers to switch between multiple consoles and perform manual correlations.


- **Conflicts between human and automation operations**: When automation scripts are implemented they mostly lack the visibility into out of band human operations and vice-a-versa as a single control surface is missing for machine and human operations this has proven to cause catastrophic failures e.g. engineer manually restarts process but automation system thinks that the node is down and replaces it causing stability issues in the cluster.


- **Missing generalized community learnings on operations**: There currently is a lack of a generic unified interface and "store" for sharing community learnings for operations of various systems this leads to various companies having to rewrite remediations of problems. e.g. topic rebalancing in Kafka, auto replacement of slow HDFS nodes, concurrent rolling upgrades etc.


- **Missing ability for sensor fusion for automation**: Lack of ability to fuse information from multiple sensors to find root cause of an issue on cluster to make best decisions for automated remediations. e.g. finding if Kafka replica lag is due to a slow leader or a slow follower or due to increased client load or due to faulty readings for replica lag.

## Key Features

- **Unified Interface**: Orion provides a unified interface to implement human and automation actions along with a barrier to prevent conflicts


- **Library of Remediations**: Orion comes with a pre-defined set of common Sensors, Operators and Actions and interfaces to define new ones to build on-top of learnings


- **Scalable**: Ability to handle thousands of nodes and 10s of clusters efficiently


- **Plugable**: Orion was developed with the understanding that not all distributed systems behave the same way, pluggability, extensibility and abstraction is at the very core of Orion

## Current State

Orion currently support management of the following systems:
- Kafka


## Usage
Orion allows implementations of user defined **Action**s which are made available via both UI and as well as automated **Operator**s which allows engineers to program automated remediation of issues based on information from **Sensor**s in a large environment.

Example:
- Safely Replace Nodes in a Cloud Environment
- Concurrent Rolling Restart
- Concurrent Rolling Upgrade
- Execution of custom workflows like Kafka topic rebalancing
- Maintain settings e.g. monitor and fix topic configurations in Kafka

## Architecture
![Image of Orion's Architecture](images/Arch1.png)


## Quick Start

**Build**

```
git clone https://github.com/pinterest/orion.git
cd orion
bash ./builds/build-deployment.sh
# Agent artifact will be generated in deployments/orion-agent-deployment/target/orion-agent_x.x.x_all.deb
# Server artifact will be generated in deployments/orion-server-deployment/target/orion-server-deployment-x.x.x-bin.tar.gz
# Note: Agent jar is generated as well if you would like to use custom scripts
```

Build scripts for Ubuntu are provided. The build script will attempt to install pre-requisites. If they don't work for your environment the following are needed to build Orion:

- OpenJDK8
- nodejs12/npm
- Maven 3+


### Dr.Kafka to Orion Migration
If you were previously using Dr.Kafka you can find instructions on migrating to Orion here

## Maintainers

- Ping-Ming Lin
- Ambud Sharma

## Contributors

- Vahid Hashemian
- Jeff Xiang

## License
Orion is distributed under Apache License, Version 2.0.