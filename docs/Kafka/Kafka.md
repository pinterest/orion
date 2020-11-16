# Orion Kafka Implementation

### Actions
|Name|Class|Type|Description|
|--------|-------|-------|------|
|Create Kafka Topic|com.pinterest.orion.core.actions.kafka.SimpleCreateKafkaTopicAction|Cluster|Creates a Kafka Topic with supplied attributes|
|Partition Reassignment|com.pinterest.orion.core.actions.kafka.ReassignmentAction|Cluster|Performs Topic Partition reassignment|
|Preferred Leader Election|com.pinterest.orion.core.actions.kafka.PreferredLeaderElectionAction|Cluster|Runs preferred leader(replica) election on the cluster|
|MinIsrRfConflictResolutionAction|com.pinterest.orion.core.actions.kafka.MinIsrRfConflictResolutionAction|Cluster|Perform alerting for topics with misconfigured min ISR configs|
|Update Kafka Topic Configuration|com.pinterest.orion.core.actions.kafka.KafkaTopicConfigUpdateAction|Cluster|Update topic configs|
|StuckConsumerGroupRecovery|com.pinterest.orion.core.actions.kafka.KafkaStuckConsumerGroupRecoveryAction|Cluster|Performs leader swap on consumer offset topic to force a coordinator swap|
|Ideal Balance Topic|com.pinterest.orion.core.actions.kafka.KafkaIdealBalanceAction|Cluster|Perform ideal balancing of the Topic using Pinterest's balancing techniques|
|Restart|com.pinterest.orion.core.actions.kafka.KafkaClusterRollingRestart|Cluster|Facade Action to trigger Kafka Broker Restart|
|Decommission|com.pinterest.orion.core.actions.kafka.KafkaClusterDecommissionBrokers|Cluster|Performs decommissioning of brokers using EC2BrokerDecommissionAction|
|EC2BrokerDecommissionAction|com.pinterest.orion.core.actions.kafka.EC2BrokerDecommissionAction|Node|Decommissions a Broker on EC2 if the broker has no Topic Partitions assigned to it|
|Restart|com.pinterest.orion.core.actions.kafka.ConcurrentKafkaRestartAction|Cluster|Restart Kafka Cluster using graph based concurrent restarts|
|Expand Kafka Topic|com.pinterest.orion.core.actions.kafka.AssignmentExpandKafkaTopicAction|Cluster|Action used to expand partitions for a given Kafka Topic|
|Create Kafka Topic|com.pinterest.orion.core.actions.kafka.AssignmentCreateKafkaTopicAction|Cluster|Create Kafka topic based on specific assignment JSON|

### Sensors
|Name|Class|Description|
|--------|-------|-------|
|KafkaBrokerConfigSensor|com.pinterest.orion.core.automation.sensor.kafka.KafkaBrokerConfigSensor|Fetches current Kafka broker configurations using Kafka Admin Client|
|KafkaBrokerSensor|com.pinterest.orion.core.automation.sensor.kafka.KafkaBrokerSensor|Fetches Kafka broker metadata|
|BrokersetAssignmentSensor|com.pinterest.orion.core.automation.sensor.kafka.KafkaClusterInfoSensor|Loads information from disk on brokersets, topics assignments etc.|
|KafkaConsumerGroupDescriptionSensor|com.pinterest.orion.core.automation.sensor.kafka.KafkaConsumerGroupDescriptionSensor|Pulls information on Kafka Consumer Groups|
|KafkaConsumerGroupOffsetSensor|com.pinterest.orion.core.automation.sensor.kafka.KafkaConsumerGroupOffsetSensor|Pull latest consumer group offsets and calculates offset lag for each CG|
|KafkaLogDirSensor|com.pinterest.orion.core.automation.sensor.kafka.KafkaLogDirectorySensor|Pulls log size on disk information|
|StuckConsumerGroupSensor|com.pinterest.orion.core.automation.sensor.kafka.KafkaStuckConsumerGroupSensor|Calculates if Consumer Groups are in stuck state|
|KafkaTopicOffsetSensor|com.pinterest.orion.core.automation.sensor.kafka.KafkaTopicOffsetSensor|Pulls the topic partition offsets|
|KafkaTopicSensor|com.pinterest.orion.core.automation.sensor.kafka.KafkaTopicSensor|Pulls information of Kafka Topic Partitions for a given cluster|


### Operators
|Name|Class|Description|
|--------|-------|-------|
||||