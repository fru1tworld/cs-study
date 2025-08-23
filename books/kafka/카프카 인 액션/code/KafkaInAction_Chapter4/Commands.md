# Commands used in Chapter 4

## Creating the kinaction Topics

`bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic kinaction_alert --partitions 3 --replication-factor 3`


`bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic kinaction_alerttrend --partitions 3 --replication-factor 3`


`bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic kinaction_audit --partitions 3 --replication-factor 3`
