#!/bin/bash
# Build script for Ubuntu

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

cd $DIR/..

# ensure root to install required packages

echo "deb https://deb.nodesource.com/node_12.x bionic main" > /etc/apt/sources.list.d/nodesource.list && \
    curl -s -k https://deb.nodesource.com/gpgkey/nodesource.gpg.key | apt-key add - && \
    apt-get update && \
    apt-get install -y nodejs openjdk-8-jdk maven && \
    rm -rf /var/lib/apt/lists/*

mvn clean package -DskipTests
