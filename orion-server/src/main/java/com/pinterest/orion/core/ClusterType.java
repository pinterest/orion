package com.pinterest.orion.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface ClusterType {

  String name();

  /**
   * Higher priority means it will be loaded first
   * @return priority
   */
  int priority();

}
