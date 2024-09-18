package com.pinterest.orion.core.automation.sensor.kafka;

import com.pinterest.orion.core.Attribute;
import com.pinterest.orion.core.Node;
import com.pinterest.orion.core.kafka.Brokerset;
import com.pinterest.orion.core.kafka.BrokersetState;
import com.pinterest.orion.core.kafka.KafkaCluster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class BrokersetStateSensor extends KafkaSensor {
    private static final Logger logger = Logger.getLogger(BrokersetStateSensor.class.getName());
    public static final String ATTR_BROKERSET_STATE_KEY = "brokersetState";

    @Override
    public void sense(KafkaCluster cluster) throws Exception {
        Attribute brokersetMapAttr = cluster.getAttribute(KafkaClusterInfoSensor.ATTR_BROKERSET_KEY);
        if (brokersetMapAttr == null) {
            logger.warning(String.format("Cluster %s has no brokerset.", cluster.getName()));
            return;
        }
        Map<String, Brokerset> brokersetMap = brokersetMapAttr.getValue();
        // Brokerset state map.
        Map<String, BrokersetState> brokersetStateMap = new HashMap<>();
        // BrokerId to brokerset alias set map
        Map<String, Set<String>> brokerToBrokersetsMap = new HashMap<>();
        for (String nodeId : cluster.getNodeMap().keySet()) {
            brokerToBrokersetsMap.put(nodeId, new HashSet<>());
        }
        for (Brokerset brokerset : brokersetMap.values()) {
            String brokersetAlias = brokerset.getBrokersetAlias();
            BrokersetState brokersetState = new BrokersetState(brokersetAlias);
            // brokerIds in this brokerset
            Set<String> brokerIds = new HashSet<>();
            List<Brokerset.BrokersetRange> brokersetRanges = brokerset.getEntries();
            if (brokersetRanges == null || brokersetRanges.isEmpty()) {
                logger.warning(String.format("Brokerset %s in cluster %s has no brokerset range.",
                    brokersetAlias, cluster.getName()));
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
                        handleInvalidBrokerset(nodeId, brokersetAlias, cluster.getName());
                    }
                }
                brokersetState.addBrokerRange(Arrays.asList(start, end));
            }
            brokersetState.setBrokerIds(new ArrayList<>(brokerIds));
            brokersetStateMap.put(brokersetAlias, brokersetState);
        }
        for (Node node : cluster.getNodeMap().values()) {
            if (node != null && node.getCurrentNodeInfo() != null) {
                String nodeId = node.getCurrentNodeInfo().getNodeId();
                node.getCurrentNodeInfo().setBrokersets(brokerToBrokersetsMap.get(nodeId));
            }
        }
        cluster.setAttribute(ATTR_BROKERSET_STATE_KEY, brokersetStateMap);
    }

    @Override
    public String getName() {
        return "BrokersetStateSensor";
    }

    protected void handleInvalidBrokerset(String nodeId, String brokersetAlias, String clusterId) {
        logger.warning(String.format("Brokerset %s in cluster %s has invalid broker id %s.",
            brokersetAlias, clusterId, nodeId));
    }
}
