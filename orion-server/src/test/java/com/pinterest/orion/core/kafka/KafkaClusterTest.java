package com.pinterest.orion.core.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


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

    @Test
    public void testKafkaClusterCachedTopicSetLogic() throws Exception {
        KafkaCluster cluster = new KafkaCluster("", "", Collections.emptyList(),
                Collections.emptyList(), null, null, null, null, null);
        // Mock all components in listTopics call path
        AdminClient adminClient = Mockito.mock(AdminClient.class);
        ListTopicsResult listTopicsResult = mock(ListTopicsResult.class);
        KafkaFuture<Set<String>> kafkaFuture = mock(KafkaFuture.class);
        Set<String> topicSet = Collections.emptySet();
        Mockito.when(kafkaFuture.get(12345, TimeUnit.MILLISECONDS)).thenReturn(topicSet);
        Mockito.when(listTopicsResult.names()).thenReturn(kafkaFuture);
        Mockito.when(adminClient.listTopics(any())).thenReturn(listTopicsResult);
        cluster.setAdminClient(adminClient);
        cluster.setClusterId("Test_Cluster");
        // Initial call. adminClient.listTopics is triggered
        cluster.getKafkaTopicSet(12345);
        verify(adminClient, times(1)).listTopics(any());
        // Set an extremely large interval. adminClient.listTopics is not triggered
        cluster.setCachedTopicSetRefreshIntervalMill(3600_000L); // 1 hour
        cluster.getKafkaTopicSet(12345);
        verify(adminClient, times(1)).listTopics(any());
        // Reset lastUpdateTime. adminClient.listTopics is forced to be triggered
        cluster.resetCachedTopicSetLastUpdateTime();
        cluster.getKafkaTopicSet(12345);
        verify(adminClient, times(2)).listTopics(any());
        // Reach TTL by setting an extremely low interval. adminClient.listTopics is triggered
        cluster.setCachedTopicSetRefreshIntervalMill(0);
        cluster.getKafkaTopicSet(12345);
        verify(adminClient, times(3)).listTopics(any());
    }
}
