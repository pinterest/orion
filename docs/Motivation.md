# Motivation
Operations on stateful data systems are more complicated than their stateless counterparts due to the need for data replication & consistency. We needed to build a framework to manage our Kafka clusters in a safe and reliable manner while providing a control plane for our clusters.

Our existing framework Dr. Kafka lacks key features around the control plane therefore more advanced operations like Cluster Upgrades, Config Updates, Rebalancing have to be done manually using shell scripts by Engineers. We also run into several conflicting operations scenarios where the automation and human interaction collide causing stability problems for the cluster e.g. multiple node replacements, assignment corruption. Orion aims to address these gaps and provide a unified control plane and automation for stateful system operations (starting with Kafka) to reduce our KTLO costs.

We also want to eliminate inconsistency in the operations performed on our clusters by on-call engineers using this centralization approach. This eliminates the chances of human error and allows for all operations to be audited as well as secured.

Control Plane is also a generic problem that needs to be addressed by multiple teams in Data Engineering, involving solutions for common tasks like instance replacement, detection and remediation of slow nodes, configuration upgrades, upgrades etc. Therefore, there is a need for a generic service agnostic framework that allows addressing issues.

Note: While individual problems could arguably be addressed by the writing scripts, they unfortunately would not allow comprehensive resolution as these scripts have 0 situational awareness. E.g. degraded state of primary cluster should not impact standby cluster. This contextual check requires that a common engine be observing these clusters.
