package com.pinterest.orion.core.actions.kafka;

import com.google.common.collect.Sets;
import com.pinterest.orion.core.Attribute;
import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.Node;
import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.alert.AlertLevel;
import com.pinterest.orion.core.actions.alert.AlertMessage;
import com.pinterest.orion.core.actions.generic.GenericClusterWideAction;
import com.pinterest.orion.server.OrionServer;
import com.pinterest.orion.utils.OrionConstants;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class ClusterRecoveryAction extends GenericClusterWideAction.ClusterAction {

    protected Cluster cluster;
    protected Set<String> candidates;
    protected Set<String> deadBrokers;
    protected Set<String> maybeDeadBrokers;
    protected Set<String> nonExistentBrokers;
    protected Set<String> sensorSet;
    private static final Logger logger = Logger.getLogger(ClusterRecoveryAction.class.getName());
    public static final String ATTR_RECOVERING_NODES = "recovering_nodes";
    private static final long cooldownMilliseconds = 3600_000L; // 1 hour

    @Override
    public String getName() {
        return "Cluster Recovery Action";
    }

    @Override
    public void runAction() throws Exception {
        String startNote = String.format(
                "ClusterRecoveryAction will try to recover brokers %s in cluster %s. ",
                candidates,
                cluster.getClusterId());
        logger.info(startNote);
        getResult().appendOut(startNote);
        try {
            boolean isSucceed = healBrokers(candidates);
            if (isSucceed) {
                markSucceeded();
            } else {
                markFailed(String.format(
                        "ClusterRecoveryAction failed to recover brokers %s in cluster %s. ",
                        candidates,
                        cluster.getClusterId()));
            }
        } catch (Exception e) {
            markFailed(e);
        }
    }

    protected void healBroker(String deadBrokerId) throws Exception {
        // This will trigger an action that will attempt to replace the broker.
        // If agent is still online but Kafka process is down, it will try to restart the broker first.
        Action brokerRecoveryAction = newBrokerRecoveryAction();
        brokerRecoveryAction.setAttribute(OrionConstants.NODE_ID, deadBrokerId, sensorSet);
        brokerRecoveryAction.setOwner("ClusterRecoveryAction");

        if (nonExistentBrokers.contains(deadBrokerId)) {
            Node existingNode = cluster.getNodeMap().values().iterator().next();
            String extractedName = deriveNonexistentHostname(
                    existingNode.getCurrentNodeInfo().getHostname(),
                    existingNode.getCurrentNodeInfo().getNodeId(),
                    deadBrokerId
            );
            // setting these attributes to indicate that the node doesn't exist in cluster map, and should skip any node-related checks
            brokerRecoveryAction.setAttribute(BrokerRecoveryAction.ATTR_NODE_EXISTS_KEY, false);
            brokerRecoveryAction.setAttribute(BrokerRecoveryAction.ATTR_NONEXISTENT_HOST_KEY, extractedName);
        }
        if (maybeDeadBrokers.contains(deadBrokerId)) {
            // Setting this flag in the action will restart the broker before replacing the broker
            brokerRecoveryAction.setAttribute(BrokerRecoveryAction.ATTR_TRY_TO_RESTART_KEY, true);
            String restartNote = "Will try to restart node " + deadBrokerId + " before replacing it. ";
            logger.info(restartNote);
            getResult().appendOut(restartNote);
        }
        String dispatchNote = String.format(
                "Dispatching BrokerRecoveryAction for node %s in cluster %s. ",
                deadBrokerId,
                cluster.getClusterId());
        logger.info(dispatchNote);
        getResult().appendOut(dispatchNote);
        getEngine().dispatch(brokerRecoveryAction);
    }

    protected boolean healBrokers(Set<String> candidates) throws Exception {
        String output;
        boolean isSucceed = false;
        if (candidates.size() == 1) {
            output = String.format(
                    "ClusterRecoveryAction trys to recover broker %s. ",
                    candidates.iterator().next());
            String deadBrokerId = candidates.iterator().next();
            healBroker(deadBrokerId);
            logger.info(output);
            isSucceed = true;
        } else if (candidates.size() > 1){
            // more than 1 brokers are dead... better alert and have human intervention
            output = String.format("More than one brokers are in bad state - dead: %s, service down: %s. " +
                            "ClusterRecoveryAction skips automatic recovery and pages oncall. ",
                    deadBrokers, maybeDeadBrokers);
            cluster.getActionEngine().alert(AlertLevel.HIGH, new AlertMessage(
                    candidates.size() + " brokers on " + cluster.getClusterId() + " are unhealthy",
                    output,
                    "orion"
            ));
            OrionServer.metricsCounterInc(
                    "broker.services.unhealthy",
                    new HashMap<String, String>() {{
                        put("clusterId", cluster.getClusterId());
                    }}
            );
            logger.severe(output);
        } else {
            output = String.format("No candidates for healing in cluster %s. ", cluster.getClusterId());
            logger.warning(output);
        }
        getResult().appendOut(output);
        return isSucceed;
    }

    public void setCandidates(Set<String> candidates) {
        this.candidates = candidates;
    }

    public void setDeadBrokers(Set<String> deadBrokers) {
        this.deadBrokers = deadBrokers;
    }

    public void setMaybeDeadBrokers(Set<String> maybeDeadBrokers) {
        this.maybeDeadBrokers = maybeDeadBrokers;
    }

    public void setNonExistentBrokers(Set<String> nonExistentBrokers) {
        this.nonExistentBrokers = nonExistentBrokers;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public void setSensorSet(Set<String> sensorSet) {
        this.sensorSet = sensorSet;
    }

    protected static String deriveNonexistentHostname(String existingHostname, String existingId, String nonExistingId) {
        existingHostname = existingHostname.split("\\.", 2)[0]; // sanitize potential suffixes
        int diff = nonExistingId.length() - existingId.length();
        if ( diff > 0 ) {
            existingId = StringUtils.leftPad(existingId, diff, '0');
        } else if (diff < 0) {
            nonExistingId = StringUtils.leftPad(nonExistingId, -diff, '0');
        }

        String ret = existingHostname.replace(existingId, nonExistingId);
        if (ret.equals(existingHostname)) {
            return null;
        }
        return ret;
    }

    protected BrokerRecoveryAction newBrokerRecoveryAction() {
        return new BrokerRecoveryAction();
    }

    public static void removeRecoveringNodesFromCandidates(Set<String> candidates, Cluster cluster) {
        // Remove all the nodes that are replaced within cooldownMilliseconds from candidates.
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        Attribute recoveringNodesAttr = cluster.getAttribute(ATTR_RECOVERING_NODES);
        if (recoveringNodesAttr == null || recoveringNodesAttr.getValue() == null) {
            // Initialize the recovering node set with the new set of candidates.
            cluster.setAttribute(ATTR_RECOVERING_NODES, candidates);
            return;
        }
        Set<String> recoveringNodes = recoveringNodesAttr.getValue();
        if (System.currentTimeMillis() - recoveringNodesAttr.getUpdateTimestamp() < cooldownMilliseconds) {
            // Remove all the nodes that are replaced within cooldownMilliseconds from candidates
            for (String node : recoveringNodes) {
                if (candidates.contains(node)) {
                    candidates.remove(node);
                }
            }
            if (candidates.isEmpty()) {
                // All the candidates are replaced within cooldownMilliseconds. Skip this round.
                logger.warning(String.format(
                        "Nodes in cooldown phase: %s; Last action time: %s; Skip recovering",
                        recoveringNodes,
                        new Date(recoveringNodesAttr.getUpdateTimestamp())));
            } else {
                // Add the new set of recovering nodes to the recovering node set. Reset update time.
                logger.warning(String.format(
                        "Nodes in cooldown phase: %s; Last action time: %s; Recovering nodes: %s",
                        recoveringNodes,
                        new Date(recoveringNodesAttr.getUpdateTimestamp()),
                        candidates));
                // TODO: Can add alert here if the size of recoveringNodes is too large.
                cluster.setAttribute(ATTR_RECOVERING_NODES, new HashSet<>(Sets.union(recoveringNodes, candidates)));
            }
        } else {
            // RecoveringNodes set reaches TTL. Replace the recovering node set with candidates.
            logger.warning(String.format(
                    "Previous recovering node set is timeout: " +
                            "Nodes no longer in recovering mode: %s; " +
                            "Last action time: %s; " +
                            "Recovering nodes: %s",
                    recoveringNodes,
                    new Date(recoveringNodesAttr.getUpdateTimestamp()),
                    candidates));
            cluster.setAttribute(ATTR_RECOVERING_NODES, new HashSet<>(candidates));
        }
    }
}
