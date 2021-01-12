package com.pinterest.orion.server;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.ClusterStateSink;
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

  public static final Map<String, Class<? extends Cluster>> clusterTypeMap = new ConcurrentHashMap<>();

  static {
    clusterTypeMap.put("Kafka", KafkaCluster.class);
    clusterTypeMap.put("MemQ", MemqCluster.class);
    clusterTypeMap.put("HBase", HBaseCluster.class);
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
    Class<? extends Cluster> clzz = clusterTypeMap.get(clusterType);
    if (clzz == null) {
      return null;
    }
    Constructor<? extends Cluster> constructor = clzz.getConstructor(String.class, String.class,
        List.class, List.class, ActionFactory.class, AlertFactory.class, ActionAuditor.class,
        ClusterStateSink.class, CostCalculator.class);
    return constructor.newInstance(clusterId, clusterName, monitors, operators, actionFactory,
        actionAuditor, clusterStateSink, costCalculator);
  }

}
