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
package com.pinterest.orion.server;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.FilterRegistration;

import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.servlet.ServletContainer;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

import com.pinterest.orion.configs.StatsConfiguration;
import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.ClusterManager;
import com.pinterest.orion.core.ClusterStateSink;
import com.pinterest.orion.core.CostCalculator;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.actions.ActionFactory;
import com.pinterest.orion.core.actions.alert.AlertFactory;
import com.pinterest.orion.core.actions.audit.ActionAuditor;
import com.pinterest.orion.core.automation.operator.Operator;
import com.pinterest.orion.core.automation.operator.OperatorFactory;
import com.pinterest.orion.core.automation.sensor.Sensor;
import com.pinterest.orion.core.automation.sensor.SensorFactory;
import com.pinterest.orion.core.configs.ClusterConfig;
import com.pinterest.orion.core.configs.PluginConfig;
import com.pinterest.orion.core.global.sensor.GlobalPluginManager;
import com.pinterest.orion.core.metrics.MetricsStore;
import com.pinterest.orion.metrics.OpenTsdbStatsPusher;
import com.pinterest.orion.metrics.StatsPusher;
import com.pinterest.orion.security.NoopAuthorizationFilter;
import com.pinterest.orion.security.OrionAuthorizationFilter;
import com.pinterest.orion.server.api.ActionEngineApi;
import com.pinterest.orion.server.api.ClusterApi;
import com.pinterest.orion.server.api.ClusterManagerApi;
import com.pinterest.orion.server.api.CustomApiFactory;
import com.pinterest.orion.server.api.MetricsApi;
import com.pinterest.orion.server.api.NodeApi;
import com.pinterest.orion.server.api.NodeRegistrationApi;
import com.pinterest.orion.server.config.OrionConf;
import com.pinterest.orion.server.config.OrionPluginConfig;
import com.pinterest.orion.utils.NetworkUtils;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.jersey.setup.JerseyContainerHolder;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.sslreload.SslReloadBundle;

public class OrionServer extends Application<OrionConf> {

  private static final Logger logger = Logger.getLogger(OrionServer.class.getCanonicalName());
  private static final String DEFAULT_URL_REWRITE_CONF_PATH = "urlrewrite.xml";

  private ClusterManager mgr;
  private HeartbeatService heartbeatService = new HeartbeatService();
  public static final MetricRegistry METRICS = SharedMetricRegistries.setDefault("METRICS");

  @Override
  public void initialize(Bootstrap<OrionConf> bootstrap) {
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    bootstrap.addBundle(new AssetsBundle("/webapp/build/", "/", "index.html"));
    bootstrap.addBundle(new SslReloadBundle());
  }

  public void additionalModules(OrionConf configuration, Environment environment) throws Exception {
    // NOT implemented for open source version
  }

  @Override
  public void run(OrionConf configuration, Environment environment) throws Exception {
    configAuthFilter(configuration, environment);
    FilterRegistration.Dynamic registration = environment.servlets().addFilter("UrlRewriteFilter",
        new UrlRewriteFilter());
    registration.addMappingForUrlPatterns(null, true, "/*");
    registration.setInitParameter("confPath", DEFAULT_URL_REWRITE_CONF_PATH);

    ActionAuditor actionAuditor = initializeActionAuditor(configuration);
    ClusterStateSink clusterStateSink = initializeClusterStateSink(configuration);
    OrionPluginConfig pluginConfigs = configuration.getPlugins();
    MetricsStore metricsStore = initializeMetricsStore(configuration);
    CostCalculator costCalculator = initializeCostCalculator(configuration);
    initializeClusterManager(pluginConfigs, actionAuditor, clusterStateSink, metricsStore,
        costCalculator);
    registerAdminAPIs(environment, configuration);
    registerAPIs(environment, configuration);
    initializeGlobalPlugins(configuration, environment);
    initializeClusters(configuration);
    initializeMetrics(configuration, environment);
    additionalModules(configuration, environment);
  }

  private void initializeGlobalPlugins(OrionConf configuration,
                                       Environment environment) throws PluginConfigurationException {
    List<PluginConfig> globalSensorConfigs = configuration.getGlobalSensorConfigs();
    GlobalPluginManager mgr = new GlobalPluginManager();
    try {
      mgr.initialize(globalSensorConfigs);
      environment.lifecycle().manage(mgr);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to initialize GlobalPluginManager", e);
    }
  }

  protected CostCalculator initializeCostCalculator(OrionConf configuration) {
    return new CostCalculator();
  }

  private MetricsStore initializeMetricsStore(OrionConf configuration) throws Exception {
    PluginConfig metricsStoreConfigs = configuration.getMetricsStoreConfigs();
    if (metricsStoreConfigs != null && metricsStoreConfigs.isEnabled()) {
      MetricsStore metricsStore = Class.forName(metricsStoreConfigs.getClazz())
          .asSubclass(MetricsStore.class).newInstance();
      metricsStore.init(metricsStoreConfigs.getConfiguration());
      return metricsStore;
    }
    return null;
  }

  protected void configAuthFilter(OrionConf configuration,
                                  Environment environment) throws Exception {
    OrionAuthorizationFilter authorizer = new NoopAuthorizationFilter();
    authorizer.configure(configuration);
    environment.jersey().register(authorizer);
    environment.jersey().register(RolesAllowedDynamicFeature.class);
  }

  protected void initializeClusterManager(OrionPluginConfig pluginConfigs,
                                          ActionAuditor actionAuditor,
                                          ClusterStateSink clusterStateSink,
                                          MetricsStore metricsStore,
                                          CostCalculator costCalculator) {
    SensorFactory sensorFactory = new SensorFactory();
    OperatorFactory operatorFactory = new OperatorFactory();
    ActionFactory actionFactory = new ActionFactory();
    AlertFactory alertFactory = new AlertFactory();
    sensorFactory.initialize(pluginConfigs.getSensorConfigs());
    operatorFactory.initialize(pluginConfigs.getOperatorConfigs());
    actionFactory.initialize(pluginConfigs.getActionConfigs());
    alertFactory.initialize(pluginConfigs.getAlertConfigs());

    mgr = new ClusterManager(sensorFactory, operatorFactory, actionFactory, alertFactory,
        actionAuditor, clusterStateSink, metricsStore, costCalculator);
  }

  protected ClusterStateSink initializeClusterStateSink(OrionConf configuration) {
    PluginConfig clusterStateSinkConfig = configuration.getClusterStateSinkConfigs();
    if (clusterStateSinkConfig != null && clusterStateSinkConfig.isEnabled()) {
      try {
        Class<?> clazz = Class.forName(clusterStateSinkConfig.getClazz());
        ClusterStateSink historySink = clazz.asSubclass(ClusterStateSink.class).newInstance();
        historySink.initialize(clusterStateSinkConfig.getConfiguration());
        return historySink;
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Failed to initialize ClusterStateSink", e);
      }
    }
    logger.warning("Cluster state sink is missing, state will not be checkpointed");
    return null;
  }

  protected ActionAuditor initializeActionAuditor(OrionConf configuration) {
    PluginConfig actionAuditorConfig = configuration.getActionAuditorConfigs();
    if (actionAuditorConfig != null && actionAuditorConfig.isEnabled()) {
      try {
        Class<?> clazz = Class.forName(actionAuditorConfig.getClazz());
        ActionAuditor actionAuditor = clazz.asSubclass(ActionAuditor.class).newInstance();
        actionAuditor.initialize(actionAuditorConfig.getConfiguration());
        return actionAuditor;
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Failed to initialize ActionAuditor", e);
      }
    }
    return null;
  }

  private void initializeMetrics(OrionConf configuration,
                                 Environment environment) throws IOException {
    if (configuration.getStatsConfiguration().isEnabled()) {
      StatsConfiguration statsConfiguration = configuration.getStatsConfiguration();
      StatsPusher stats = new OpenTsdbStatsPusher();
      stats.configure(NetworkUtils.getHostnameForLocalhost(), statsConfiguration.getMetricsPrefix(),
          statsConfiguration.getDestinationHostname(), statsConfiguration.getDestinationPort(),
          statsConfiguration.getPushInterval());
      stats.start();
    }
    heartbeatService.start();
  }

  public void initializeClusters(OrionConf configuration) throws Exception {
    ClusterManager mgr = getClusterManager();
    for (ClusterConfig clusterConfig : configuration.getClusterConfigs()) {
      String clusterId = clusterConfig.getClusterId();
      if (clusterConfig.getPlugins() != null) {
        getClusterManager().addClusterPluginConfigs(clusterId, clusterConfig.getPlugins());
      }
      List<Sensor> monitors = getClusterManager().getSensorFactory()
          .getAllSensorInstances(clusterId);
      List<Operator> operators = getClusterManager().getOperatorFactory()
          .getAllOperatorInstances(clusterId);

      Cluster cluster = ClusterTypeMap.getClusterInstance(clusterConfig.getType(), clusterId,
          clusterId, monitors, operators, mgr.getActionFactory(), mgr.getAlertFactory(),
          mgr.getActionAuditor(), mgr.getClusterStateSink(), mgr.getCostCalculator());
      if (cluster == null) {
        logger
            .severe("No cluster implementation found for cluster type:" + clusterConfig.getType());
        continue;
      }
      try {
        Map<String, Object> clusterConf = clusterConfig.getConfiguration();
        cluster.initialize(clusterConf);
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      mgr.addCluster(cluster);
      logger.info("Initialized cluster:" + cluster.getName());
    }
    mgr.loadClusterActionsFromActionAuditor();
  }

  private void registerAdminAPIs(Environment environment, OrionConf configuration) {
    final DropwizardResourceConfig jerseyConfig = new DropwizardResourceConfig(
        environment.metrics());
    JerseyContainerHolder jerseyContainerHolder = new JerseyContainerHolder(
        new ServletContainer(jerseyConfig));
    JerseyEnvironment jerseyEnvironment = new JerseyEnvironment(jerseyContainerHolder,
        jerseyConfig);

    jerseyEnvironment.register(new JacksonMessageBodyProvider(environment.getObjectMapper()));
    jerseyEnvironment.register(new NodeRegistrationApi(mgr));

    environment.admin().addServlet("admin api", jerseyContainerHolder.getContainer())
        .addMapping("/admin/api/*");
  }

  private void registerAPIs(Environment environment, OrionConf configuration) {
    environment.jersey().setUrlPattern("/api/*");
    environment.jersey().register(new ClusterManagerApi(mgr));
    environment.jersey().register(new ClusterApi(mgr));
    environment.jersey().register(new NodeApi(mgr));
    environment.jersey().register(new MetricsApi(mgr));
    environment.jersey().register(new ActionEngineApi(mgr));
    environment.jersey().register(new NodeRegistrationApi(mgr)); // TODO: this should be removed in
                                                                 // the future and the admin api
                                                                 // should be the endpoint for
                                                                 // registeration
    if (configuration.getCustomApiFactoryClasses() != null) {
      for (String factoryClass : configuration.getCustomApiFactoryClasses()) {
        CustomApiFactory instance;
        try {
          instance = Class.forName(factoryClass).asSubclass(CustomApiFactory.class).newInstance();
          instance.registerAPIs(environment, environment.jersey(), mgr);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
          logger.log(Level.SEVERE, "Failed to initialize custom APIs", e);
        }
      }
    }
  }

  public ClusterManager getClusterManager() {
    return mgr;
  }

  public static Logger getLogger() {
    return logger;
  }

  public static void main(String[] args) throws Exception {
    new OrionServer().run(args);
  }
}
