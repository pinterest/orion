package com.pinterest.orion.core.automation.operator.kafka;

import com.pinterest.orion.core.Attribute;
import com.pinterest.orion.core.Node;
import com.pinterest.orion.core.automation.sensor.kafka.KafkaClusterInfoSensor;
import com.pinterest.orion.core.kafka.Brokerset;
import com.pinterest.orion.core.kafka.KafkaCluster;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class BrokersetNodeOperator extends KafkaOperator {
    private static final Logger logger = Logger
        .getLogger(BrokersetNodeOperator.class.getName());
    @Override
    public void operate(KafkaCluster cluster) throws Exception {
        // Add brokerset to broker and broker id to brokerset
        // Need removal logic as well
        // Add validation
        Attribute brokersetMapAttr = cluster.getAttribute(KafkaClusterInfoSensor.ATTR_BROKERSET_KEY);
        Map<String, Brokerset> brokersetMap = brokersetMapAttr.getValue();
        System.out.println("[TEST] brokersetMap: " + brokersetMap);
        System.out.println("[TEST] nodeMap: " + cluster.getNodeMap());
        Set<String> brokerIds = new HashSet<>();
        for (Brokerset brokerset : brokersetMap.values()) {
            String brokersetAlias = brokerset.getBrokersetAlias();
            List<Brokerset.BrokersetRange> brokersetRanges = brokerset.getEntries();
            for (Brokerset.BrokersetRange brokersetRange : brokersetRanges) {
                int start = brokersetRange.getStartBrokerIdx();
                int end = brokersetRange.getEndBrokerIdx();
                for (int i = start; i <= end; i++) {
                    Node node = cluster.getNodeMap().get(i);
                    if (node == null) {
                        System.out.println("[TEST] id: " + i + " is null");
                    } else {
                        brokerIds.add(node.getCurrentNodeInfo().getNodeId());
                        node.getCurrentNodeInfo().getBrokersets().add(brokersetAlias);
                    }
                }
            }
            System.out.println("[TEST] brokersetAlias: " + brokersetAlias);
            System.out.println("[TEST] brokersetRanges: " + brokersetRanges);
            System.out.println("[TEST] brokerIds: " + brokerIds);
            brokerset.setBrokerIds(brokerIds);
        }

    }

    @Override
    public String getName() {
        return "BrokersetNodeOperator";
    }
}
