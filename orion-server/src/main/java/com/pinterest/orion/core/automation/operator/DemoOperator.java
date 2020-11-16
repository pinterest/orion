package com.pinterest.orion.core.automation.operator;

import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.actions.Action;
import com.pinterest.orion.core.actions.generic.DemoAction;

public class DemoOperator extends Operator {
  int counter = 0;

  @Override
  public void operate(Cluster cluster) throws Exception {
    Action a = new DemoAction();
    a.setAttribute("id", counter++);
    dispatch(a);
  }

  @Override
  public String getName() {
    return "DemoOperator";
  }
}
