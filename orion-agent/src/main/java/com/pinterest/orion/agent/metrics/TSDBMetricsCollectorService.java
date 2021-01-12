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
import com.pinterest.orion.agent.OrionAgent;
import com.pinterest.orion.agent.OrionAgentConfig;
import com.pinterest.orion.agent.kafka.KafkaAgent;
import com.pinterest.orion.common.Metric;
import com.pinterest.orion.common.Metrics;
import com.pinterest.orion.common.Value;
import io.dropwizard.metrics5.Gauge;
import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TSDBMetricsCollectorService implements Runnable {

  private static final Logger logger = Logger.getLogger(TSDBMetricsCollectorService.class.getName());
  private BaseAgent agent;
  private ExecutorService es;

  public TSDBMetricsCollectorService(BaseAgent agent) {
    this.agent = agent;
    es = Executors.newCachedThreadPool();
  }

  public void start() {
    es.submit(this);
  }

  @Override
  public void run() {
    try {
      collectToMetricRegistry();
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Metrics collection loop failed", e);
    }
  }

  private void addMetricToRegistry(Metric m) {
    for (Value v: m.getValues()) {
      MetricName name = new MetricName(v.getName(), m.getTags());
      for (String transmission: m.getTransmission()) {
        MetricRegistry registry = SharedMetricRegistries.getOrCreate(transmission);
        switch (v.getType()) {
          case COUNTER:
            registry.counter(name).inc(v.getValueAsLong());
            break;
          case GAUGE:
            if (!registry.getGauges().containsKey(name)) {
              registry.register(name, new SettableGauge<>(v.getValue()));
            } else {
              SettableGauge gauge = (SettableGauge) registry.getGauges().get(name);
              gauge.setValue(v.getValue());
            }
            break;
          case HISTOGRAM:
            registry.histogram(name).update(v.getValueAsLong());
            break;
        }
      }
    }
  }

  private void collectToMetricRegistry() throws InterruptedException, IOException {
    long lastMetricPollTime = 0;
    while (true) {
      try {
        agent.initializeMetricsPoll();
        logger.info("Initializing TSDB metrics collection");
        long now = System.currentTimeMillis();
        if (now - lastMetricPollTime > agent.getConfig().getMetricsPollInterval()) {
          try {
            Metrics metrics = agent.getServiceMetrics();
            for (Metric m : metrics.getMetrics()) {
              addMetricToRegistry(m);
            }
          } catch (Exception e) {
            OrionAgent.TSDB_METRICS.counter("metrics.failed").inc();
            logger.log(Level.SEVERE, "Failed to collect tsdb metrics: ", e);
          }
          lastMetricPollTime = now;
        }
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Failed to poll metrics from agent", e);
      } finally {
        agent.endMetricsPoll();
      }
      Thread.sleep(agent.getConfig().getMetricsPollInterval());
    }
  }

  private static class SettableGauge<Double> implements Gauge<Double> {

    private Double v;

    public SettableGauge(Double v) {
      this.v = v;
    };

    @Override
    public Double getValue() {
      return v;
    }

    public void setValue(Double v) {
      this.v = v;
    }
  }

  public static void main(String[] args) throws Exception {
    OrionAgentConfig config = new Yaml().loadAs(new FileInputStream(args[0]), OrionAgentConfig.class);
    KafkaAgent agent = new KafkaAgent(config);
    agent.initialize();
    agent.initializeMetricsPoll();
    TSDBMetricsCollectorService service = new TSDBMetricsCollectorService(agent);
    service.run();
  }
}
