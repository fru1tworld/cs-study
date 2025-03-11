# Commands used in Chapter 6

## Listing topics from Zookeeper

	bin/zookeeper-shell.sh localhost:2181 
	ls /brokers/topics

## Find Controller in ZooKeeper

    bin/zookeeper-shell.sh localhost:2181 
	get /controller 

    
## Starting a broker with a JMX Port

	JMX_PORT=$JMX_PORT bin/kafka-server-start.sh config/server0.properties 
	
    
## Describe Topic

    $ bin/kafka-topics.sh --describe --bootstrap-server localhost:9094 --topic kinaction_replica_test 
    
    # Sample output
	Topic:kinaction_replica_test	PartitionCount:1	ReplicationFactor:3	Configs:
	Topic: kinaction_replica_test	Partition: 0	Leader: 0	Replicas: 1,0,2	Isr: 0,2
	
	
## Under-replicated-partitions flag

	bin/kafka-topics.sh --describe --bootstrap-server localhost:9094 --under-replicated-partitions 

	#Sample output
	Topic: kinaction_replica_test	Partition: 0	Leader: 0	Replicas: 1,0,2	Isr: 0,2
