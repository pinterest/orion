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
            registry.counter(name).inc(v.getValue());
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
            registry.histogram(name).update(v.getValue());
            break;
        }
      }
    }
  }

  private void collectToMetricRegistry() throws InterruptedException {
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
            OrionAgent.HEARTBEAT_METRICS.counter("metrics.failed").inc();
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

  private static class SettableGauge<Long> implements Gauge<Long> {

    private Long v;

    public SettableGauge(Long v) {
      this.v = v;
    };

    @Override
    public Long getValue() {
      return v;
    }

    public void setValue(Long v) {
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
