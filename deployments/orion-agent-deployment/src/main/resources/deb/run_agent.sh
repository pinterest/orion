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

JAVA_HOME=/usr/lib/jvm/openjdk-8-jdk
ORION_HOME=/opt/orion-agent
ORION_CONFIG_FILE=/etc/orion-agent/kafka-agent.yaml
LOG_PROPERTIES=/opt/orion-agent/logging.properties
LOG_DIR=/var/log/orion-agent
DAEMON=$JAVA_HOME/bin/java
CLASSPATH=$ORION_HOME/*:$ORION_HOME/lib/*
DAEMON_OPTS="-server -Xmx800M -Xms800M -verbosegc -Xloggc:$LOG_DIR/gc.log
    -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=20 -XX:GCLogFileSize=20M
    -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:ErrorFile=$LOG_DIR/jvm_error.log
    -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt/orion-agent/
    -Djava.util.logging.config.file=$LOG_PROPERTIES
    -Dlog.dir=$LOG_DIR
    -cp $CLASSPATH"
exec ${DAEMON} ${DAEMON_OPTS} \
     com.pinterest.orion.agent.OrionAgent \
     $ORION_CONFIG_FILE >> "$LOG_DIR/orion-agent-stdout.log" 2>&1