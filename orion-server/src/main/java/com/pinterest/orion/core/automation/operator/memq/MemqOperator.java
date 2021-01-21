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
package com.pinterest.orion.core.automation.operator.memq;

import com.pinterest.orion.core.Cluster;
import com.pinterest.orion.core.automation.operator.Operator;
import com.pinterest.orion.core.memq.MemqCluster;

public abstract class MemqOperator extends Operator {



  @Override
  public final void operate(Cluster cluster) throws Exception {
    if(cluster instanceof MemqCluster){
      operate((MemqCluster) cluster);
    }
  }

  public abstract void operate(MemqCluster cluster) throws Exception;

}
