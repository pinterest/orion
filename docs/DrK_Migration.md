# Migrating from [DoctorKafka](https://github.com/pinterest/doctorkafka) to Orion

If you were previously using DoctorKafka, here are the instructions for migrating from DoctorKafka to Orion.

Note: Currently only AWS EC2 replacements are supported. Auto-rebalancing isn't implemented in Orion yet since we have migrated to static rebalancing techniques which as they have higher stability in production.

## Requirements:
1. Orion-compatible agents need to be installed on the brokers and configured to communicate with the Orion Server
2. AWS IAM roles are configured correctly for the server machine

## Migration Overview

Dr. Kafka and Orion have different architecture, a migration in this case represents running Orion with configurations that will provide feature parity with Dr. Kafka.

Before configuring Orion with feature parity of Dr. Kafka please ensure to:

1. Read [quick start guide](QuickStart.md)
2. Read [kafka service overview](Kafka/README.md)
3. Deploy Orion Server and Agent as described in Quick Start guide
4. Update Orion Server configuration with the Dr. Kafka Parity Configuration (described below)
5. Restart Orion Server


```
NOTE: We highly recommend using the full stack of Orion Kafka service instead of limiting features to Dr. Kafka parity. The new capabilities are very useful for managing large Kafka deployments.
```

**Orion Dr. Kafka Parity Configuration**
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
