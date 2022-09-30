package com.pinterest.orion.core.kafka;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;


public class KafkaClusterTest {

    private static final Map<String, Object> EMPTY_MAP = new HashMap<>();
    private static final Map<String, Integer> RESULT_INT_MAP = new HashMap<String, Integer>() {{
        put(KafkaCluster.ATTR_KAFKA_ADMIN_CLIENT_CLUSTER_REQUEST_TIMEOUT_MILLISECONDS_KEY, 11111);
        put(KafkaCluster.ATTR_KAFKA_ADMIN_CLIENT_TOPIC_REQUEST_TIMEOUT_MILLISECONDS_KEY, 22222);
        put(KafkaCluster.ATTR_KAFKA_ADMIN_CLIENT_CONSUMER_GROUP_REQUEST_TIMEOUT_MILLISECONDS_KEY, 33333);
    }};

    @Test
    public void testGetKafkaAdminClientClusterRequestTimeoutMilliseconds() throws Exception {
        KafkaCluster cluster = new KafkaCluster("", "", Collections.emptyList(),
                Collections.emptyList(), null, null, null, null, null);
        // Cluster config attribute is null
        assertEquals(-1, cluster.getKafkaAdminClientClusterRequestTimeoutMilliseconds());
        // Cluster config attribute value is null
        cluster.setAttribute(cluster.ATTR_CONF_KEY, null);
        assertEquals(-1, cluster.getKafkaAdminClientClusterRequestTimeoutMilliseconds());
        // Cluster does not have timeout attribute
        cluster.setAttribute(cluster.ATTR_CONF_KEY, EMPTY_MAP);
        assertEquals(-1, cluster.getKafkaAdminClientClusterRequestTimeoutMilliseconds());
        // Cluster has cluster admin client timeout attribute
        cluster.setAttribute(cluster.ATTR_CONF_KEY, RESULT_INT_MAP);
        assertEquals(11111, cluster.getKafkaAdminClientClusterRequestTimeoutMilliseconds());
    }

    @Test
    public void testGetKafkaAdminClientTopicRequestTimeoutMilliseconds() throws Exception {
        KafkaCluster cluster = new KafkaCluster("", "", Collections.emptyList(),
                Collections.emptyList(), null, null, null, null, null);
        // Cluster config attribute is null
        assertEquals(-1, cluster.getKafkaAdminClientTopicRequestTimeoutMilliseconds());
        // Cluster config attribute value is null
        cluster.setAttribute(cluster.ATTR_CONF_KEY, null);
        assertEquals(-1, cluster.getKafkaAdminClientTopicRequestTimeoutMilliseconds());
        // Cluster does not have timeout attribute
        cluster.setAttribute(cluster.ATTR_CONF_KEY, EMPTY_MAP);
        assertEquals(-1, cluster.getKafkaAdminClientTopicRequestTimeoutMilliseconds());
        // Cluster has topic admin client timeout attribute
        cluster.setAttribute(cluster.ATTR_CONF_KEY, RESULT_INT_MAP);
        assertEquals(22222, cluster.getKafkaAdminClientTopicRequestTimeoutMilliseconds());
    }

    @Test
    public void testGetKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds() throws Exception {
        KafkaCluster cluster = new KafkaCluster("", "", Collections.emptyList(),
                Collections.emptyList(), null, null, null, null, null);
        // Cluster config attribute is null
        assertEquals(-1, cluster.getKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds());
        // Cluster config attribute value is null
        cluster.setAttribute(cluster.ATTR_CONF_KEY, null);
        assertEquals(-1, cluster.getKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds());
        // Cluster does not have timeout attribute
        cluster.setAttribute(cluster.ATTR_CONF_KEY, EMPTY_MAP);
        assertEquals(-1, cluster.getKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds());
        // Cluster has consumer group admin client timeout attribute
        cluster.setAttribute(cluster.ATTR_CONF_KEY, RESULT_INT_MAP);
        assertEquals(33333, cluster.getKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds());
    }
}
