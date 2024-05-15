package com.pinterest.orion.utils;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.HttpURLConnection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TestTeletraanClient {

    private TeletraanClient teletraanClient = new TeletraanClient(
            "https://teletraan.com/", "test_token");

    @Test
    public void testGetTerminateHostUrl() {
        assertEquals(teletraanClient.getTerminateHostUrl("test_cluster"),
                "https://teletraan.com/v1/envs/memq/test_cluster/hosts?replaceHost=false");
    }

    @Test
    public void testGetReplaceHostUrl() {
        assertEquals(teletraanClient.getReplaceHostUrl("test_cluster"),
                "https://teletraan.com/v1/envs/memq/test_cluster/hosts?replaceHost=true");
    }

    @Test
    public void testGetCheckHostStatusUrl() {
        assertEquals(teletraanClient.getCheckHostStatusUrl("test_host"),
                "https://teletraan.com/v1/hosts/test_host");
    }

    @Test
    public void testGetToeknHeader() {
        assertEquals(teletraanClient.getTokenHeader("test_token"), "token test_token");
    }

    @Test
    public void testTerminateHost() throws Exception {
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        String instanceId = "test_instance_id";
        String clusterId = "test_cluster";
        Mockito.when(httpClient.execute(Mockito.any())).thenReturn(response);
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
        Mockito.when(statusLine.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        assertTrue(teletraanClient.terminateHost(httpClient, instanceId, clusterId, "test_token"));
        verify(httpClient, times(1)).execute(Mockito.any());
    }

    @Test
    public void testTerminateHostHttpCallFailure() throws Exception {
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        String instanceId = "test_instance_id";
        String clusterId = "test_cluster";
        Mockito.when(httpClient.execute(Mockito.any())).thenReturn(response);
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
        Mockito.when(statusLine.getStatusCode()).thenReturn(HttpURLConnection.HTTP_GATEWAY_TIMEOUT);
        assertFalse(teletraanClient.terminateHost(httpClient, instanceId, clusterId, "test_token"));
        verify(httpClient, times(1)).execute(Mockito.any());
    }

    @Test
    public void testIsInstancePendingTermination() throws Exception {
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        String hostName = "test_host";
        Mockito.when(httpClient.execute(Mockito.any())).thenReturn(response);
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
        Mockito.when(statusLine.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        Mockito.when(response.getEntity()).thenReturn(new StringEntity("[]"));
        assertTrue(teletraanClient.isInstancePendingTermination(httpClient, hostName, "test_token"));
        verify(httpClient, times(1)).execute(Mockito.any());
    }

    @Test
    public void testIsInstancePendingTerminationHttpCallFailure() throws Exception {
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        String hostName = "test_host";
        Mockito.when(httpClient.execute(Mockito.any())).thenReturn(response);
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
        Mockito.when(statusLine.getStatusCode()).thenReturn(HttpURLConnection.HTTP_GATEWAY_TIMEOUT);
        assertFalse(teletraanClient.isInstancePendingTermination(httpClient, hostName, "test_token"));
        verify(httpClient, times(1)).execute(Mockito.any());
    }

    @Test
    public void testParsePendingTerminationStatus() throws Exception {
        String testResponseEntityString = "[]";
        assertEquals(teletraanClient.ParseHostPendingTerminationStatus(new StringEntity(testResponseEntityString)), true);
        testResponseEntityString = "[{'pendingTerminate': True, 'hostName': 'hostName', 'groupName': 'groupName', 'ip': '8.8.8.8', 'hostId': 'i-0000000000000000', 'accountId': '00000000000', 'createDate': 1712018950930, 'lastUpdateDate': 1712018950930, 'state': 'ACTIVE', 'canRetire': 0}]";
        assertEquals(teletraanClient.ParseHostPendingTerminationStatus(new StringEntity(testResponseEntityString)), true);
        testResponseEntityString = "[{'pendingTerminate': False, 'hostName': 'hostName', 'groupName': 'groupName', 'ip': '8.8.8.8', 'hostId': 'i-0000000000000000', 'accountId': '00000000000', 'createDate': 1712018950930, 'lastUpdateDate': 1712018950930, 'state': 'ACTIVE', 'canRetire': 0}]";
        assertEquals(teletraanClient.ParseHostPendingTerminationStatus(new StringEntity(testResponseEntityString)), false);
    }

    @Test
    public void testParseTerminatedStatus() throws Exception {
        String testResponseEntityString = "[]";
        assertEquals(teletraanClient.ParseHostTerminatedStatus(new StringEntity(testResponseEntityString)), true);
        testResponseEntityString = "[{'pendingTerminate': True, 'hostName': 'hostName', 'groupName': 'groupName', 'ip': '8.8.8.8', 'hostId': 'i-0000000000000000', 'accountId': '00000000000', 'createDate': 1712018950930, 'lastUpdateDate': 1712018950930, 'state': 'ACTIVE', 'canRetire': 0}]";
        assertEquals(teletraanClient.ParseHostTerminatedStatus(new StringEntity(testResponseEntityString)), false);
    }

    @Test
    public void testReplaceHost() throws Exception {
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        String instanceId = "test_instance_id";
        String clusterId = "test_cluster";
        Mockito.when(httpClient.execute(Mockito.any())).thenReturn(response);
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
        Mockito.when(statusLine.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        assertTrue(teletraanClient.replaceHost(httpClient, instanceId, clusterId, "test_token"));
        verify(httpClient, times(1)).execute(Mockito.any());
    }

    @Test
    public void testReplaceHostWithHttpFailure() throws Exception {
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        String instanceId = "test_instance_id";
        String clusterId = "test_cluster";
        Mockito.when(httpClient.execute(Mockito.any())).thenReturn(response);
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
        Mockito.when(statusLine.getStatusCode()).thenReturn(HttpURLConnection.HTTP_GATEWAY_TIMEOUT);
        assertFalse(teletraanClient.replaceHost(httpClient, instanceId, clusterId, "test_token"));
        verify(httpClient, times(1)).execute(Mockito.any());
    }
}
