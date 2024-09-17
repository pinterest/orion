package com.pinterest.orion.core.automation.operator.kafka;

import com.pinterest.orion.core.Attribute;
import com.pinterest.orion.core.Node;
import com.pinterest.orion.core.automation.sensor.kafka.KafkaClusterInfoSensor;
import com.pinterest.orion.core.kafka.Brokerset;
import com.pinterest.orion.core.kafka.KafkaCluster;

import java.util.HashMap;
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
        // Just initialize with all nodes
        Attribute brokersetMapAttr = cluster.getAttribute(KafkaClusterInfoSensor.ATTR_BROKERSET_KEY);
        if (brokersetMapAttr == null) {
            System.out.println("[TEST] brokersetMapAttr is null for cluster: " + cluster.getName());
            return;
        }
        Map<String, Brokerset> brokersetMap = brokersetMapAttr.getValue();
        // Broker -> brokersets map
        Map<String, Set<String>> brokerToBrokersetsMap = new HashMap<>();
        for (String nodeId : cluster.getNodeMap().keySet()) {
            brokerToBrokersetsMap.put(nodeId, new HashSet<>());
        }

        for (Brokerset brokerset : brokersetMap.values()) {
            // brokerIds in this brokerset
            Set<String> brokerIds = new HashSet<>();
            String brokersetAlias = brokerset.getBrokersetAlias();
            List<Brokerset.BrokersetRange> brokersetRanges = brokerset.getEntries();
            if (brokersetRanges == null || brokersetRanges.isEmpty()) {
                System.out.println(
                    String.format("[TEST] brokersetRanges is null or empty for brokerset: %s in cluster: %s",
                        brokersetAlias, cluster.getName()));
                continue;
            }
            for (Brokerset.BrokersetRange brokersetRange : brokersetRanges) {
                int start = brokersetRange.getStartBrokerIdx();
                int end = brokersetRange.getEndBrokerIdx();
                for (int i = start; i <= end; i++) {
                    String nodeId = Integer.toString(i);
                    Node node = cluster.getNodeMap().get(nodeId);
                    if (node == null || node.getCurrentNodeInfo() == null) {
                        System.out.println(
                            String.format(
                                "[TEST] node is null for nodeId: %s in cluster: %s",
                                nodeId, cluster.getName()));
                        continue;
                    }
                    brokerIds.add(node.getCurrentNodeInfo().getNodeId());
                    brokerToBrokersetsMap.get(nodeId).add(brokersetAlias);
                }
            }
            brokerset.setBrokerIds(brokerIds);
        }
        for (Node node : cluster.getNodeMap().values()) {
            if (node != null && node.getCurrentNodeInfo() != null) {
                String nodeId = node.getCurrentNodeInfo().getNodeId();
                node.getCurrentNodeInfo().setBrokersets(brokerToBrokersetsMap.get(nodeId));
            }
        }
        System.out.println(String.format("[TEST1] Cluster: %s brokerToBrokersetsMap: %s",
            cluster.getName(), brokerToBrokersetsMap));
    }

    @Override
    public String getName() {
        return "BrokersetNodeOperator";
    }
}
