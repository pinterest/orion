/*******************************************************************************
 * Copyright 2020 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.pinterest.orion.utils;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Map;

import javax.naming.AuthenticationException;
import javax.net.ssl.SSLContext;

import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

public class NetworkUtils {

  private static SSLConnectionSocketFactory factory;

  public static String getIPFromHostnameIfAvailable(String hostname) {
    try {
      return InetAddress.getByName(hostname).getHostAddress();
    } catch (UnknownHostException e) {
      return hostname;
    }
  }
  
  public static String getHostnameFromIpIfAvailable(String ip) {
    try {
      return InetAddress.getByName(ip).getHostName();
    } catch (UnknownHostException e) {
      return ip;
    }
  }
  
  public static String getHostnameForLocalhost() {
    String hostName;
    try {
      hostName = InetAddress.getLocalHost().getHostName();
    } catch (Exception e) {
      // fall back to env var.
      hostName = System.getenv("HOSTNAME");
    }
    return hostName;
  }
  
  public static String getIpForHost() {
    try {
      return InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      return "0.0.0.0";
    }
  }

  public static void initSSLConnectionFactory(Map<String, String> connectionProperties)
      throws KeyManagementException,
             NoSuchAlgorithmException,
             UnrecoverableKeyException,
             KeyStoreException,
             CertificateException,
             IOException {
    SSLContext sslContext;
    if(connectionProperties != null){
      SSLContextBuilder builder = new SSLContextBuilder();
      builder.loadKeyMaterial(new File(connectionProperties.get("ssl.keystore.location")),
          connectionProperties.get("ssl.keystore.password").toCharArray(),
          connectionProperties.get("ssl.key.password").toCharArray());
      builder.loadTrustMaterial(new File(connectionProperties.get("ssl.truststore.location")),
          connectionProperties.get("ssl.truststore.password").toCharArray());
      sslContext = builder.build();
    } else {
      sslContext = SSLContext.getDefault();
    }
    factory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
  }

  /**
   * Make an HTTP Put request on the supplied URI and return the response entity
   * as {@link String}
   *
   * @param uri
   * @return
   * @throws IOException
   * @throws AuthenticationException
   */
  public static String makePutRequest(String uri, byte[] payload) throws IOException,
                                                                         AuthenticationException {
    HttpPut putRequest = new HttpPut(uri);
    try {
      putRequest.setHeader("content-type", "application/json");
      String payloadString = new String(payload);
      putRequest.setEntity(new StringEntity(payloadString));
      CloseableHttpClient client = getClient(putRequest);
      CloseableHttpResponse response = makeRequest(client, putRequest);
      String entity = null;
      int statusCode = response.getStatusLine().getStatusCode();
      switch (statusCode) {
      case 200:
        entity = EntityUtils.toString(response.getEntity());
        break;
      case 204:
        break;
      case 500:
        throw new AuthenticationException();
      default:
        String message = response.getEntity() != null ? ("\nmessage: " + EntityUtils.toString(response.getEntity())): null;
        throw new IOException("Request failed(" + statusCode + ") reason: "
            + response.getStatusLine().getReasonPhrase() + message);
      }
      response.close();
      client.close();
      return entity;
    } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
      throw new IOException(e);
    }
  }

  /**
   * Make an HTTP POST request on the supplied URI and return the response entity
   * as {@link String}
   *
   * @param uri
   * @return
   * @throws IOException
   */

  public static String makePostRequest(String uri,
                                       byte[] payload,
                                       boolean closeClient) throws IOException {
    HttpPost postRequest = new HttpPost(uri);
    CloseableHttpClient client = getClient(postRequest);
    try {
      postRequest.setHeader("content-type", "application/json");
      if (payload != null) {
        String payloadStrig = new String(payload);
        postRequest.setEntity(new StringEntity(payloadStrig));
      }
      CloseableHttpResponse response = makeRequest(client, postRequest);
      if (response.getStatusLine().getStatusCode() != 200
          && response.getStatusLine().getStatusCode() != 204) {
        throw new IOException("Request failed(" + response.getStatusLine().getStatusCode()
            + ") reason:" + response.getStatusLine().getReasonPhrase());
      }
      String entity = null;
      if (response.getStatusLine().getStatusCode() == 200) {
        entity = EntityUtils.toString(response.getEntity());
      }
      response.close();
      if (closeClient) {
        client.close();
      }
      return entity;
    } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException
        | IOException e) {
      throw new IOException(e);
    }
  }

  /**
   * Copied from
   * https://github.com/srotya/sidewinder/blob/development/core/src/test/java/com/srotya
   * /sidewinder/core/qa/TestUtils.java
   * under Apache 2.0 license
   *
   * @param client
   */
  public static CloseableHttpResponse makeRequest(CloseableHttpClient client,
                                                  HttpRequestBase request)
      throws KeyManagementException,
             NoSuchAlgorithmException,
             KeyStoreException,
             IOException {
    return client.execute(request);
  }

  /**
   * Copied from
   * https://github.com/srotya/sidewinder/blob/development/core/src/test/java/com/srotya
   * /sidewinder/core/qa/TestUtils.java
   * under Apache 2.0 license
   */
  public static CloseableHttpClient buildClient(int connectTimeout,
                                                int requestTimeout,
                                                CredentialsProvider provider) {
    HttpClientBuilder clientBuilder = HttpClients.custom();
    if (provider != null) {
      clientBuilder.setDefaultCredentialsProvider(provider);
    }
    RequestConfig config = RequestConfig.custom().setConnectTimeout(connectTimeout)
        .setConnectionRequestTimeout(requestTimeout).setAuthenticationEnabled(true).build();
    return clientBuilder.setDefaultRequestConfig(config).build();
  }

  public static CloseableHttpClient buildClientWithSSL(int connectTimeout,
                                                       int requestTimeout,
                                                       CredentialsProvider provider) {
    HttpClientBuilder clientBuilder = HttpClients.custom();
    if (provider != null) {
      clientBuilder.setDefaultCredentialsProvider(provider);
    }
    clientBuilder.setSSLSocketFactory(factory);
    RequestConfig config = RequestConfig.custom().setConnectTimeout(connectTimeout)
        .setConnectionRequestTimeout(requestTimeout).setAuthenticationEnabled(true).build();
    return clientBuilder.setDefaultRequestConfig(config).build();
  }

  public static boolean checkPort(int port) {
    Socket sc = null;
    try {
      sc = new Socket("localhost", port);
      return true;
    } catch (IOException e) {
    } finally {
      if (sc != null) {
        try {
          sc.close();
        } catch (IOException e) {
        }
      }
    }
    return false;
  }

  public static CloseableHttpClient getClient(HttpRequestBase r) {
    if (r.getURI().getScheme().equals("https")) {
      return buildClientWithSSL(1000, 1000, null);
    } else {
      return buildClient(1000, 1000, null);
    }
  }
}
