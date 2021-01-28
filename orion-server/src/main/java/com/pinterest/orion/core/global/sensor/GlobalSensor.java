package com.pinterest.orion.core.global.sensor;

import com.pinterest.orion.core.Context;
import com.pinterest.orion.core.Plugin;

public abstract class GlobalSensor extends Context implements Plugin, Runnable {
  
  public void run() {
    try {
      observe();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public abstract void observe() throws Exception;
  
  public abstract int getInterval(); 

}
