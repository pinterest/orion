# Generic Plugins

This page documents the currently available Generic plugins in Orion.

Generic plugins are service agnostic i.e. they are designed to work irrespective of which service is being managed via Orion e.g. rolling restart, rolling upgrade etc.

## Actions
|Name|Class|Type|Description|
|--------|-------|-------|------|
|Email|com.pinterest.orion.core.actions.alert.LocalhostEmailAlertAction|Alert|Send email based alerts to the configured addresses|
|PagerDuty|com.pinterest.orion.core.actions.alert.PagerDutyAlertAction|Alert|Creates a pager using v2 pager duty API|
|Slack|com.pinterest.orion.core.actions.alert.SlackAlertAction|Alert|Sends alert as a slack message in a configured channel, requires additional configs see Alert documentation for details|
|Start Cluster Service|com.pinterest.orion.core.actions.generic.GenericClusterWideAction.ParallelClusterStartAction|Cluster|Generic cluster Start|
|Stop Cluster Service|com.pinterest.orion.core.actions.generic.GenericClusterWideAction.ParallelClusterStopAction|Cluster|Generic cluster Stop|
|Enable Maintenance Mode|com.pinterest.orion.core.actions.generic.GenericClusterWideAction.ParallelEnableNodeMaintenanceModeAction|Cluster|Enable maintenance mode on specified nodes|
|Disable Maintenance Mode|com.pinterest.orion.core.actions.generic.GenericClusterWideAction.ParallelDisableNodeMaintenanceModeAction|Cluster|Disable maintenance mode on specified nodes|
|Rolling EC2 Replacement|com.pinterest.orion.core.actions.generic.GenericClusterWideAction.RollingReplacementAction|Cluster|Perform rolling replacement of EC2 nodes while ensuring service is stable|

## Operators
|Name|Class|Type|Description|
|--------|-------|-------|------|

## Sensors
|Name|Class|Description|
|--------|-------|------|
|Scripted Sensor|com.pinterest.orion.core.automation.sensor.ScriptedSensor|Allows JSR226 compliant scripting language like Javascript, Jython etc. to be used for writing sensors|