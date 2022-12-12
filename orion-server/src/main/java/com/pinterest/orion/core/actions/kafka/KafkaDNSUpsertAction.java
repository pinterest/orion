package com.pinterest.orion.core.actions.kafka;

import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.aws.Ec2Utils;

import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

public class KafkaDNSUpsertAction extends Action {
    private static final Logger logger = Logger
            .getLogger(KafkaDNSUpsertAction.class.getCanonicalName());

    public static String NODE_NAME = "NODE_NAME";
    public static String IP_ADDR = "IP_ADDR";

    String zoneName;
    String zoneId;
    private String nodeName;
    private String ipAddr;

    @Override
    public String getName() {
        return "KafkaDNSUpsertAction";
    }

    @Override
    public void initialize(Map<String, Object> config) throws PluginConfigurationException {
        super.initialize(config);
        if (!config.containsKey(Ec2Utils.CONF_ROUTE53_ZONE_ID)) {
            throw new PluginConfigurationException(
                    "Cannot find key " + Ec2Utils.CONF_ROUTE53_ZONE_ID + " in config of " + getName());
        }
        if (!config.containsKey(Ec2Utils.CONF_ROUTE53_ZONE_NAME)) {
            throw new PluginConfigurationException(
                    "Cannot find key " + Ec2Utils.CONF_ROUTE53_ZONE_NAME + " in config " + getName());
        }
        zoneName = config.get(Ec2Utils.CONF_ROUTE53_ZONE_ID).toString();
        zoneId = config.get(Ec2Utils.CONF_ROUTE53_ZONE_NAME).toString();
        ArrayList<String> errors = new ArrayList<>();
        if(!config.containsKey(NODE_NAME)) {
            errors.add(NODE_NAME);
        }
        nodeName = config.get(NODE_NAME).toString();
        if(!config.containsKey(IP_ADDR)) {
            errors.add(IP_ADDR);
        }
        ipAddr = config.get(IP_ADDR).toString();
        if (!errors.equals("")) {
            logger.severe("The creation of KafkaDNSUpsertAction was missing configs: " + errors);
        }
    }

    @Override
    public void runAction() throws Exception {
        Ec2Utils.upsertR53Record(nodeName, ipAddr, zoneName, zoneId, logger);
    }
}
