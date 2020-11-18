# Migrating from [DoctorKafka](https://github.com/pinterest/doctorkafka) to Orion

If you were previously using DoctorKafka, here are the instructions for migrating from DoctorKafka to Orion.

Note: Currently only AWS EC2 replacements are supported. Auto-rebalancing isn't implemented in Orion yet since we have migrated to static rebalancing techniques which as they have higher stability in production.

## Requirements:
1. Orion-compatible agents need to be installed on the brokers and configured to communicate with the Orion Server
2. AWS IAM roles are configured correctly for the server machine

## Simple Example Configuration:
```yaml
server:
  applicationConnectors:
    - type: http
      port: 8443
  adminConnectors:
    - type: http
      port: 8444

clusterConfigs:
  - clusterId: test01
    type: kafka
    configuration:
      serversetPath: </path/to/your.serverset>
  - clusterId: test02
    type: kafka
    plugins:
        actionConfigs:
          - key: awsec2replacement
            configuration:
              dry_run: true # will run replacements in dry-run mode
    configuration:
      serversetPath: </path/to/your.serverset>

plugins:
  sensorConfigs:
    - key: brokerSensor
      class: com.pinterest.orion.core.automation.sensor.kafka.KafkaBrokerSensor
      interval: 60
      enabled: true

  operatorConfigs:
    - key: brokerHealingOperator
      class: com.pinterest.orion.core.automation.operator.kafka.BrokerHealingOperator
      enabled: true
      configuration:
        deadBrokerThresholdSeconds: <Time needed to declare a broker is dead> # default value 5 minutes

  actionConfigs:
    - key: brokerrecovery
      class: com.pinterest.orion.core.actions.kafka.BrokerRecoveryAction
      enabled: true
      configuration:
        dry_run: false
    - key: awsec2replacement
      class: com.pinterest.orion.core.actions.aws.ReplaceEC2InstanceAction
      enabled: true
      configuration:
        zoneId: <AWS EC2 Route 53 Zone ID>
        name: <AWS EC2 Route 53 Zone Name>
```