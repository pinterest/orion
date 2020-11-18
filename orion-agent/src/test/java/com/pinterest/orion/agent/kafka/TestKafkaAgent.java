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
package com.pinterest.orion.agent.kafka;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.pinterest.orion.agent.BaseAgent;
import com.pinterest.orion.agent.OrionAgentConfig;
import com.pinterest.orion.agent.kafka.KafkaAgent;
import com.pinterest.orion.agent.utils.OrionCmd;
import com.pinterest.orion.common.CmdResult;
import com.pinterest.orion.common.NodeCmd;

public class TestKafkaAgent {

  @Before
  public void setup() {
    agentConfigs.put(KafkaAgent.KAFKA_SERVER_PROPERTIES,
        "src/test/resources/test.kafkaserver.properties");
    agentConfigs.put(OrionAgentConfig.CMD_LOG_DIRECTORY,
        "target/test" + UUID.randomUUID().toString());
    config.setAgentConfigs(agentConfigs);
  }

  private OrionAgentConfig config = new OrionAgentConfig();
  private Map<String, String> agentConfigs = new HashMap<>();

  @Test
  public void testKafkaPlainTextListenerPortExtraction() {
    Properties kafkaBrokerProperties = new Properties();
    kafkaBrokerProperties.put("listeners", "PLAINTEXT://:9092,SSL://:9093");
    assertEquals(9092, KafkaAgent.getKafkaPlainTextListenerPort(kafkaBrokerProperties));
  }

  @Test
  public void testNetstatProbe() throws Exception {
    BaseAgent agent = new KafkaAgent(config);
    NodeCmd currentCommand = new NodeCmd(UUID.randomUUID().toString(), "probeNetstat");
    agent.setCurrentCommand(currentCommand);
    OrionCmd probeNetstat = agent.probeNetstat();
    CmdResult result = probeNetstat.get();
    assertEquals(0, result.getExitCode());
  }

  @Test
  public void getKafkaUptime() throws Exception {

    KafkaAgent agent = new KafkaAgent(config);
    long shouldBeNegativeOne = agent.getKafkaUptime().get();
    assertEquals(-1L , shouldBeNegativeOne);
  }

  @Test
  public void parseKafkaUptimeLine() {
    assertNull(KafkaAgent.parseKafkaUptimeLine("gibberish"));
    assertNull(KafkaAgent.parseKafkaUptimeLine("gibberish with spaces"));
    assertEquals("123", KafkaAgent.parseKafkaUptimeLine("123 java some/class/path -DsomeKey=someValue kafka.Kafka some/config.properties"));
  }

}