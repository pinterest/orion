package com.pinterest.orion.core.automation.sensor.kafka;

import com.pinterest.orion.core.Attribute;
import com.pinterest.orion.core.kafka.KafkaCluster;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class KafkaSensorTest {

    @Test
    public void testGetKafkaAdminClientClusterRequestTimeoutMilliseconds() throws Exception {
        KafkaCluster cluster = Mockito.mock(KafkaCluster.class);
        Attribute valueAttribute = Mockito.mock(Attribute.class);
        // Cluster does not have timeout attribute
        Mockito.when(cluster.containsAttribute(KafkaSensor.ATTR_KAFKA_ADMIN_CLIENT_CLUSTER_REQUEST_TIMEOUT_MILLISECONDS_KEY)).thenReturn(false);
        assertFalse(KafkaSensor.containsKafkaAdminClientClusterRequestTimeoutMilliseconds(cluster));
        // Cluster has cluster admin client timeout attribute
        Mockito.when(valueAttribute.getValue()).thenReturn("11111");
        Mockito.when(cluster.containsAttribute(KafkaSensor.ATTR_KAFKA_ADMIN_CLIENT_CLUSTER_REQUEST_TIMEOUT_MILLISECONDS_KEY)).thenReturn(true);
        Mockito.when(cluster.getAttribute(KafkaSensor.ATTR_KAFKA_ADMIN_CLIENT_CLUSTER_REQUEST_TIMEOUT_MILLISECONDS_KEY)).thenReturn(valueAttribute);
        assertTrue(KafkaSensor.containsKafkaAdminClientClusterRequestTimeoutMilliseconds(cluster));
        assertEquals(11111, KafkaSensor.getKafkaAdminClientClusterRequestTimeoutMilliseconds(cluster));
    }

    @Test
    public void testGetKafkaAdminClientTopicRequestTimeoutMilliseconds() throws Exception {
        KafkaCluster cluster = Mockito.mock(KafkaCluster.class);
        Attribute valueAttribute = Mockito.mock(Attribute.class);
        // Cluster does not have timeout attribute
        Mockito.when(cluster.containsAttribute(KafkaSensor.ATTR_KAFKA_ADMIN_CLIENT_TOPIC_REQUEST_TIMEOUT_MILLISECONDS_KEY)).thenReturn(false);
        assertFalse(KafkaSensor.containsKafkaAdminClientTopicRequestTimeoutMilliseconds(cluster));
        // Cluster has topic admin client timeout attribute
        Mockito.when(valueAttribute.getValue()).thenReturn("22222");
        Mockito.when(cluster.containsAttribute(KafkaSensor.ATTR_KAFKA_ADMIN_CLIENT_TOPIC_REQUEST_TIMEOUT_MILLISECONDS_KEY)).thenReturn(true);
        Mockito.when(cluster.getAttribute(KafkaSensor.ATTR_KAFKA_ADMIN_CLIENT_TOPIC_REQUEST_TIMEOUT_MILLISECONDS_KEY)).thenReturn(valueAttribute);
        assertTrue(KafkaSensor.containsKafkaAdminClientTopicRequestTimeoutMilliseconds(cluster));
        assertEquals(22222, KafkaSensor.getKafkaAdminClientTopicRequestTimeoutMilliseconds(cluster));
    }

    @Test
    public void testGetKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds() throws Exception {
        KafkaCluster cluster = Mockito.mock(KafkaCluster.class);
        Attribute valueAttribute = Mockito.mock(Attribute.class);
        // Cluster does not have timeout attribute
        Mockito.when(cluster.containsAttribute(KafkaSensor.ATTR_KAFKA_ADMIN_CLIENT_CONSUMER_GROUP_REQUEST_TIMEOUT_MILLISECONDS_KEY)).thenReturn(false);
        assertFalse(KafkaSensor.containsKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds(cluster));
        // Cluster has consumer group admin client timeout attribute
        Mockito.when(valueAttribute.getValue()).thenReturn("33333");
        Mockito.when(cluster.containsAttribute(KafkaSensor.ATTR_KAFKA_ADMIN_CLIENT_CONSUMER_GROUP_REQUEST_TIMEOUT_MILLISECONDS_KEY)).thenReturn(true);
        Mockito.when(cluster.getAttribute(KafkaSensor.ATTR_KAFKA_ADMIN_CLIENT_CONSUMER_GROUP_REQUEST_TIMEOUT_MILLISECONDS_KEY)).thenReturn(valueAttribute);
        assertTrue(KafkaSensor.containsKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds(cluster));
        assertEquals(33333, KafkaSensor.getKafkaAdminClientConsumerGroupRequestTimeoutMilliseconds(cluster));
    }
}
