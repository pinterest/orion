## Topic Management

We have developed a new way to manage Kafka Topics that allows scalable topic management. All Kafka topics are now expressed as JSON following the infrastructure as code pattern.
You can see an example Topic JSON on the right. Here's a description of the various parts.

For more details on how topic management works please see our presentation at [2020 Kafka Summit](https://www.confluent.io/resources/kafka-summit-2020/organic-growth-and-a-good-night-sleep-effective-kafka-operations-at-pinterest/)

| Field Name | Description |
|------------|-------------|
|topicName	|Name of your Kafka Topic, this MUST be unique for a given Kafka cluster. Multiple words in a topic name should be separated by `_` (underscore)|
|brokerset	|Brokersets are a virtualization framework to allow us to decouple topics from brokers so our clients do not need to be worried about balancing and skews of topics.|
|stride	|Allows leader skews to be automatically generated, this ensures that when there is a node (broker) failure in Kafka, the entire load of the broker doesn't tip over to the next node and is rather dispersed.|
|project|Name of the project, this helps us track the owners of a pipeline allowing chargeback and paging during incidents|
|config|Optional configurations for the Kafka topic. Typically most users specify retention.ms i.e. time based retention.|
|consumers|If you have any real-time consumers for your Kafka Topic then please specify details here as a JSON array.|

```
{
  "topicName": "test_kafka",
  "brokerset": "Static_B120_P120_0",
  "replicationFactor": 3,
  "stride": 0,
  "project": "xyz",
  "config": {
    "message.format.version": "1.1.0-IV0",
    "retention.ms": "86400000"
  }
}
```

### Brokersets
Brokersets are a virtualization framework to allow us to decouple topics from brokers so our clients do not need to be worried about balancing and skews of topics.

Static = static partition counts (supports specific use cases and legacy workloads)
Capacity = dynamic partition counts (supports organic horizontal scaling workloads)

A brokerset describes the capacity allocation, placement and partition count of a topic. e.g.
Unless otherwise specified we request that all customers should consider Capacity based design so you don't have to worry about scaling issues as your workload scales up.

Explanation:
Capacity_B120_P120_0: Capacity brokerset, 120 brokers, 120 partitions, 0 index (i.e. first brokerset of this capacity spec in the cluster)|

### Stride
Stride starts with 0 and goes up in increments of 1. The next stride number is usually 1+ the last topic stride on a given brokerset. 

Stride allows offseting assignments so when a given broker goes down there isn't a leader spike on the next broker with ideal balancer.

### Configuration (custom configuration)
Other configuration can be specified, please refer to Apache Kafka's Topic Configuration Section

### Consumers
Orion Kafka Topic Management setup also allows registration of consumers. This information can subsequently be used for automated actions, quota enforcement etc.

```
   [
      {
        "name": "my consumer",
        "project": "orion-customer-1",
        "clientLibrary": "kafka-client",
        "clientVersion": "2.0.0",
        "clientTechnology": "spark",
        "description": "this is a test consumer",
        "environment": "prod",
        "topics":[
          "test_kafka"
        ]
      }
   ]
```

| Field Name | Description |
|------------|-------------|
|name|The name of your consumer group, this MUST MATCH the group.id configuration of your Kafka consumer|
|project|Name of the project|
|clientLibrary|What client library (dependency artifact name) are you using e.g.Java client: kafka-client,Python: confluent-python-kafka,C++: librdkafka|
|clientVersion|Version of the client library / dependency e.g.Java: kafka-client, Version: 2.1.0|
|clientTechnology|What technology are you using the dependency in? Here are valid values:standalone-consumer, kafka-streams, spark, flink|
|description|In a few words describe what your consumer does|
|environment|prod/staging/canary/dev|
|topics|What topics is this consumer subscribing to|
