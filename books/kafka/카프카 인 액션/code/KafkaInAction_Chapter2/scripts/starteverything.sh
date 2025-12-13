#!/usr/bin/env bash

bin/zookeeper-server-start.sh -daemon config/zookeeper.properties
sleep 20
bin/kafka-server-start.sh -daemon config/server0.properties
bin/kafka-server-start.sh -daemon config/server1.properties
bin/kafka-server-start.sh -daemon config/server2.properties
