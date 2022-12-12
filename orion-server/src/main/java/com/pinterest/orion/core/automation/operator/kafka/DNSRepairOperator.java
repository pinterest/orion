package com.pinterest.orion.core.automation.operator.kafka;

import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.core.Node;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.aws.Ec2Utils;
import com.pinterest.orion.core.actions.kafka.KafkaDNSUpsertAction;
import com.pinterest.orion.core.kafka.KafkaCluster;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.logging.Logger;

public class DNSRepairOperator extends KafkaOperator {
    private static final Logger logger = Logger
            .getLogger(DNSRepairOperator.class.getCanonicalName());
    String zoneName = null;

    @Override
    public void initialize(Map<String, Object> config) throws PluginConfigurationException {
        super.initialize(config);
        if (!config.containsKey(Ec2Utils.CONF_ROUTE53_ZONE_NAME)) {
            throw new PluginConfigurationException(
                    "Cannot find key " + Ec2Utils.CONF_ROUTE53_ZONE_NAME + " in config " + getName());
        }
        zoneName = config.get(Ec2Utils.CONF_ROUTE53_ZONE_NAME).toString();
    }

    @Override
    public void operate(KafkaCluster cluster) throws Exception {
        Map<String, Node> nodeMap = cluster.getNodeMap();

        for (Map.Entry<String, Node> entry : nodeMap.entrySet()) {
            NodeInfo nodeInfo = entry.getValue().getCurrentNodeInfo();
            //Hostname includes the zone name in it `.ec2.pin220.com`
            String hostName = nodeInfo.getHostname();
            String ipAddr = nodeInfo.getIp();
            try {
                InetAddress.getByName(hostName);
            } catch (UnknownHostException e) {
                String nameWithoutZone = hostName.split("\\.")[0];
                Action action = createDNSUpsertAction(nameWithoutZone, ipAddr);
                logger.info("DNS entry missing for [" + hostName + "], starting DNSUpsertAction");
                dispatch(action);
            }
        }
    }

    public static KafkaDNSUpsertAction createDNSUpsertAction(String nodeName, String ipAddr) {
        KafkaDNSUpsertAction action = new KafkaDNSUpsertAction();
        action.setAttribute(KafkaDNSUpsertAction.NODE_NAME, nodeName);
        action.setAttribute(KafkaDNSUpsertAction.IP_ADDR, ipAddr);
        return action;
    }

    @Override
    public String getName() {
        return "DNSRepairOperator";
    }
}
