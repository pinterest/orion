package com.pinterest.orion.core.actions.memq;

import com.pinterest.orion.core.Node;
import com.pinterest.orion.core.actions.aws.Ec2Utils;
import com.pinterest.orion.core.actions.generic.NodeDecommissionAction;
import com.pinterest.orion.core.memq.MemqCluster;
import com.pinterest.orion.utils.EC2Helper;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesResponse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class MemqEC2BrokerDecommissionAction extends NodeDecommissionAction {
    protected static Set<InstanceStateName> TERMINATION_STATE = new HashSet<>(
            Arrays.asList(InstanceStateName.STOPPED, InstanceStateName.TERMINATED));

    /**
     * Decommission the node by terminating the EC2 instance via the EC2 client.
     * @param node The node to decommission
     * @return true if the decommission action is successful, false otherwise
     * @throws Exception if there is an error during decommission
     */
    @Override
    public boolean decommission(Node node) throws Exception {
        String hostName = node.getCurrentNodeInfo().getHostname();
        String region = node.getCluster().getAttribute(MemqCluster.CLUSTER_REGION).getValue();
        String instanceId = getEC2Helper().getInstanceIdUsingHostName(hostName, region);
        try (Ec2Client ec2Client = getEc2Client()) {
            boolean hasTerminating = doTermination(ec2Client, instanceId);
            if (!hasTerminating) {
                markFailed("Could not terminate the instance " + instanceId);
                return false;
            }
            boolean terminated = isInstanceTerminated(ec2Client, instanceId);
            if (!terminated) {
                markFailed("Termination state check timeout for " + instanceId);
                return false;
            }
        } catch (Exception e) {
            markFailed("Error in instance termination: " + e);
            return false;
        }
        super.decommission(node);
        markSucceeded();
        return true;
    }

    protected static boolean doTermination(Ec2Client ec2Client, String instanceId) {
        TerminateInstancesRequest terminateInstancesRequest =
                TerminateInstancesRequest.builder().instanceIds(instanceId).build();
        TerminateInstancesResponse terminateInstancesResponse =
                ec2Client.terminateInstances(terminateInstancesRequest);
        return terminateInstancesResponse.hasTerminatingInstances();
    }

    protected boolean isInstanceTerminated(Ec2Client ec2Client, String instanceId) {
        try {
            Ec2Utils.waitForInstanceStateChange(ec2Client, instanceId, TERMINATION_STATE, logger());
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public String getName() {
        return "MemqEC2BrokerDecommissionAction";
    }

    protected Ec2Client getEc2Client() {
        return Ec2Client.create();
    }

    protected abstract EC2Helper getEC2Helper();
}
