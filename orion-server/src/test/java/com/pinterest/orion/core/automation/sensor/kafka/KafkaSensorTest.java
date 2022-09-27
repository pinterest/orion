package com.pinterest.orion.core.automation.sensor.kafka;

import com.pinterest.orion.core.Attribute;
import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.kafka.KafkaCluster;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class KafkaSensorTest {

    private static final Map<String, Object> EMPTY_MAP = new HashMap<>();
    private static final Map<String, String> RESULT_MAP = new HashMap<String, String>() {{
        put(KafkaSensor.ATTR_KAFKA_ADMIN_CLIENT_CLUSTER_REQUEST_TIMEOUT_MILLISECONDS_KEY, "11111");
        put(KafkaSensor.ATTR_KAFKA_ADMIN_CLIENT_TOPIC_REQUEST_TIMEOUT_MILLISECONDS_KEY, "22222");
        put(KafkaSensor.ATTR_KAFKA_ADMIN_CLIENT_CONSUMER_GROUP_REQUEST_TIMEOUT_MILLISECONDS_KEY, "33333");
    }};

    @Test
    public void testGetKafkaAdminClientClusterRequestTimeoutMilliseconds() throws Exception {
        KafkaCluster cluster = Mockito.mock(KafkaCluster.class);
        Mockito.when(cluster.containsAttribute(Cluster.ATTR_CONF_KEY)).thenReturn(true);
        // Cluster config attribute is null
        Mockito.when(cluster.getAttribute(Cluster.ATTR_CONF_KEY)).thenReturn(null);
        assertFalse(KafkaSensor.containsKafkaAdminClientClusterRequestTimeoutMilliseconds(cluster));
        // Cluster config attribute value is null
        Mockito.when(cluster.getAttribute(Cluster.ATTR_CONF_KEY)).thenReturn(new Attribute(null, null, System.currentTimeMillis()));
        assertFalse(KafkaSensor.containsKafkaAdminClientClusterRequestTimeoutMilliseconds(cluster));
        // Cluster does not have timeout attribute
        Mockito.when(cluster.getAttribute(Cluster.ATTR_CONF_KEY)).thenReturn(new Attribute(null, EMPTY_MAP, System.currentTimeMillis()));
        assertFalse(KafkaSensor.containsKafkaAdminClientClusterRequestTimeoutMilliseconds(cluster));
        // Cluster has cluster admin client timeout attribute
        Mockito.when(cluster.getAttribute(Cluster.ATTR_CONF_KEY)).thenReturn(new Attribute(null, RESULT_MAP, System.currentTimeMillis()));
        assertTrue(KafkaSensor.containsKafkaAdminClientClusterRequestTimeoutMilliseconds(cluster));
        assertEquals(11111, KafkaSensor.getKafkaAdminClientClusterRequestTimeoutMilliseconds(cluster));
    }

    @Test
    public void testGetKafkaAdminClientTopicRequestTimeoutMilliseconds() throws Exception {
        KafkaCluster cluster = Mockito.mock(KafkaCluster.class);
        Mockito.when(cluster.containsAttribute(Cluster.ATTR_CONF_KEY)).thenReturn(true);
        // Cluster config attribute is null
        Mockito.when(cluster.getAttribute(Cluster.ATTR_CONF_KEY)).thenReturn(null);
        assertFalse(KafkaSensor.containsKafkaAdminClientTopicRequestTimeoutMilliseconds(cluster));
        // Cluster config attribute value is null
        Mockito.when(cluster.getAttribute(Cluster.ATTR_CONF_KEY)).thenReturn(new Attribute(null, null, System.currentTimeMillis()));
        assertFalse(KafkaSensor.containsKafkaAdminClientTopicRequestTimeoutMilliseconds(cluster));
        // Cluster does not have timeout attribute
        Mockito.when(cluster.getAttribute(Cluster.ATTR_CONF_KEY)).thenReturn(new Attribute(null, EMPTY_MAP, System.currentTimeMillis()));
        assertFalse(KafkaSensor.containsKafkaAdminClientTopicRequestTimeoutMilliseconds(cluster));
        // Cluster has cluster admin client timeout attribute
        Mockito.when(cluster.getAttribute(Cluster.ATTR_CONF_KEY)).thenReturn(new Attribute(null, RESULT_MAP, System.currentTimeMillis()));
        assertTrue(KafkaSensor.containsKafkaAdminClientTopicRequestTimeoutMilliseconds(cluster));
        assertEquals(22222, KafkaSensor.getKafkaAdminClientTopicRequestTimeoutMilliseconds(cluster));
    }

    @Test
    public void testGetKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds() throws Exception {
        KafkaCluster cluster = Mockito.mock(KafkaCluster.class);
        Mockito.when(cluster.containsAttribute(Cluster.ATTR_CONF_KEY)).thenReturn(true);
        // Cluster config attribute is null
        Mockito.when(cluster.getAttribute(Cluster.ATTR_CONF_KEY)).thenReturn(null);
        assertFalse(KafkaSensor.containsKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds(cluster));
        // Cluster config attribute value is null
        Mockito.when(cluster.getAttribute(Cluster.ATTR_CONF_KEY)).thenReturn(new Attribute(null, null, System.currentTimeMillis()));
        assertFalse(KafkaSensor.containsKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds(cluster));
        // Cluster does not have timeout attribute
        Mockito.when(cluster.getAttribute(Cluster.ATTR_CONF_KEY)).thenReturn(new Attribute(null, EMPTY_MAP, System.currentTimeMillis()));
        assertFalse(KafkaSensor.containsKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds(cluster));
        // Cluster has cluster admin client timeout attribute
        Mockito.when(cluster.getAttribute(Cluster.ATTR_CONF_KEY)).thenReturn(new Attribute(null, RESULT_MAP, System.currentTimeMillis()));
        assertTrue(KafkaSensor.containsKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds(cluster));
        assertEquals(33333, KafkaSensor.getKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds(cluster));
    }
}
