# Commands used in Chapter 7

## Creating a topic for Chapter 7

	bin/kafka-topics.sh --create --bootstrap-server localhost:9094 \ 
	--topic kinaction_topicandpart \ 
	--replication-factor 2 \ 
	--partitions 2

    
## Deleting a topic

	bin/kafka-topics.sh --delete --bootstrap-server localhost:9094 \ // #A <1>
	--topic kinaction_topicandpart
	
	
## Looking at a dump of a segment

	bin/kafka-dump-log.sh --print-data-log --files /tmp/kafkainaction/kafka-logs-0/kinaction_topicandpart-1/*.log| awk -F: '{print $NF}' | grep kinaction

## Creating a compacted topic

	bin/kafka-topics.sh --create --bootstrap-server localhost:9094 \
	--topic kinaction_compact --partitions 3 --replication-factor 3 \
	--config cleanup.policy=compact 

