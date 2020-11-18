# Generic Plugins

This page documents the currently available Generic plugins in Orion.

Generic plugins are service agnostic i.e. they are designed to work irrespective of which service is being managed via Orion e.g. rolling restart, rolling upgrade etc.

## Actions

**Name: Email Alert**

Class: `com.pinterest.orion.core.actions.alert.LocalhostEmailAlert`

Description: Send email based alerts to the configured addresses


**Name: Slack Alert**

Class: `com.pinterest.orion.core.actions.alert.SlackAlert`

Description: Sends alert as a slack message in a configured channel


**Name: Start Cluster Service**

Class: `com.pinterest.orion.core.actions.generic.GenericClusterWideAction.ParallelClusterStartAction`

Description: Parallel cluster start

**Name: Stop Cluster Service**

Class: `com.pinterest.orion.core.actions.generic.GenericClusterWideAction.ParallelClusterStopAction`

Description: Parallel cluster stop


**Name: Enable Maintenance Mode**

Class: `com.pinterest.orion.core.actions.generic.GenericClusterWideAction.ParallelEnableNodeMaintenanceModeAction`

Description: Enable maintenance mode on specified nodes


**Name: Disable Maintenance Mode**

Class: `com.pinterest.orion.core.actions.generic.GenericClusterWideAction.ParallelDisableNodeMaintenanceModeAction`

Description: Disable maintenance mode on specified nodes


**Name: Rolling EC2 Replacement**

Class: om.pinterest.orion.core.actions.generic.GenericClusterWideAction.RollingReplacementAction

Description: Perform rolling replacement of EC2 nodes while ensuring service is stable

## Operators


## Sensors
**Name: Scripted Sensor**

Class: `com.pinterest.orion.core.automation.sensor.ScriptedSensor`

Description: Allows JSR226 compliant scripting language like Javascript, Jython etc. to be used for writing sensors
