package com.pinterest.orion.core.automation.sensor.kafka;

import com.pinterest.orion.core.kafka.KafkaCluster;

public class BrokerMetricsSensor extends KafkaSensor {

    @Override
    public String getName() {
        return "BrokerMetricsSensor";
    }

    @Override
    public void sense(KafkaCluster cluster) throws Exception {
    }
}
