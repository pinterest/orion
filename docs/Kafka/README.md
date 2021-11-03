# Orion Kafka Service
Orion is capable of managing multiple clusters. We have created an Orion implementation for Kafka which includes plugins. We use it to mange 10s of Kafka clusters containing 1000s of nodes.

The following Actions, Sensors and Operators are created specifically for managing Kafka.


### Actions

**Create Kafka Topic**

Class: `com.pinterest.orion.core.actions.kafka.SimpleCreateKafkaTopicAction`

Description: Creates a Kafka Topic with supplied attributes


**Delete Kafka Topic**

Class: `com.pinterest.orion.core.actions.kafka.AssignmentDeleteKafkaTopicAction`

Description: Delete the specified kafka topic if it includes the field `"delete": true`
AND the server description includes 
```     
plugins:
  operatorConfigs:
    - key: brokersetTopicOperator
      enabled: true
      configuration:
        enableTopicDeletion: true
```
Under the individual cluster config sections

**Partition Reassignment**

Class: `com.pinterest.orion.core.actions.kafka.ReassignmentAction`

Description: Performs Topic Partition reassignment


**Preferred Leader Election**

Class: `com.pinterest.orion.core.actions.kafka.PreferredLeaderElectionAction`

Description: Runs preferred leader(replica) election on the cluster


**MinIsrRfConflictResolutionAction**

Class: `com.pinterest.orion.core.actions.kafka.MinIsrRfConflictResolutionAction`

Description: Perform alerting for topics with misconfigured min ISR configs



**Update Kafka Topic Configuration**

Class: `com.pinterest.orion.core.actions.kafka.KafkaTopicConfigUpdateAction`

Description: Update topic configs if it differs from the ideal config


**StuckConsumerGroupRecovery**

Class: `com.pinterest.orion.core.actions.kafka.KafkaStuckConsumerGroupRecoveryAction`

Description: Performs leader swap on consumer offset topic to force a coordinator swap



**Ideal Balance Topic**

Class: `com.pinterest.orion.core.actions.kafka.KafkaIdealBalanceAction`

Description: Perform ideal balancing of the Topic using Pinterest's balancing techniques



**Restart Broker**

Class: `com.pinterest.orion.core.actions.kafka.KafkaClusterRollingRestart`

Description: Facade Action to trigger Kafka Broker Restart



**Decommission**

Class: `com.pinterest.orion.core.actions.kafka.KafkaClusterDecommissionBrokers`

Description: Performs decommissioning of brokers using EC2BrokerDecommissionAction



**EC2BrokerDecommissionAction**

Class: `com.pinterest.orion.core.actions.kafka.EC2BrokerDecommissionAction`

Description: Decommissions a Broker on EC2 if the broker has no Topic Partitions assigned to it



**Concurrent Restart**

Class: `com.pinterest.orion.core.actions.kafka.ConcurrentKafkaRestartAction`

Description: Restart Kafka Cluster using graph based concurrent restarts



**Expand Kafka Topic**

Class: `com.pinterest.orion.core.actions.kafka.AssignmentExpandKafkaTopicAction`

Description: Action used to expand partitions for a given Kafka Topic



**Create Kafka Topic**

Class: `com.pinterest.orion.core.actions.kafka.AssignmentCreateKafkaTopicAction`

Description: Create Kafka topic based on specific assignment JSON



### Sensors

**KafkaBrokerConfigSensor**

Class: `com.pinterest.orion.core.automation.sensor.kafka.KafkaBrokerConfigSensor`

Description: Fetches current Kafka broker configurations using Kafka Admin Client


**KafkaBrokerSensor**

Class: `com.pinterest.orion.core.automation.sensor.kafka.KafkaBrokerSensor`

Description: Fetches Kafka broker metadata

**BrokersetAssignmentSensor**

Class: `com.pinterest.orion.core.automation.sensor.kafka.KafkaClusterInfoSensor`

Description: Loads information from disk on brokersets, topics assignments etc. Please see more details on Topic Management [here](Topics.md)

**KafkaConsumerGroupDescriptionSensor**

Class: `com.pinterest.orion.core.automation.sensor.kafka.KafkaConsumerGroupDescriptionSensor`

Description: Pulls information on Kafka Consumer Groups

**KafkaConsumerGroupOffsetSensor**

Class: `com.pinterest.orion.core.automation.sensor.kafka.KafkaConsumerGroupOffsetSensor`

Description: Pull latest consumer group offsets and calculates offset lag for each CG

**KafkaLogDirSensor**

Class: `com.pinterest.orion.core.automation.sensor.kafka.KafkaLogDirectorySensor`

Description: Pulls log size on disk information

**StuckConsumerGroupSensor**

Class: `com.pinterest.orion.core.automation.sensor.kafka.KafkaStuckConsumerGroupSensor`

Description: Calculates if Consumer Groups are in stuck state

**KafkaTopicOffsetSensor**

Class: `com.pinterest.orion.core.automation.sensor.kafka.KafkaTopicOffsetSensor`

Description: Pulls the topic partition offsets

**KafkaTopicSensor**

Class: `com.pinterest.orion.core.automation.sensor.kafka.KafkaTopicSensor`

Description: Pulls information of Kafka Topic Partitions for a given cluster


### Operators

**Broker Healing Operator**

Class: `com.pinterest.orion.core.automation.operator.kafka.BrokerHealingOperator`

Description: Remediates Kafka Brokers based on heartbeats and data from Kafka

**BrokersetTopicOperator**

Class: `com.pinterest.orion.core.automation.operator.kafka.BrokersetTopicOperator`

Description: Create/Update topics assignments based on config files

**ConfigConflictOperator**

Class: `com.pinterest.orion.core.automation.operator.kafka.ConfigConflictOperator`

Description: Resolve conflicts between topic configurations, currently reconciles `min.insync.replicas` if it drops below the actual ISR of a topic

**TopicConfigOperator**

Class: `com.pinterest.orion.core.automation.operator.kafka.KafkaTopicConfigOperator`

Description: Update configurations of topics from config files.

**StuckConsumerGroupOperator**

Class: `com.pinterest.orion.core.automation.operator.kafka.StuckConsumerGroupOperator`

Description: This was written to mitigate an issue in Kafka 2.3.1 where consumer groups would occasionally get stuck in rebalancing state. It is now used to fix any similar issues by bumping the consumer group coordinator, forcing the rebalance to happen
