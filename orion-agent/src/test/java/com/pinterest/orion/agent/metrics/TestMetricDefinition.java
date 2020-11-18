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
package com.pinterest.orion.agent.metrics;

import com.pinterest.orion.agent.BaseAgent;
import com.pinterest.orion.agent.utils.MetricUtils;
import com.pinterest.orion.agent.utils.OrionCmd;
import com.pinterest.orion.common.MetricType;
import com.pinterest.orion.common.NodeInfo;
import com.pinterest.orion.common.StatusInfo;
import org.junit.Test;

import java.util.*;
import java.util.logging.Logger;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class TestMetricDefinition {

    @Test
    public void testFillMetricDefinitionTemplate() {
        Map<String, List<String>> entityValueMap = new HashMap<>();
        entityValueMap.put("topic", Arrays.asList("topic1", "topic2", "topic3"));
        entityValueMap.put("host", Arrays.asList("host001"));
        entityValueMap.put("topicUnused", Arrays.asList("topicUnused1", "topicUnused2"));

        MetricDefinition def = new MetricDefinition();
        def.setMetricsSource("jmx");
        def.setMetricType(MetricType.GAUGE);
        def.setInjectedEntities(Arrays.asList("topic", "host"));

        MetricInputDefinition inputDef = new MetricInputDefinition();
        Map<String, String> inputDefAttr = new HashMap<>();
        inputDefAttr.put(MetricInputDefinition.METRIC_NAME, "test.metric.name.${topic}");
        inputDefAttr.put(MetricInputDefinition.ATTRIBUTE_NAME, "test.attr.name");
        inputDef.setInputDefinitionAttributes(inputDefAttr);

        def.setInput(inputDef);

        MetricOutputDefinition outputDef = new MetricOutputDefinition();
        Map<String, String> outputTags = new HashMap<>();
        outputTags.put("topic", "${topic}");
        outputTags.put("host", "${host}");
        outputDef.setTags(outputTags);
        outputDef.setName("test.metric.name.output");
        outputDef.setTransmission(Collections.singleton("testOutputTransmission"));

        def.setOutput(outputDef);

        Set<MetricDefinition> filled = MetricUtils.fillMetricDefinitionTemplate(entityValueMap, def, null);

        assertEquals(3, filled.size()); // 3 x 1
        for (MetricDefinition newDef: filled) {
            assertFalse(newDef.getOutput().getTags().containsKey("topicUnused"));
            assertFalse(newDef.getOutput().getTags().containsValue("topicUnused1"));
            assertFalse(newDef.getOutput().getTags().containsValue("topicUnused2"));
            assertTrue(newDef.getOutput().getTags().containsKey("topic"));
            assertTrue(newDef.getOutput().getTags().containsKey("host"));
            assertFalse(newDef.getInput().getInputDefinitionAttributes().get(MetricInputDefinition.METRIC_NAME).contains("${"));
            assertTrue(newDef.getInput().getInputDefinitionAttributes().get(MetricInputDefinition.METRIC_NAME).contains("test.metric.name.topic"));
        }
    }

    @Test
    public void testFillMetricDefinitionTemplateUnusedInput() {
        Map<String, List<String>> entityValueMap = new HashMap<>();
        entityValueMap.put("topic", Arrays.asList("topic1", "topic2", "topic3"));
        entityValueMap.put("host", Arrays.asList("host001"));

        MetricDefinition def = new MetricDefinition();
        def.setMetricsSource("jmx");
        def.setMetricType(MetricType.GAUGE);
        def.setInjectedEntities(Arrays.asList("topic", "host"));

        MetricInputDefinition inputDef = new MetricInputDefinition();
        Map<String, String> inputDefAttr = new HashMap<>();
        inputDefAttr.put(MetricInputDefinition.METRIC_NAME, "test.metric.name.${topicUnused}");  // ${topicUnused} not in entityValueMap
        inputDefAttr.put(MetricInputDefinition.ATTRIBUTE_NAME, "test.attr.name");
        inputDef.setInputDefinitionAttributes(inputDefAttr);

        def.setInput(inputDef);

        MetricOutputDefinition outputDef = new MetricOutputDefinition();
        Map<String, String> outputTags = new HashMap<>();
        outputTags.put("topic", "${topic}");
        outputTags.put("host", "${host}");
        outputDef.setTags(outputTags);
        outputDef.setName("test.metric.name.output");
        outputDef.setTransmission(Collections.singleton("testOutputTransmission"));

        def.setOutput(outputDef);

        Set<MetricDefinition> filled = MetricUtils.fillMetricDefinitionTemplate(entityValueMap, def, null); // should throw RuntimeException and log severe
        assertEquals(0, filled.size());
    }

    @Test
    public void testFillMetricDefinitionTemplateUnusedOutput() {
        Map<String, List<String>> entityValueMap = new HashMap<>();
        entityValueMap.put("topic", Arrays.asList("topic1", "topic2", "topic3"));
        entityValueMap.put("host", Arrays.asList("host001"));

        MetricDefinition def = new MetricDefinition();
        def.setMetricsSource("jmx");
        def.setMetricType(MetricType.GAUGE);
        def.setInjectedEntities(Arrays.asList("topic", "host"));

        MetricInputDefinition inputDef = new MetricInputDefinition();
        Map<String, String> inputDefAttr = new HashMap<>();
        inputDefAttr.put(MetricInputDefinition.METRIC_NAME, "test.metric.name.${topic}");
        inputDefAttr.put(MetricInputDefinition.ATTRIBUTE_NAME, "test.attr.name");
        inputDef.setInputDefinitionAttributes(inputDefAttr);

        def.setInput(inputDef);

        MetricOutputDefinition outputDef = new MetricOutputDefinition();
        Map<String, String> outputTags = new HashMap<>();
        outputTags.put("topic", "${topicUnused}");  // ${topicUnused} not in entityValueMap
        outputTags.put("host", "${host}");
        outputDef.setTags(outputTags);
        outputDef.setName("test.metric.name.output");
        outputDef.setTransmission(Collections.singleton("testOutputTransmission"));

        def.setOutput(outputDef);

        Set<MetricDefinition> filled = MetricUtils.fillMetricDefinitionTemplate(entityValueMap, def, null); // should throw RuntimeException and log severe
        assertEquals(0, filled.size());
    }

    @Test
    public void testFillMetricDefinitionTemplateAdditionalEntity() {
        Map<String, List<String>> entityValueMap = new HashMap<>();
        entityValueMap.put("topic", Arrays.asList("topic1", "topic2", "topic3"));
        entityValueMap.put("host", Arrays.asList("host001"));
        entityValueMap.put("entityUsed", Arrays.asList("entityVal1", "entityVal1", "entityVal2"));
        entityValueMap.put("topicUnused", Arrays.asList("topicUnused1", "topicUnused2"));

        MetricDefinition def = new MetricDefinition();
        def.setMetricsSource("jmx");
        def.setMetricType(MetricType.GAUGE);
        def.setInjectedEntities(Arrays.asList("topic", "host", "entityUsed"));

        MetricInputDefinition inputDef = new MetricInputDefinition();
        Map<String, String> inputDefAttr = new HashMap<>();
        inputDefAttr.put(MetricInputDefinition.METRIC_NAME, "test.metric.name.${topic}.${entityUsed}");
        inputDefAttr.put(MetricInputDefinition.ATTRIBUTE_NAME, "test.attr.name");
        inputDef.setInputDefinitionAttributes(inputDefAttr);

        def.setInput(inputDef);

        MetricOutputDefinition outputDef = new MetricOutputDefinition();
        Map<String, String> outputTags = new HashMap<>();
        outputTags.put("topic", "${topic}");
        outputTags.put("host", "${host}");
        outputTags.put("entityUsed", "${entityUsed}");
        outputDef.setTags(outputTags);
        outputDef.setName("test.metric.name.output");
        outputDef.setTransmission(Collections.singleton("testOutputTransmission"));

        def.setOutput(outputDef);

        Set<MetricDefinition> filled = MetricUtils.fillMetricDefinitionTemplate(entityValueMap, def, null);

        assertEquals(6, filled.size()); // 3 x 1 x 2
        for (MetricDefinition newDef: filled) {
            assertFalse(newDef.getOutput().getTags().containsKey("topicUnused"));
            assertFalse(newDef.getOutput().getTags().containsValue("topicUnused1"));
            assertFalse(newDef.getOutput().getTags().containsValue("topicUnused2"));
            assertTrue(newDef.getOutput().getTags().containsKey("topic"));
            assertTrue(newDef.getOutput().getTags().containsKey("host"));
            assertTrue(newDef.getOutput().getTags().containsKey("entityUsed"));
            assertFalse(newDef.getInput().getInputDefinitionAttributes().get(MetricInputDefinition.METRIC_NAME).contains("${"));
            assertTrue(newDef.getInput().getInputDefinitionAttributes().get(MetricInputDefinition.METRIC_NAME).contains("test.metric.name.topic"));
            assertTrue(newDef.getInput().getInputDefinitionAttributes().get(MetricInputDefinition.METRIC_NAME).contains("entityVal"));
        }
    }

    @Test
    public void testGenerateTasks() throws Exception {
        Map<String, List<String>> entityValueMap = new HashMap<>();
        entityValueMap.put("topic", Arrays.asList("topic1", "topic2", "topic3"));
        entityValueMap.put("host", Arrays.asList("host001"));
        entityValueMap.put("entityUsed", Arrays.asList("entityVal1", "entityVal1", "entityVal2"));
        entityValueMap.put("topicUnused", Arrays.asList("topicUnused1", "topicUnused2"));

        MetricDefinition def1 = new MetricDefinition();
        def1.setMetricsSource("jmx");
        def1.setMetricType(MetricType.GAUGE);
        def1.setInjectedEntities(Arrays.asList("topic", "host", "entityUsed"));

        MetricInputDefinition inputDef = new MetricInputDefinition();
        Map<String, String> inputDefAttr = new HashMap<>();
        inputDefAttr.put(MetricInputDefinition.METRIC_NAME, "test.metric.name.${topic}.${entityUsed}");
        inputDefAttr.put(MetricInputDefinition.ATTRIBUTE_NAME, "test.attr.name");
        inputDef.setInputDefinitionAttributes(inputDefAttr);

        def1.setInput(inputDef);

        MetricOutputDefinition outputDef = new MetricOutputDefinition();
        Map<String, String> outputTags = new HashMap<>();
        outputTags.put("topic", "${topic}");
        outputTags.put("host", "${host}");
        outputTags.put("entityUsed", "${entityUsed}");
        outputDef.setTags(outputTags);
        outputDef.setName("test.metric.name.output");
        outputDef.setTransmission(Collections.singleton("testOutputTransmission"));

        def1.setOutput(outputDef);

        MetricDefinition def2 = new MetricDefinition();
        def2.setMetricsSource("jmx");
        def2.setMetricType(MetricType.GAUGE);
        def2.setInjectedEntities(Arrays.asList("topic", "host", "entityUsed"));

        MetricInputDefinition inputDef2 = new MetricInputDefinition();
        Map<String, String> inputDefAttr2 = new HashMap<>();
        inputDefAttr.put(MetricInputDefinition.METRIC_NAME, "test.metric.name2.${topic}.${entityUsed}");
        inputDefAttr.put(MetricInputDefinition.ATTRIBUTE_NAME, "test.attr.name2");
        inputDef.setInputDefinitionAttributes(inputDefAttr2);

        def2.setInput(inputDef2);

        MetricOutputDefinition outputDef2 = new MetricOutputDefinition();
        outputDef2.setTags(outputTags);
        outputDef2.setName("test.metric.name2.output");
        outputDef2.setTransmission(Collections.singleton("testOutputTransmission"));

        def2.setOutput(outputDef2);

        List<MetricDefinition> defs = Arrays.asList(def1, def2);

        Set<MetricRetrieverTask> tasks = MetricUtils.getTasksFromMetricDefinitionTemplates(entityValueMap, defs, new BaseAgent(null) {
            @Override
            public void addToTasksFromDefinition(Set<MetricRetrieverTask> task, MetricDefinition newDef) throws Exception {
                task.add(new JMXMetricRetreiverTask(newDef, null));
            }

            @Override
            protected int getMetricsPort() {
                return 9999;
            }

            @Override
            protected Logger getLogger() {
                return null;
            }

            @Override
            public List<String> getEntityValues(String entity) {
                return null;
            }

            @Override
            public StatusInfo getAgentStatus() throws Exception {
                return null;
            }

            @Override
            public OrionCmd startService() throws Exception {
                return null;
            }

            @Override
            public OrionCmd stopService() throws Exception {
                return null;
            }

            @Override
            public OrionCmd restartService() throws Exception {
                return null;
            }

            @Override
            public OrionCmd updateConfigs() throws Exception {
                return null;
            }

            @Override
            public OrionCmd upgradeAgent() throws Exception {
                return null;
            }

            @Override
            public OrionCmd upgradeService() throws Exception {
                return null;
            }

            @Override
            public StatusInfo getServiceStatus() throws Exception {
                return null;
            }

            @Override
            public NodeInfo getNodeInfo() throws Exception {
                return null;
            }

            @Override
            public OrionCmd probeNetstat() throws Exception {
                return null;
            }
        });

        assertEquals(12, tasks.size());
    }
}
