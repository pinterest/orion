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
package com.pinterest.orion.core.automation.conflicts;

public class MinIsrRfConflict extends Conflict {

    private String topicName;
    private boolean topicInLCONF;
    private int currRf;
    private int currMinIsr;
    private int clusterDefaultRf;

    public String getTopicName() {
        return topicName;
    }

    public boolean isTopicInLCONF() {
        return topicInLCONF;
    }

    public int getCurrRf() {
        return currRf;
    }

    public int getCurrMinIsr() {
        return currMinIsr;
    }

    public int getClusterDefaultRf() {
        return clusterDefaultRf;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public void setTopicInLCONF(boolean topicInLCONF) {
        this.topicInLCONF = topicInLCONF;
    }

    public void setCurrRf(int currRf) {
        this.currRf = currRf;
    }

    public void setCurrMinIsr(int currMinIsr) {
        this.currMinIsr = currMinIsr;
    }

    public void setClusterDefaultRf(int clusterDefaultRf) {
        this.clusterDefaultRf = clusterDefaultRf;
    }

    @Override
    public boolean detectConflict() {
        return currRf < currMinIsr;
    }
}
