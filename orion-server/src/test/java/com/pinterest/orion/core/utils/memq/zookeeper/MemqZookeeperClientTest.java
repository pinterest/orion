package com.pinterest.orion.core.utils.memq.zookeeper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Verifies that the chroot in a zk connection string is parsed out into a Curator namespace.
 * This is the core of the fix: the chroot must NOT stay in the connection string (where it is
 * dropped on dynamic reconfiguration) but be applied as a client-side namespace instead.
 */
public class MemqZookeeperClientTest {

  @Test
  public void parsesSingleHostWithChroot() {
    String zkUrl = "zk-host.example.com:2181/memq/cluster01";
    assertEquals("zk-host.example.com:2181", MemqZookeeperClient.parseConnectString(zkUrl));
    assertEquals("memq/cluster01", MemqZookeeperClient.parseNamespace(zkUrl));
  }

  @Test
  public void parsesMultiHostWithChroot() {
    String zkUrl = "10.12.90.128:2181,10.12.72.198:2181,10.12.7.82:2181/memq/cluster09";
    assertEquals("10.12.90.128:2181,10.12.72.198:2181,10.12.7.82:2181",
        MemqZookeeperClient.parseConnectString(zkUrl));
    assertEquals("memq/cluster09", MemqZookeeperClient.parseNamespace(zkUrl));
  }

  @Test
  public void parsesNestedChroot() {
    String zkUrl = "host:2181/a/b/c";
    assertEquals("host:2181", MemqZookeeperClient.parseConnectString(zkUrl));
    assertEquals("a/b/c", MemqZookeeperClient.parseNamespace(zkUrl));
  }

  @Test
  public void handlesNoChroot() {
    String zkUrl = "host:2181";
    assertEquals("host:2181", MemqZookeeperClient.parseConnectString(zkUrl));
    assertNull(MemqZookeeperClient.parseNamespace(zkUrl));
  }

  @Test
  public void handlesTrailingSlashAsNoChroot() {
    String zkUrl = "host:2181/";
    assertEquals("host:2181", MemqZookeeperClient.parseConnectString(zkUrl));
    assertNull(MemqZookeeperClient.parseNamespace(zkUrl));
  }
}
