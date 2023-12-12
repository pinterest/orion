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
import java.util.List;
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
            if (healBrokers(candidates) && isChildActionsSuccess()) {
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

    /**
     * Check if all the child actions finished.
     * If any of the child actions failed, mark this action as failed.
     * @return true if all the child actions are successful.
     */
    private boolean isChildActionsSuccess() {
        boolean isSuccess = true;
        List<Action> childActions = getChildren();
        if (childActions == null || childActions.isEmpty()) {
            return true;
        }
        for (Action childAction : childActions) {
            try {
                childAction.get(); // wait for the child action to finish
            } catch (Exception e) {
                String errorMsg = String.format(
                        "Child action %s failed: %s",
                        childAction.getName(),
                        e.getMessage());
                logger.warning(errorMsg);
                getResult().appendOut(errorMsg);
                isSuccess = false;
            }
            isSuccess = isSuccess && childAction.isSuccess();
        }
        return isSuccess;
    }

    /**
     * This method triggers a BrokerRecoveryAction for a broker.
     * If the broker is non-existent or dead, it will replace the broker with a new broker.
     * If the broker is still online but Kafka process is down, it will try to restart the broker first. If restart fails, it will replace the broker.
     * @param brokerId the id of the broker to be recovered.
     * @throws Exception if the child action is not triggered successfully.
     */
    protected void triggerBrokerRecoveryAction(String brokerId) throws Exception {

        Action brokerRecoveryAction = newBrokerRecoveryAction();
        brokerRecoveryAction.setAttribute(OrionConstants.NODE_ID, brokerId, sensorSet);
        brokerRecoveryAction.setOwner("ClusterRecoveryAction");

        if (nonExistentBrokers.contains(brokerId)) {
            Node existingNode = cluster.getNodeMap().values().iterator().next();
            String extractedName = deriveNonexistentHostname(
                    existingNode.getCurrentNodeInfo().getHostname(),
                    existingNode.getCurrentNodeInfo().getNodeId(),
                    brokerId
            );
            // setting these attributes to indicate that the node doesn't exist in cluster map, and should skip any node-related checks
            brokerRecoveryAction.setAttribute(BrokerRecoveryAction.ATTR_NODE_EXISTS_KEY, false);
            brokerRecoveryAction.setAttribute(BrokerRecoveryAction.ATTR_NONEXISTENT_HOST_KEY, extractedName);
        }
        if (maybeDeadBrokers.contains(brokerId)) {
            // Setting this flag in the action will restart the broker before replacing the broker
            brokerRecoveryAction.setAttribute(BrokerRecoveryAction.ATTR_TRY_TO_RESTART_KEY, true);
            String restartNote =
                    "BrokerRecoveryAction will try to restart node " + brokerId + " before replacing it.";
            logger.info(restartNote);
            getResult().appendOut(restartNote);
        }
        String note = String.format(
                "Trigger BrokerRecoveryAction for node %s in cluster %s. Check the child action for details.",
                brokerId,
                cluster.getClusterId());
        logger.info(note);
        getResult().appendOut(note);
        dispatchChildAction(brokerRecoveryAction);
    }

    /**
     * This method adds a child action to the action list.
     * It also dispatches the child action to the action engine.
     * @param childAction the child action to be added.
     * @throws Exception if the child action is not triggered successfully.
     */
    private void dispatchChildAction(Action childAction) throws Exception {
        this.getChildren().add(childAction);
        getEngine().dispatchChild(this, childAction);
    }

    /**
     * This method triggers a BrokerRecoveryAction for each broker in candidates.
     * It checks if the number of candidates is more than one.
     * If the number of candidates is more than one, it will alert and not trigger any action.
     * @param candidates a set of broker ids that are in bad states.
     * @return true if all the child actions are triggered successfully.
     * @throws Exception if any of the child actions is not triggered successfully.
     */
    protected boolean healBrokers(Set<String> candidates) throws Exception {
        String output;
        boolean isActionTriggered = false;
        if (candidates.size() == 1) {
            output = String.format(
                    "ClusterRecoveryAction attempts to recover broker %s. ",
                    candidates.iterator().next());
            String brokerId = candidates.iterator().next();
            logger.info(output);
            triggerBrokerRecoveryAction(brokerId);
            isActionTriggered = true;
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
        return isActionTriggered;
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

    /**
     * This method derives the hostname of a non-existent node from the hostname of an existing node.
     * @param existingHostname the hostname of an existing node.
     * @param existingId the id of the existing node.
     * @param nonExistingId the id of the non-existent node.
     * @return the hostname of the non-existent node.
     */
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

    /**
     * This method removes all the nodes that are replaced within cooldownMilliseconds from candidates.
     * It also adds the new set of recovering nodes to the recovering node set.
     * If the recovering node set reaches TTL, it replaces the recovering node set with candidates.
     * @param candidates a set of nodes that are in bad states.
     * @param cluster the cluster that the nodes belong to.
     */
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
