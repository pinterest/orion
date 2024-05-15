package com.pinterest.orion.core.actions.memq;

import com.pinterest.orion.core.actions.aws.Ec2Utils;
import com.pinterest.orion.core.actions.aws.RebootEC2InstanceAction;
import com.pinterest.orion.core.memq.MemqCluster;
import com.pinterest.orion.utils.EC2Helper;
import software.amazon.awssdk.services.ec2.Ec2Client;

public abstract class MemqEC2BrokerRebootAction extends RebootEC2InstanceAction {

    /**
     * Reboot the node by rebooting the EC2 instance via the EC2 client.
     * @throws Exception if there is an error during reboot
     */
    @Override
    public void runAction() throws Exception {
        if(!initializeNode(false)){
            return;
        }
        setHostname(node.getCurrentNodeInfo().getHostname());
        String region = node.getCluster().getAttribute(MemqCluster.CLUSTER_REGION).getValue();
        setInstanceId(getEC2Helper().getInstanceIdUsingHostName(getHostname(), region));
        try (Ec2Client ec2Client = getEc2Client()) {
            super.rebootInstance(ec2Client);
            boolean running = isInstanceRunning(ec2Client);
            if (!running) {
                markFailed("Running state check timeout for " + getInstanceId());
                return;
            }
        } catch (Exception e) {
            markFailed("Error in instance reboot: " + e);
            return;
        }
        markSucceeded();
    }

    protected boolean isInstanceRunning(Ec2Client ec2Client) {
        try {
            Ec2Utils.waitForInstanceStateChange(
                    ec2Client, getInstanceId(), RUNNING_STATE, logger());
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public String getName() {
        return "RebootMemqEC2Node " + getHostname() + " (" + getInstanceId() + ")";
    }

    protected Ec2Client getEc2Client() {
        return Ec2Client.create();
    }

    protected abstract EC2Helper getEC2Helper();
}
