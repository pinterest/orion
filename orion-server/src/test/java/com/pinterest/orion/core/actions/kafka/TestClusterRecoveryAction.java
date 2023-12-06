package com.pinterest.orion.core.actions.kafka;

import com.google.common.collect.Sets;
import com.pinterest.orion.core.Attribute;
import com.pinterest.orion.core.Cluster;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;

import static com.pinterest.orion.core.actions.kafka.ClusterRecoveryAction.removeRecoveringNodesFromCandidates;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class TestClusterRecoveryAction {

    @Test
    public void testRemoveRecoveringNodesFromCandidatesClearCandidates() {
        Set<String> testCandidates = new HashSet<String>() {{
            add("1");
            add("2");
        }};
        Cluster testCluster = Mockito.mock(Cluster.class);
        Mockito.when(testCluster.getClusterId()).thenReturn("test_cluster");
        Set<String> testRecoveringNodes = new HashSet<String>() {{
            add("1");
            add("2");
        }};
        Attribute testRecoveringNodesAttribute = Mockito.mock(Attribute.class);
        Mockito.when(testRecoveringNodesAttribute.getValue()).thenReturn(testRecoveringNodes);
        Mockito.when(testRecoveringNodesAttribute.getUpdateTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(testCluster.getAttribute(ClusterRecoveryAction.ATTR_RECOVERING_NODES))
                .thenReturn(testRecoveringNodesAttribute);
        removeRecoveringNodesFromCandidates(testCandidates, testCluster);
        // testCandidates should be cleared.
        // testRecoveringNodes should not be updated.
        assertEquals(testCandidates, new HashSet<String>());
        verify(testCluster, never())
                .setAttribute(Mockito.eq(ClusterRecoveryAction.ATTR_RECOVERING_NODES), Mockito.any());
    }

    @Test
    public void testRemoveRecoveringNodesFromCandidatesRecoveringNodeTimeout() {
        Set<String> testCandidates = new HashSet<String>() {{
            add("1");
            add("2");
        }};
        Cluster testCluster = Mockito.mock(Cluster.class);
        Mockito.when(testCluster.getClusterId()).thenReturn("test_cluster");
        Set<String> testRecoveringNodes = new HashSet<String>() {{
            add("1");
            add("3");
        }};
        Attribute testRecoveringNodesAttribute = Mockito.mock(Attribute.class);
        Mockito.when(testRecoveringNodesAttribute.getValue()).thenReturn(testRecoveringNodes);
        Mockito.when(testRecoveringNodesAttribute.getUpdateTimestamp()).thenReturn(0L);
        Mockito.when(testCluster.getAttribute(ClusterRecoveryAction.ATTR_RECOVERING_NODES))
                .thenReturn(testRecoveringNodesAttribute);
        removeRecoveringNodesFromCandidates(testCandidates, testCluster);
        // testCandidates should not be updated because testRecoveringNodes reaches TTL.
        // testRecoveringNodes should be updated to testCandidates values.
        assertEquals(testCandidates, new HashSet<String>() {{
            add("1");
            add("2");
        }});
        ArgumentCaptor<Set> argument = ArgumentCaptor.forClass(Set.class);
        verify(testCluster, Mockito.times(1))
                .setAttribute(Mockito.eq(ClusterRecoveryAction.ATTR_RECOVERING_NODES), argument.capture());
        assertEquals(argument.getValue(), testCandidates);
    }

    @Test
    public void testRemoveRecoveringNodesFromCandidatesUpdateRecoveringNodeAndCandidates() {
        Set<String> testCandidates = new HashSet<String>() {{
            add("1");
            add("2");
            add("3");
        }};
        Cluster testCluster = Mockito.mock(Cluster.class);
        Mockito.when(testCluster.getClusterId()).thenReturn("test_cluster");
        Set<String> testRecoveringNodes = new HashSet<String>() {{
            add("2");
            add("3");
            add("4");
        }};
        Attribute testRecoveringNodesAttribute = Mockito.mock(Attribute.class);
        Mockito.when(testRecoveringNodesAttribute.getValue()).thenReturn(testRecoveringNodes);
        Mockito.when(testRecoveringNodesAttribute.getUpdateTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(testCluster.getAttribute(ClusterRecoveryAction.ATTR_RECOVERING_NODES))
                .thenReturn(testRecoveringNodesAttribute);
        removeRecoveringNodesFromCandidates(testCandidates, testCluster);
        // testCandidates should be updated to the difference between testCandidates and testRecoveringNodes.
        // testRecoveringNodes should be updated to the union of testCandidates and testRecoveringNodes.
        assertEquals(testCandidates, new HashSet<String>() {{
            add("1");
        }});
        ArgumentCaptor<Set> argument = ArgumentCaptor.forClass(Set.class);
        verify(testCluster, Mockito.times(1))
                .setAttribute(Mockito.eq(ClusterRecoveryAction.ATTR_RECOVERING_NODES), argument.capture());
        assertEquals(argument.getValue(), Sets.union(testCandidates, testRecoveringNodes));
    }
}
