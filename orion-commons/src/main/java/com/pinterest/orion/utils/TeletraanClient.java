package com.pinterest.orion.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.logging.Logger;

public class TeletraanClient {

    private static final String TERMINATE_HOST_URL_POSTFIX = "v1/envs/memq/%s/hosts?replaceHost=false";
    private static final String REPLACE_HOST_URL_POSTFIX = "v1/envs/memq/%s/hosts?replaceHost=true";
    private static final String CHECK_HOST_STATUS_URL_POSTFIX = "v1/hosts/%s";
    private static Logger logger = Logger.getLogger(TeletraanClient.class.getCanonicalName());
    private String teletraanUrl;
    private String teletraanToken;

    public TeletraanClient(String teletraanUrl, String teletraanToken) {
        this.teletraanUrl = teletraanUrl;
        this.teletraanToken = teletraanToken;
    }

    public String getTeletraanUrl() {
        return teletraanUrl;
    }

    public String getTeletraanToken() {
        return teletraanToken;
    }

    public void setTeletraanUrl(String teletraanUrl) {
        this.teletraanUrl = teletraanUrl;
    }

    public void setTeletraanToken(String teletraanToken) {
        this.teletraanToken = teletraanToken;
    }

    protected String getCheckHostStatusUrl(String hostName) {
        return String.format(getTeletraanUrl() + CHECK_HOST_STATUS_URL_POSTFIX, hostName);
    }

    protected String getTerminateHostUrl(String clusterId) {
        return String.format(getTeletraanUrl() + TERMINATE_HOST_URL_POSTFIX, clusterId);
    }

    protected String getReplaceHostUrl(String clusterId) {
        return String.format(getTeletraanUrl() + REPLACE_HOST_URL_POSTFIX, clusterId);
    }

    /**
     * Get the token header for the Teletraan API.
     * It uses overrideTeletraanToken. If it is not set, it uses the saved token.
     * @param overrideTeletraanToken
     * @return String token header
     * @throws RuntimeException if the token is not set either in the override or saved token.
     */
    protected String getTokenHeader(String overrideTeletraanToken) throws RuntimeException {
        if (overrideTeletraanToken != null && !overrideTeletraanToken.isEmpty()) {
            return "token " + overrideTeletraanToken;
        }
        String savedToken = getTeletraanToken();
        if (savedToken == null || savedToken.isEmpty()) {
            throw new RuntimeException("Token is not set.");
        }
        return "token " + savedToken;
    }

    /**
     * Generate a StringEntity for the host to be replaced or terminated.
     * @param instanceId
     * @return StringEntity
     * @throws UnsupportedEncodingException
     */
    protected StringEntity generateHostEntity(String instanceId) throws UnsupportedEncodingException {
        JsonArray hostArray = new JsonArray();
        hostArray.add(instanceId);
        return new StringEntity(hostArray.toString());
    }


    /**
     * Replace a host in a cluster using the Teletraan API.
     * @param httpClient
     * @param instanceId
     * @param clusterId
     * @param overrideTeletraanToken
     * @return boolean indicating success or failure
     */
    public boolean replaceHost(
            CloseableHttpClient httpClient,
            String instanceId,
            String clusterId,
            String overrideTeletraanToken) {
        try {
            String replaceHostUrl = getReplaceHostUrl(clusterId);
            HttpDeleteWithBody hostReplacementRequest = new HttpDeleteWithBody(replaceHostUrl);
            hostReplacementRequest.setEntity(generateHostEntity(instanceId));
            hostReplacementRequest.setHeader("Content-Type", "application/json");
            hostReplacementRequest.setHeader("Authorization", getTokenHeader(overrideTeletraanToken));
            logger.info(String.format(
                    "Replacing host %s in cluster %s via teletraan API: %s",
                    instanceId, clusterId, replaceHostUrl));

            CloseableHttpResponse response = httpClient.execute(hostReplacementRequest);
            if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK
                    && response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_NO_CONTENT) {
                logger.severe(String.format(
                        "Failed to replace host %s in cluster %s via teletraan API: %s. Status: %s",
                        instanceId, clusterId, replaceHostUrl, response.getStatusLine().getStatusCode()));
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.severe("Error in host replacement via teletraan API: " + e);
            return false;
        }
    }

    /**
     * Terminate a host from a cluster using the Teletraan API.
     * @param httpClient
     * @param instanceId
     * @param clusterId
     * @param overrideTeletraanToken
     * @return boolean indicating success or failure
     */
    public boolean terminateHost(
            CloseableHttpClient httpClient,
            String instanceId,
            String clusterId,
            String overrideTeletraanToken) {
        try {
            String terminateHostUrl = getTerminateHostUrl(clusterId);
            HttpDeleteWithBody hostTerminationRequest = new HttpDeleteWithBody(terminateHostUrl);
            hostTerminationRequest.setEntity(generateHostEntity(instanceId));
            hostTerminationRequest.setHeader("Content-Type", "application/json");
            hostTerminationRequest.setHeader("Authorization", getTokenHeader(overrideTeletraanToken));
            logger.info(String.format(
                    "Terminating host %s from cluster %s via teletraan API: %s",
                    instanceId, clusterId, terminateHostUrl));

            CloseableHttpResponse response = httpClient.execute(hostTerminationRequest);
            if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK
                    && response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_NO_CONTENT) {
                logger.severe(String.format(
                        "Failed to terminate host %s from cluster %s via teletraan API: %s. Status: %s",
                        instanceId, clusterId, terminateHostUrl, response.getStatusLine().getStatusCode()));
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.severe("Error in host termination via teletraan API: " + e);
            return false;
        }
    }

    /**
     * Parse the response entity to check if the host is pending termination.
     * @param responseEntity HttpEntity from the response
     * @return boolean indicating if the host is pending termination
     */
    protected boolean ParseHostPendingTerminationStatus(HttpEntity responseEntity) {
        try {
            String responseEntityString = EntityUtils.toString(responseEntity);
            JsonParser parser = new JsonParser();
            JsonArray responseEntityArray = (JsonArray) parser.parse(responseEntityString);
            if (responseEntityArray.size() == 0) {
                // No record found for this host. It can be terminated already. So, return true.
                return true;
            }
            JsonObject mostRecentHostStatus = responseEntityArray.get(0).getAsJsonObject();
            return mostRecentHostStatus.get("pendingTerminate").getAsBoolean();
        } catch (Exception e) {
            logger.severe("Error in parsing host status: " + e);
            return false;
        }
    }

    /**
     * Parse the response entity to check if the host is terminated.
     * @param responseEntity HttpEntity from the response
     * @return boolean indicating if the host is terminated
     */
    protected boolean ParseHostTerminatedStatus(HttpEntity responseEntity) {
        try {
            String responseEntityString = EntityUtils.toString(responseEntity);
            JsonParser parser = new JsonParser();
            JsonArray responseEntityArray = (JsonArray) parser.parse(responseEntityString);
            if (responseEntityArray.size() == 0) {
                // No record found for this host. Host has been terminated.
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.severe("Error in parsing host status: " + e);
            return false;
        }
    }

    /**
     * Check the status of a host using the Teletraan API.
     * @param httpClient
     * @param hostName
     * @param overrideTeletraanToken
     * @return HttpEntity from the response.
     * The entity is a json array of host status objects. The first object is the most recent status.
     * If the array is empty, the host is terminated or not exist.
     */
    public HttpEntity checkHostStatus(
            CloseableHttpClient httpClient,
            String hostName,
            String overrideTeletraanToken) {
        try {
            String checkHostStatusUrl = getCheckHostStatusUrl(hostName);
            HttpGet hostStatusCheckRequest = new HttpGet(checkHostStatusUrl);
            hostStatusCheckRequest.setHeader("Content-Type", "application/json");
            hostStatusCheckRequest.setHeader("Authorization", getTokenHeader(overrideTeletraanToken));
            logger.info(String.format("Checking host status for %s via Teletraan API: %s",
                    hostName, checkHostStatusUrl));
            CloseableHttpResponse response = httpClient.execute(hostStatusCheckRequest);
            if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
                logger.severe(String.format(
                        "Failed to get host status for %s via Teletraan API: %s. Status: %s",
                        hostName, checkHostStatusUrl, response.getStatusLine().getStatusCode()));
                return null;
            }
            return response.getEntity();
        } catch (Exception e) {
            logger.severe(String.format(
                    "Error in host status check for %s via Teletraan API. Exception: %s", hostName, e
            ));
            return null;
        }
    }

    /**
     * Check if the instance is pending termination using the Teletraan API.
     * @param httpClient
     * @param hostName
     * @param overrideTeletraanToken
     * @return boolean indicating if the instance is pending termination
     */
    public boolean isInstancePendingTermination(
            CloseableHttpClient httpClient,
            String hostName,
            String overrideTeletraanToken) {
        HttpEntity hostStatusCheckEntity = checkHostStatus(httpClient, hostName, overrideTeletraanToken);
        if (hostStatusCheckEntity == null) {
            return false;
        }
        return ParseHostPendingTerminationStatus(hostStatusCheckEntity);
    }

    /**
     * Check if the instance is terminated using the Teletraan API.
     * @param httpClient
     * @param hostName
     * @param overrideTeletraanToken
     * @return boolean indicating if the instance is terminated
     */
    public boolean isInstanceTerminated(
            CloseableHttpClient httpClient,
            String hostName,
            String overrideTeletraanToken) {
        HttpEntity hostStatusCheckEntity = checkHostStatus(httpClient, hostName, overrideTeletraanToken);
        if (hostStatusCheckEntity == null) {
            return false;
        }
        return ParseHostTerminatedStatus(hostStatusCheckEntity);
    }
}
