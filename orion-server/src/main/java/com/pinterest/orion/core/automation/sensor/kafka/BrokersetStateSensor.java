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

    /**
     * KafkaClusterInfoSensor loads the brokerset information from the config files.
     * This sensor generates brokerset state information based on the brokerset information.
     * @param cluster Kafka cluster to sense.
     * @throws Exception If there is an error sensing the cluster.
     */
    @Override
    public void sense(KafkaCluster cluster) throws Exception {
        // Brokerset information and broker information are loaded from other sensors.
        // If they are not ready, skip this sensor.
        if (!cluster.containsAttribute(KafkaClusterInfoSensor.ATTR_BROKERSET_KEY)) {
            // No brokerset information available. Skip.
            return;
        }
        Attribute brokersetMapAttr = cluster.getAttribute(KafkaClusterInfoSensor.ATTR_BROKERSET_KEY);
        if (brokersetMapAttr == null) {
            // No brokerset information available. Skip.
            return;
        }
        if (cluster.getNodeMap() == null || cluster.getNodeMap().isEmpty()) {
            // No node information available. Skip.
            return;
        }
        // Brokerset info from config files
        Map<String, Brokerset> brokersetMap = brokersetMapAttr.getValue();
        // Brokerset state map.
        Map<String, BrokersetState> brokersetStateMap = new HashMap<>();
        // BrokerId to brokerset alias set map
        Map<String, Set<String>> brokerToBrokersetsMap = new HashMap<>();
        for (String nodeId : cluster.getNodeMap().keySet()) {
            brokerToBrokersetsMap.put(nodeId, new HashSet<>());
        }
        // Get broker ids for each brokerset.
        for (Brokerset brokerset : brokersetMap.values()) {
            String brokersetAlias = brokerset.getBrokersetAlias();
            BrokersetState brokersetState = new BrokersetState(brokersetAlias);
            Set<String> brokerIds = new HashSet<>();
            List<Brokerset.BrokersetRange> brokersetRanges = brokerset.getEntries();
            if (brokersetRanges == null || brokersetRanges.isEmpty()) {
                logger.warning(String.format("Brokerset %s in cluster %s has no brokerset range.",
                    brokersetAlias, cluster.getName()));
                continue;
            }
            boolean invalidBrokerset = false;
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
                        invalidBrokerset = true;
                    }
                }
                brokersetState.addBrokerRange(Arrays.asList(start, end));
            }
            brokersetState.setBrokerIds(new ArrayList<>(brokerIds));
            try {
                updateBrokersetStateWithMetrics(cluster, brokersetState, brokerIds);
            } catch (Exception e) {
                logger.warning(
                    String.format(
                        "Failed to update brokerset state with metrics for brokerset %s in cluster %s. Error: %s",
                        brokersetAlias,
                        cluster.getName(),
                        e.getMessage()));
            }
            brokersetStateMap.put(brokersetAlias, brokersetState);
            if (invalidBrokerset) {
                handleInvalidBrokerset(brokersetAlias, cluster.getName());
            }
        }
        // TODO: Add other brokerset status information (ex. usage data).
        cluster.setAttribute(ATTR_BROKERSET_STATE_KEY, brokersetStateMap);
        // Update node info with brokerset information.
        for (Node node : cluster.getNodeMap().values()) {
            if (node != null && node.getCurrentNodeInfo() != null) {
                String nodeId = node.getCurrentNodeInfo().getNodeId();
                node.getCurrentNodeInfo().setBrokersets(brokerToBrokersetsMap.get(nodeId));
            }
        }
    }

    /**
     * Update brokerset state with metrics.
     * This method should be overridden by subclasses to update brokerset state with metrics.
     * @param cluster Kafka cluster.
     * @param brokersetState Brokerset state to update. It has state fields and raw metrics fields to update.
     * @param brokerIds Broker ids in the brokerset.
     */
    protected void updateBrokersetStateWithMetrics(
        KafkaCluster cluster,
        BrokersetState brokersetState,
        Set<String> brokerIds) {
    }

    @Override
    public String getName() {
        return "BrokersetStateSensor";
    }

    /**
     * Handle invalid brokerset.
     * Invalid brokerset means that some brokers recorded in brokerset config files are not found in the cluster.
     * @param brokersetAlias Brokerset alias.
     * @param clusterId Cluster id.
     */
    protected void handleInvalidBrokerset(String brokersetAlias, String clusterId) {
        logger.warning(String.format("Brokerset %s in cluster %s has invalid brokers.",
            brokersetAlias, clusterId));
    }
}
