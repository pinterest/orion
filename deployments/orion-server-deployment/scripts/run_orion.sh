#!/bin/bash
#*******************************************************************************
# Copyright 2020 Pinterest, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#*******************************************************************************

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

export JAVA_HOME=/usr/lib/jvm/openjdk-8-jdk
export LOG_DIR=/var/log/orion
export ORION_HOME=$(pwd $DIR/..)
export JAR=$ORION_HOME/*.jar

mkdir -p $LOG_DIR

$JAVA_HOME/bin/java -server -Xms8G -Xmx8G -verbosegc -Xloggc:$LOG_DIR/gc.log \
    -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=100 -XX:GCLogFileSize=2M \
    -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:+PrintClassHistogram \
    -XX:+UseG1GC -XX:MaxGCPauseMillis=250 -XX:G1ReservePercent=10 -XX:ConcGCThreads=4 \
    -XX:ParallelGCThreads=4 -XX:G1HeapRegionSize=8m -XX:InitiatingHeapOccupancyPercent=70 \
    -XX:ErrorFile=$LOG_DIR/jvm_error.log \
    -cp $JAR com.pinterest.orion.server.OrionServer server \
    $ORION_HOME/conf/kafka-server.yaml > $LOG_DIR/orion-stdout.log 2>&1
