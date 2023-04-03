package com.pinterest.orion.core.actions.kafka;

import com.pinterest.orion.core.Attribute;
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
        zoneName = config.get(Ec2Utils.CONF_ROUTE53_ZONE_NAME).toString();
        zoneId = config.get(Ec2Utils.CONF_ROUTE53_ZONE_ID).toString();
        ArrayList<String> errors = new ArrayList<>();
        Map<String, Attribute> attributes = getAttributes();
        if(!attributes.containsKey(NODE_NAME)) {
            errors.add(NODE_NAME);
        } else {
            nodeName = attributes.get(NODE_NAME).getValue();
        }
        if(!attributes.containsKey(IP_ADDR)) {
            errors.add(IP_ADDR);
        } else {
            ipAddr = attributes.get(IP_ADDR).getValue();
        }
        if (!errors.isEmpty()) {
            throw new ExceptionInInitializerError(
                    "The creation of KafkaDNSUpsertAction was missing attributes: " + errors);
        }
    }

    @Override
    public void runAction() throws Exception {
        try {
            Ec2Utils.upsertR53Record(nodeName, ipAddr, zoneName, zoneId, logger);
            markSucceeded();
        } catch (Exception e) {
            markFailed(e);
        }
    }
}
