package com.pinterest.orion.server;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.reflections.Reflections;

import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.ClusterStateSink;
import com.pinterest.orion.core.ClusterType;
import com.pinterest.orion.core.CostCalculator;
import com.pinterest.orion.core.actions.ActionFactory;
import com.pinterest.orion.core.actions.alert.AlertFactory;
import com.pinterest.orion.core.actions.audit.ActionAuditor;
import com.pinterest.orion.core.automation.operator.Operator;
import com.pinterest.orion.core.automation.sensor.Sensor;
import com.pinterest.orion.core.hbase.HBaseCluster;
import com.pinterest.orion.core.kafka.KafkaCluster;
import com.pinterest.orion.core.memq.MemqCluster;

public class ClusterTypeMap {

  private static final Logger logger = Logger.getLogger(ClusterTypeMap.class.getCanonicalName());

  public static final Map<String, Class<? extends Cluster>> clusterTypeMap = new ConcurrentHashMap<>();
  private static final Map<String, Integer> classPriorityMap = new HashMap<>();

  static {
    clusterTypeMap.put("kafka", KafkaCluster.class);
    clusterTypeMap.put("memq", MemqCluster.class);
    clusterTypeMap.put("hbase", HBaseCluster.class);

    try {
      Reflections reflections = new Reflections("com.pinterest.orion.core");
      Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(ClusterType.class);
      for (Class<?> annotatedClass : annotatedClasses) {
        ClusterType plugin = annotatedClass.getAnnotation(ClusterType.class);
        if (plugin == null) {
          logger.severe("Plugin info null:" + plugin);
          continue;
        }
        registerHandlerClassWithAlias(annotatedClass.asSubclass(Cluster.class), plugin);
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to pull clustertypes from annotations", e);
    }
  }

  private static void registerHandlerClassWithAlias(Class<? extends Cluster> annotatedClass,
                                                    ClusterType plugin) {
    Integer integer = classPriorityMap.get(plugin.name());
    if (integer != null && integer > plugin.priority()) {
      return;
    }
    classPriorityMap.put(plugin.name(), plugin.priority());
    clusterTypeMap.put(plugin.name(), annotatedClass);
  }

  public static Cluster getClusterInstance(String clusterType,
                                           String clusterId,
                                           String clusterName,
                                           List<Sensor> monitors,
                                           List<Operator> operators,
                                           ActionFactory actionFactory,
                                           AlertFactory alertFactory,
                                           ActionAuditor actionAuditor,
                                           ClusterStateSink clusterStateSink,
                                           CostCalculator costCalculator) throws NoSuchMethodException,
                                                                          SecurityException,
                                                                          InstantiationException,
                                                                          IllegalAccessException,
                                                                          IllegalArgumentException,
                                                                          InvocationTargetException {
    Class<? extends Cluster> clzz = clusterTypeMap.get(clusterType.toLowerCase());
    if (clzz == null) {
      return null;
    }
    Constructor<? extends Cluster> constructor = clzz.getConstructor(String.class, String.class,
        List.class, List.class, ActionFactory.class, AlertFactory.class, ActionAuditor.class,
        ClusterStateSink.class, CostCalculator.class);
    return constructor.newInstance(clusterId, clusterName, monitors, operators, actionFactory,
        alertFactory, actionAuditor, clusterStateSink, costCalculator);
  }

}
