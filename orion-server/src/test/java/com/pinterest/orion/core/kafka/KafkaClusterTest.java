package com.pinterest.orion.core.kafka;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class KafkaClusterTest {

    private static final Map<String, Object> EMPTY_MAP = new HashMap<>();
    private static final Map<String, String> RESULT_MAP = new HashMap<String, String>() {{
        put(KafkaCluster.ATTR_KAFKA_ADMIN_CLIENT_CLUSTER_REQUEST_TIMEOUT_MILLISECONDS_KEY, "11111");
        put(KafkaCluster.ATTR_KAFKA_ADMIN_CLIENT_TOPIC_REQUEST_TIMEOUT_MILLISECONDS_KEY, "22222");
        put(KafkaCluster.ATTR_KAFKA_ADMIN_CLIENT_CONSUMER_GROUP_REQUEST_TIMEOUT_MILLISECONDS_KEY, "33333");
    }};

    @Test
    public void testGetKafkaAdminClientClusterRequestTimeoutMilliseconds() throws Exception {
        KafkaCluster cluster = new KafkaCluster("", "", Collections.emptyList(),
                Collections.emptyList(), null, null, null, null, null);
        // Cluster config attribute is null
        assertFalse(cluster.containsKafkaAdminClientClusterRequestTimeoutMilliseconds());
        // Cluster config attribute value is null
        cluster.setAttribute(cluster.ATTR_CONF_KEY, null);
        assertFalse(cluster.containsKafkaAdminClientClusterRequestTimeoutMilliseconds());
        // Cluster does not have timeout attribute
        cluster.setAttribute(cluster.ATTR_CONF_KEY, EMPTY_MAP);
        assertFalse(cluster.containsKafkaAdminClientClusterRequestTimeoutMilliseconds());
        // Cluster has cluster admin client timeout attribute
        cluster.setAttribute(cluster.ATTR_CONF_KEY, RESULT_MAP);
        assertTrue(cluster.containsKafkaAdminClientClusterRequestTimeoutMilliseconds());
        assertEquals(11111, cluster.getKafkaAdminClientClusterRequestTimeoutMilliseconds());
    }

    @Test
    public void testGetKafkaAdminClientTopicRequestTimeoutMilliseconds() throws Exception {
        KafkaCluster cluster = new KafkaCluster("", "", Collections.emptyList(),
                Collections.emptyList(), null, null, null, null, null);
        // Cluster config attribute is null
        assertFalse(cluster.containsKafkaAdminClientTopicRequestTimeoutMilliseconds());
        // Cluster config attribute value is null
        cluster.setAttribute(cluster.ATTR_CONF_KEY, null);
        assertFalse(cluster.containsKafkaAdminClientTopicRequestTimeoutMilliseconds());
        // Cluster does not have timeout attribute
        cluster.setAttribute(cluster.ATTR_CONF_KEY, EMPTY_MAP);
        assertFalse(cluster.containsKafkaAdminClientTopicRequestTimeoutMilliseconds());
        // Cluster has topic admin client timeout attribute
        cluster.setAttribute(cluster.ATTR_CONF_KEY, RESULT_MAP);
        assertTrue(cluster.containsKafkaAdminClientTopicRequestTimeoutMilliseconds());
        assertEquals(22222, cluster.getKafkaAdminClientTopicRequestTimeoutMilliseconds());
    }

    @Test
    public void testGetKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds() throws Exception {
        KafkaCluster cluster = new KafkaCluster("", "", Collections.emptyList(),
                Collections.emptyList(), null, null, null, null, null);
        // Cluster config attribute is null
        assertFalse(cluster.containsKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds());
        // Cluster config attribute value is null
        cluster.setAttribute(cluster.ATTR_CONF_KEY, null);
        assertFalse(cluster.containsKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds());
        // Cluster does not have timeout attribute
        cluster.setAttribute(cluster.ATTR_CONF_KEY, EMPTY_MAP);
        assertFalse(cluster.containsKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds());
        // Cluster has consumer group admin client timeout attribute
        cluster.setAttribute(cluster.ATTR_CONF_KEY, RESULT_MAP);
        assertTrue(cluster.containsKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds());
        assertEquals(33333, cluster.getKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds());
    }
}
