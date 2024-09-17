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
        Attribute brokersetMapAttr = cluster.getAttribute(KafkaClusterInfoSensor.ATTR_BROKERSET_KEY);
        if (brokersetMapAttr == null) {
            // TODO: Cluster has not brokerset. Return.
            return;
        }
        Map<String, Brokerset> brokersetMap = brokersetMapAttr.getValue();
        // BrokerId to brokerset alias set map
        Map<String, Set<String>> brokerToBrokersetsMap = new HashMap<>();
        for (String nodeId : cluster.getNodeMap().keySet()) {
            brokerToBrokersetsMap.put(nodeId, new HashSet<>());
        }
        for (Brokerset brokerset : brokersetMap.values()) {
            String brokersetAlias = brokerset.getBrokersetAlias();
            // brokerIds in this brokerset
            Set<String> brokerIds = new HashSet<>();
            List<Brokerset.BrokersetRange> brokersetRanges = brokerset.getEntries();
            if (brokersetRanges == null || brokersetRanges.isEmpty()) {
                // TODO: Brokerset has no brokerset range. Publish metrics - override internally?
                continue;
            }
            for (Brokerset.BrokersetRange brokersetRange : brokersetRanges) {
                int start = brokersetRange.getStartBrokerIdx();
                int end = brokersetRange.getEndBrokerIdx();
                for (int i = start; i <= end; i++) {
                    String nodeId = Integer.toString(i);
                    if (cluster.getNodeMap().containsKey(nodeId)) {
                        Node node = cluster.getNodeMap().get(nodeId);
                        brokerIds.add(node.getCurrentNodeInfo().getNodeId());
                        brokerToBrokersetsMap.get(nodeId).add(brokersetAlias);
                    } else {
                        // TODO: Brokerset contains invalid broker id.
                    }
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
